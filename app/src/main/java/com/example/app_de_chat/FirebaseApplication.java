package com.example.app_de_chat;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class FirebaseApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
}
