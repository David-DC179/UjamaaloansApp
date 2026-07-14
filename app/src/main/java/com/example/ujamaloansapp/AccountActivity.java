package com.example.ujamaloansapp;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AccountActivity extends AppCompatActivity {

    TextView accountEmail;
    EditText accountName, accountPhone;
    Button saveAccountBtn;

    DatabaseHelper databaseHelper;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        accountEmail = findViewById(R.id.accountEmail);
        accountName = findViewById(R.id.accountName);
        accountPhone = findViewById(R.id.accountPhone);
        saveAccountBtn = findViewById(R.id.saveAccountBtn);

        loadProfile();

        saveAccountBtn.setOnClickListener(v -> saveProfile());

    }



    private void loadProfile() {

        String email = sessionManager.getEmail();

        Cursor cursor = databaseHelper.getUserByIdentifier(email);

        if (cursor.moveToFirst()) {

            accountEmail.setText(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            accountName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            accountPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow("phone")));

        }

        cursor.close();

    }



    private void saveProfile() {

        String name = accountName.getText().toString();
        String phone = accountPhone.getText().toString();

        if (name.isEmpty() || phone.isEmpty()) {

            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;

        }

        boolean updated = databaseHelper.updateUserProfile(sessionManager.getEmail(), name, phone);

        if (updated) {

            sessionManager.saveSession(sessionManager.getEmail(), name);

            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();

        } else {

            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();

        }

    }

}
