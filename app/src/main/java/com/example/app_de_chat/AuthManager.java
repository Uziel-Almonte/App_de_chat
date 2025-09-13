package com.example.app_de_chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class AuthManager {
    private static final String PREFS = "auth_prefs";
    private static final String KEY_USER = "user";
    private static final String KEY_PASS = "pass";
    private static final String KEY_LOGGED_IN = "logged_in";

    private AuthManager() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isUserRegistered(Context ctx) {
        return prefs(ctx).contains(KEY_USER) && prefs(ctx).contains(KEY_PASS);
    }

    public static void register(Context ctx, String user, String pass) {
        prefs(ctx).edit()
                .putString(KEY_USER, user)
                .putString(KEY_PASS, pass)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public static boolean login(Context ctx, String user, String pass) {
        String u = prefs(ctx).getString(KEY_USER, null);
        String p = prefs(ctx).getString(KEY_PASS, null);
        if (!TextUtils.isEmpty(u) && !TextUtils.isEmpty(p) && u.equals(user) && p.equals(pass)) {
            prefs(ctx).edit().putBoolean(KEY_LOGGED_IN, true).apply();
            return true;
        }
        return false;
    }

    public static void logout(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_LOGGED_IN, false).apply();
    }

    public static boolean isLoggedIn(Context ctx) {
        return prefs(ctx).getBoolean(KEY_LOGGED_IN, false);
    }

    public static String getRegisteredUser(Context ctx) {
        return prefs(ctx).getString(KEY_USER, null);
    }
}
