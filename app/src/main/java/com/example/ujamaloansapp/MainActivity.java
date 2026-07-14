package com.example.ujamaloansapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    EditText loanAmount, startDate, preferredTime;
    Spinner termSpinner, purposeSpinner;
    Button submitLoanBtn;

    DatabaseHelper databaseHelper;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        loanAmount = findViewById(R.id.loanAmount);
        startDate = findViewById(R.id.startDate);
        preferredTime = findViewById(R.id.preferredTime);
        termSpinner = findViewById(R.id.termSpinner);
        purposeSpinner = findViewById(R.id.purposeSpinner);
        submitLoanBtn = findViewById(R.id.submitLoanBtn);

        ArrayAdapter<String> termAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"3 months", "6 months", "12 months", "24 months"}
        );
        termSpinner.setAdapter(termAdapter);

        ArrayAdapter<String> purposeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Business", "Education", "Medical", "Agriculture", "Other"}
        );
        purposeSpinner.setAdapter(purposeAdapter);

        startDate.setOnClickListener(v -> showDatePicker());
        preferredTime.setOnClickListener(v -> showTimePicker());

        submitLoanBtn.setOnClickListener(v -> submitLoan());

    }



    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    String date = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth
                    );

                    startDate.setText(date);

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();

    }



    private void showTimePicker() {

        Calendar calendar = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {

                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    preferredTime.setText(time);

                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );

        dialog.show();

    }



    private void submitLoan() {

        String amountStr = loanAmount.getText().toString();
        String date = startDate.getText().toString();

        if (amountStr.isEmpty()) {

            Toast.makeText(this, "Enter a loan amount", Toast.LENGTH_SHORT).show();
            return;

        }

        if (date.isEmpty()) {

            Toast.makeText(this, "Pick a preferred start date", Toast.LENGTH_SHORT).show();
            return;

        }

        double amount = Double.parseDouble(amountStr);
        String term = termSpinner.getSelectedItem().toString();
        int termMonths = Integer.parseInt(term.split(" ")[0]);
        String purpose = purposeSpinner.getSelectedItem().toString();

        Calendar due = Calendar.getInstance();
        due.add(Calendar.MONTH, termMonths);

        String dueDate = String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                due.get(Calendar.YEAR),
                due.get(Calendar.MONTH) + 1,
                due.get(Calendar.DAY_OF_MONTH)
        );

        String userEmail = sessionManager.getEmail();

        long result = databaseHelper.insertLoan(userEmail, amount, termMonths, purpose, date, dueDate);

        if (result != -1) {

            NotificationHelper.showNotification(
                    this,
                    "Loan Application Submitted",
                    "Your loan application for " + amount + " has been received."
            );

            syncLoanToCloud(userEmail, amount, termMonths, purpose, date, dueDate);

            Toast.makeText(this, "Loan application submitted", Toast.LENGTH_SHORT).show();

            finish();

        } else {

            Toast.makeText(this, "Failed to submit application", Toast.LENGTH_SHORT).show();

        }

    }



    private void syncLoanToCloud(
            String userEmail,
            double amount,
            int termMonths,
            String purpose,
            String appliedDate,
            String dueDate
    ) {

        Map<String, Object> loan = new HashMap<>();

        loan.put("userEmail", userEmail);
        loan.put("amount", amount);
        loan.put("termMonths", termMonths);
        loan.put("purpose", purpose);
        loan.put("status", "Pending");
        loan.put("appliedDate", appliedDate);
        loan.put("dueDate", dueDate);

        FirebaseFirestore.getInstance()
                .collection("loans")
                .add(loan)
                .addOnSuccessListener(ref -> Log.d("Firestore", "Loan synced: " + ref.getId()))
                .addOnFailureListener(e -> Log.w("Firestore", "Loan sync failed", e));

    }

}
