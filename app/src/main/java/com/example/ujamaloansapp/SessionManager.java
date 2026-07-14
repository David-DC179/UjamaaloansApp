package com.example.ujamaloansapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "UjamaaLoansSession";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_LAST_SYNC = "last_sync_millis";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String email, String name) {
        prefs.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_NAME, name)
                .apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, null);
    }

    public boolean isLoggedIn() {
        return getEmail() != null;
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    public void setLastSyncTime(long millis) {
        prefs.edit().putLong(KEY_LAST_SYNC, millis).apply();
    }

    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }

}
