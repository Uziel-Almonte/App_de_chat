package com.example.app_de_chat;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import com.example.app_de_chat.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;



// MainActivity.java

// ... other imports

import com.example.app_de_chat.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding; // Declared here
    FirebaseAuth mAuth;
    // Button button; // No longer needed as separate variable if using binding
    // TextView textView; // No longer needed as separate variable if using binding
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ==== THIS IS THE CRITICAL FIX ====
        // Inflate the layout using ViewBinding BEFORE setContentView
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // Now use the binding's root view for setContentView
        setContentView(binding.getRoot()); // This was your line 45

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user == null) {
            // User is not signed in, redirect to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class); // Use MainActivity.this for context
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish MainActivity so the user can't go back to it
            return;   // IMPORTANT: Prevent rest of onCreate from executing
        } else {
            // User is signed in, display their details
            // Assuming user_details is the ID of a TextView in activity_main.xml
            binding.userDetails.setText(user.getEmail()); // Use binding to access the TextView
        }

        // Assuming btnLogout is the ID of a Button in activity_main.xml
        binding.btnLogout.setOnClickListener(new View.OnClickListener() { // Use binding to access the Button
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class); // Use MainActivity.this
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        setSupportActionBar(binding.appBarMain.toolbar); // Use binding
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        // .setAnchorView(R.id.fab) // If using an anchor, ensure it's correct
                        .show();
            }
        });

        DrawerLayout drawer = binding.drawerLayout; // Use binding
        NavigationView navigationView = binding.navView; // Use binding

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    // ... rest of your MainActivity (onOptionsItemSelected, onSupportNavigateUp, etc.)
    // Remember to use FirebaseAuth.getInstance().signOut() in onOptionsItemSelected as well.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) { // Ensure R.id.action_logout exists in your menu XML
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}