package com.example.app_de_chat.ui.gallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_de_chat.ChatActivity;
import com.example.app_de_chat.RecentChatItem;
import com.example.app_de_chat.RecentChatsAdapter;
import com.example.app_de_chat.databinding.FragmentGalleryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.DocumentSnapshot;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private RecentChatsAdapter adapter;
    private ListenerRegistration registration;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        new ViewModelProvider(this).get(GalleryViewModel.class); // keep ViewModel, but not used now

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup RecyclerView
        adapter = new RecentChatsAdapter();
        binding.recyclerRecentChats.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRecentChats.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            Intent i = new Intent(requireContext(), ChatActivity.class);
            i.putExtra("userId", item.getUserId());
            i.putExtra("userName", item.getUserName());
            startActivity(i);
        });

        subscribeToRecentChats();
        return root;
    }

    private void subscribeToRecentChats() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            showEmpty(true);
            return;
        }
        Query query = FirebaseFirestore.getInstance()
                .collection("recent_chats")
                .document(uid)
                .collection("chats")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Detach previous listener if any
        if (registration != null) {
            registration.remove();
            registration = null;
        }

        registration = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                // Try fallback if Firestore failed
                loadFromRealtimeFallback();
                return;
            }
            List<RecentChatItem> list = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    RecentChatItem item = doc.toObject(RecentChatItem.class);
                    if (item.getUserId() == null || item.getUserId().isEmpty()) {
                        item.setUserId(doc.getId());
                    }
                    if (item.getTimestamp() == 0L && doc.contains("timestamp")) {
                        Long ts = doc.getLong("timestamp");
                        item.setTimestamp(ts != null ? ts : 0L);
                    }
                    list.add(item);
                }
            }
            if (list.isEmpty()) {
                // If Firestore has no data, try to reconstruct from Realtime DB
                loadFromRealtimeFallback();
            } else {
                adapter.setItems(list);
                showEmpty(false);
                // Resolve missing display names
                fetchAndApplyDisplayNames(list, false);
            }
        });
    }

    private void loadFromRealtimeFallback() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || getContext() == null) {
            showEmpty(true);
            return;
        }
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, RecentChatItem> map = new HashMap<>();
                for (DataSnapshot roomSnap : snapshot.getChildren()) {
                    String roomKey = roomSnap.getKey();
                    if (roomKey == null) continue;
                    if (!roomKey.startsWith(uid)) continue; // Only rooms for current user
                    String otherId = roomKey.substring(uid.length());
                    long lastTs = 0L;
                    String lastPreview = null;
                    for (DataSnapshot msgSnap : roomSnap.getChildren()) {
                        Long ts = msgSnap.child("timestamp").getValue(Long.class);
                        String type = msgSnap.child("messageType").getValue(String.class);
                        String text = msgSnap.child("message").getValue(String.class);
                        long t = ts != null ? ts : 0L;
                        if (t >= lastTs) {
                            lastTs = t;
                            if ("image".equals(type)) {
                                lastPreview = "Imagen";
                            } else {
                                lastPreview = text != null ? text : "Mensaje";
                            }
                        }
                    }
                    RecentChatItem item = new RecentChatItem();
                    item.setUserId(otherId);
                    item.setUserName(otherId); // Placeholder until displayName is resolved
                    item.setLastMessage(lastPreview != null ? lastPreview : "Mensaje");
                    item.setTimestamp(lastTs);
                    map.put(otherId, item);
                }
                List<RecentChatItem> list = new ArrayList<>(map.values());
                // Sort by timestamp desc
                list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.setItems(list);
                showEmpty(list.isEmpty());

                // Resolve names and then backfill Firestore with the nicer names
                fetchAndApplyDisplayNames(list, true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showEmpty(true);
            }
        });
    }

    private void fetchAndApplyDisplayNames(List<RecentChatItem> list, boolean backfillAfter) {
        if (list == null || list.isEmpty()) return;
        // Collect IDs needing lookup
        List<String> idsToFetch = new ArrayList<>();
        for (RecentChatItem item : list) {
            String name = item.getUserName();
            if (name == null || name.isEmpty() || name.equals(item.getUserId())) {
                idsToFetch.add(item.getUserId());
            }
        }
        if (idsToFetch.isEmpty()) {
            if (backfillAfter) backfillFirestore(list);
            return;
        }
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        // Chunk in groups of up to 10 for whereIn
        int batchSize = 10;
        final int[] pending = {0};
        final boolean[] anyUpdated = {false};
        for (int i = 0; i < idsToFetch.size(); i += batchSize) {
            int end = Math.min(i + batchSize, idsToFetch.size());
            List<String> chunk = idsToFetch.subList(i, end);
            pending[0]++;
            fs.collection("users")
                    .whereIn(FieldPath.documentId(), new ArrayList<>(chunk))
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null) {
                            for (DocumentSnapshot doc : snap) {
                                String uid = doc.getId();
                                String displayName = doc.getString("displayName");
                                String email = doc.getString("email");
                                String best = (displayName != null && !displayName.isEmpty()) ? displayName
                                        : (email != null && !email.isEmpty()) ? email : uid;
                                for (RecentChatItem item : list) {
                                    if (uid.equals(item.getUserId())) {
                                        if (!best.equals(item.getUserName())) {
                                            item.setUserName(best);
                                            anyUpdated[0] = true;
                                        }
                                    }
                                }
                            }
                        }
                    })
                    .addOnCompleteListener(task -> {
                        pending[0]--;
                        if (pending[0] == 0) {
                            if (anyUpdated[0]) {
                                adapter.setItems(new ArrayList<>(list));
                            }
                            if (backfillAfter) backfillFirestore(list);
                        }
                    });
        }
    }

    private void backfillFirestore(List<RecentChatItem> list) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || list == null || list.isEmpty()) return;
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        for (RecentChatItem item : list) {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", item.getUserId());
            data.put("userName", item.getUserName());
            data.put("lastMessage", item.getLastMessage());
            data.put("timestamp", item.getTimestamp());
            fs.collection("recent_chats").document(uid).collection("chats").document(item.getUserId()).set(data);
        }
    }

    private void showEmpty(boolean empty) {
        if (binding == null) return;
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerRecentChats.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        binding = null;
    }
}