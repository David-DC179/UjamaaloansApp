package com.example.ujamaloansapp;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ReportActivity extends AppCompatActivity {

    ListView loansListView;
    TextView emptyView;

    DatabaseHelper databaseHelper;
    SessionManager sessionManager;

    Cursor loansCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        loansListView = findViewById(R.id.loansListView);
        emptyView = findViewById(R.id.emptyView);

        loansCursor = databaseHelper.getLoansCursor(sessionManager.getEmail());

        if (loansCursor.getCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        }

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                loansCursor,
                new String[]{"purpose", "status"},
                new int[]{android.R.id.text1, android.R.id.text2},
                0
        );

        loansListView.setAdapter(adapter);

        loansListView.setOnItemClickListener((parent, view, position, id) -> {

            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            showReceipt(cursor);

        });

    }



    private void showReceipt(Cursor cursor) {

        double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
        int termMonths = cursor.getInt(cursor.getColumnIndexOrThrow("term_months"));
        String purpose = cursor.getString(cursor.getColumnIndexOrThrow("purpose"));
        String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
        String appliedDate = cursor.getString(cursor.getColumnIndexOrThrow("applied_date"));
        String dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));

        String receipt = "Amount: " + amount +
                "\nTerm: " + termMonths + " months" +
                "\nPurpose: " + purpose +
                "\nStatus: " + status +
                "\nApplied Date: " + appliedDate +
                "\nDue Date: " + dueDate;

        new AlertDialog.Builder(this)
                .setTitle("Loan Receipt")
                .setMessage(receipt)
                .setPositiveButton("Close", null)
                .show();

    }



    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (loansCursor != null) {
            loansCursor.close();
        }

    }

}
