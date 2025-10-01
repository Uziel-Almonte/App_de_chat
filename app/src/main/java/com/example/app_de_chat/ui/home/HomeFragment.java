package com.example.app_de_chat.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_de_chat.User;
import com.example.app_de_chat.UserAdapter;
import com.example.app_de_chat.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import com.example.app_de_chat.ChatActivity;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private UserAdapter userAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search button click listener
        binding.btnSearch.setOnClickListener(v -> searchUser());

        return root;
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter();
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerUsers.setAdapter(userAdapter);

        // Handle user click - open ChatActivity
        userAdapter.setOnUserClickListener(user -> {
            openChatActivity(user);
        });
    }

    private void searchUser() {
        String email = binding.editEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.editEmail.setError("Please enter an email");
            return;
        }

        // Don't allow searching for own email
        if (email.equals(mAuth.getCurrentUser().getEmail())) {
            Toast.makeText(getContext(), "You cannot search for yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        showLoading(true);

        // Search for users with the entered email
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        List<User> users = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            users.add(user);
                        }

                        if (users.isEmpty()) {
                            showNoResults();
                        } else {
                            showResults(users);
                        }
                    } else {
                        Toast.makeText(getContext(), "Search failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openChatActivity(User user) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("userId", user.getUid());
        intent.putExtra("userName", user.getDisplayName());
        intent.putExtra("userEmail", user.getEmail());
        startActivity(intent);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.textResults.setVisibility(View.GONE);
            binding.recyclerUsers.setVisibility(View.GONE);
            binding.textNoResults.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void showResults(List<User> users) {
        binding.textResults.setVisibility(View.VISIBLE);
        binding.recyclerUsers.setVisibility(View.VISIBLE);
        binding.textNoResults.setVisibility(View.GONE);
        userAdapter.setUsers(users);
    }

    private void showNoResults() {
        binding.textResults.setVisibility(View.GONE);
        binding.recyclerUsers.setVisibility(View.GONE);
        binding.textNoResults.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}