package com.example.ujamaloansapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ComplainsActivity extends AppCompatActivity {

    Spinner complaintCategorySpinner;
    EditText complaintMessage;
    Button submitComplaintBtn;

    DatabaseHelper databaseHelper;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_complains);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        complaintCategorySpinner = findViewById(R.id.complaintCategorySpinner);
        complaintMessage = findViewById(R.id.complaintMessage);
        submitComplaintBtn = findViewById(R.id.submitComplaintBtn);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Service", "Loan Processing", "App Issue", "Other"}
        );

        complaintCategorySpinner.setAdapter(categoryAdapter);

        submitComplaintBtn.setOnClickListener(v -> submitComplaint());

    }



    private void submitComplaint() {

        String category = complaintCategorySpinner.getSelectedItem().toString();
        String message = complaintMessage.getText().toString();

        if (message.isEmpty()) {

            Toast.makeText(this, "Please describe your complaint", Toast.LENGTH_SHORT).show();
            return;

        }

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        String fullMessage = category + ": " + message;

        long result = databaseHelper.insertFeedback(
                sessionManager.getEmail(),
                "complaint",
                fullMessage,
                0,
                date
        );

        if (result != -1) {

            NotificationHelper.showNotification(
                    this,
                    "Complaint Submitted",
                    "We've received your complaint and will follow up."
            );

            syncComplaintToCloud(fullMessage, date);

            Toast.makeText(this, "Complaint submitted", Toast.LENGTH_SHORT).show();

            finish();

        } else {

            Toast.makeText(this, "Failed to submit complaint", Toast.LENGTH_SHORT).show();

        }

    }



    private void syncComplaintToCloud(String message, String date) {

        Map<String, Object> complaint = new HashMap<>();

        complaint.put("userEmail", sessionManager.getEmail());
        complaint.put("type", "complaint");
        complaint.put("message", message);
        complaint.put("rating", 0);
        complaint.put("date", date);

        FirebaseFirestore.getInstance()
                .collection("feedback")
                .add(complaint)
                .addOnSuccessListener(ref -> Log.d("Firestore", "Complaint synced: " + ref.getId()))
                .addOnFailureListener(e -> Log.w("Firestore", "Complaint sync failed", e));

    }

}
