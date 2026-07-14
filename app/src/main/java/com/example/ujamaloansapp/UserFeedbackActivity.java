package com.example.ujamaloansapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserFeedbackActivity extends AppCompatActivity {

    RatingBar feedbackRating;
    EditText feedbackMessage;
    Button submitFeedbackBtn;

    DatabaseHelper databaseHelper;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_feedback);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        feedbackRating = findViewById(R.id.feedbackRating);
        feedbackMessage = findViewById(R.id.feedbackMessage);
        submitFeedbackBtn = findViewById(R.id.submitFeedbackBtn);

        submitFeedbackBtn.setOnClickListener(v -> submitFeedback());

    }



    private void submitFeedback() {

        String message = feedbackMessage.getText().toString();
        int rating = (int) feedbackRating.getRating();

        if (message.isEmpty()) {

            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;

        }

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        long result = databaseHelper.insertFeedback(
                sessionManager.getEmail(),
                "feedback",
                message,
                rating,
                date
        );

        if (result != -1) {

            NotificationHelper.showNotification(
                    this,
                    "Feedback Submitted",
                    "Thank you for your feedback!"
            );

            Toast.makeText(this, "Feedback submitted", Toast.LENGTH_SHORT).show();

            finish();

        } else {

            Toast.makeText(this, "Failed to submit feedback", Toast.LENGTH_SHORT).show();

        }

    }

}
