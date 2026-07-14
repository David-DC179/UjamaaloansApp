package com.example.ujamaloansapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private static final SimpleDateFormat DUE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    DrawerLayout drawerLayout;
    NavigationView navView;
    Toolbar toolbar;
    TextView userWelcome;

    MaterialCardView nextDueCard;
    TextView paymentStatusLabel;
    TextView nextDueAmount;
    TextView nextDueDate;
    TextView nextDueCountdown;
    TextView activeLoansCount;
    TextView totalOutstanding;

    LinearLayout recentLoansContainer;
    TextView recentLoansEmpty;
    TextView viewAllLoansLink;

    FloatingActionButton applyLoanFab;

    SessionManager sessionManager;
    DatabaseHelper databaseHelper;
    FirestoreSyncManager firestoreSyncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        databaseHelper = new DatabaseHelper(this);
        firestoreSyncManager = new FirestoreSyncManager(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.nav_open_desc,
                R.string.nav_close_desc
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navView.setNavigationItemSelectedListener(this::onNavItemSelected);


        // Greet the logged-in user

        userWelcome = findViewById(R.id.userWelcome);

        String name = sessionManager.getName();

        if (name != null) {
            userWelcome.setText("Hello, " + name);
        }


        // Wire the floating action button - the entry point for applying for a new loan

        applyLoanFab = findViewById(R.id.applyLoanFab);
        applyLoanFab.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));


        // Loan summary widgets

        nextDueCard = findViewById(R.id.nextDueCard);
        paymentStatusLabel = findViewById(R.id.paymentStatusLabel);
        nextDueAmount = findViewById(R.id.nextDueAmount);
        nextDueDate = findViewById(R.id.nextDueDate);
        nextDueCountdown = findViewById(R.id.nextDueCountdown);
        activeLoansCount = findViewById(R.id.activeLoansCount);
        totalOutstanding = findViewById(R.id.totalOutstanding);

        nextDueCard.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));


        // Recent loans widgets

        recentLoansContainer = findViewById(R.id.recentLoansContainer);
        recentLoansEmpty = findViewById(R.id.recentLoansEmpty);
        viewAllLoansLink = findViewById(R.id.viewAllLoansLink);

        viewAllLoansLink.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));

        seedDemoDataIfNeeded();

        requestNotificationPermission();

    }



    // First-time accounts start with no loan history - seed sample data so the dashboard isn't empty

    private void seedDemoDataIfNeeded() {

        String userEmail = sessionManager.getEmail();

        if (databaseHelper.getLoanSummary(userEmail).totalLoans == 0) {

            databaseHelper.seedSampleLoans(userEmail);

        }

    }



    @Override
    protected void onResume() {

        super.onResume();

        loadLoanSummary();
        loadRecentLoans();

        attemptBackgroundSync();

    }



    // Opportunistically sync with Firestore when a connection is available, so loans applied
    // while offline go up automatically instead of waiting for a manual Settings sync
    private void attemptBackgroundSync() {

        if (!NetworkUtils.isConnected(this)) {
            return;
        }

        firestoreSyncManager.syncNow(sessionManager.getEmail(), new FirestoreSyncManager.SyncCallback() {

            @Override
            public void onSyncSuccess(int loansPushed, int feedbackPushed, int loansPulled, int feedbackPulled) {

                sessionManager.setLastSyncTime(System.currentTimeMillis());

                if (loansPulled > 0 || loansPushed > 0) {

                    runOnUiThread(() -> {
                        loadLoanSummary();
                        loadRecentLoans();
                    });

                }

            }

            @Override
            public void onSyncFailed(String reason) {
                // Silent - the manual "Sync Now" action in Settings surfaces failures to the user
            }

        });

    }



    private void loadLoanSummary() {

        String userEmail = sessionManager.getEmail();

        DatabaseHelper.LoanSummary summary = databaseHelper.getLoanSummary(userEmail);

        activeLoansCount.setText(String.valueOf(summary.pendingLoans));
        totalOutstanding.setText(String.format(Locale.getDefault(), "Tsh %,.0f", summary.totalOutstanding));


        Cursor cursor = databaseHelper.getNextDueLoan(userEmail);

        if (cursor.moveToFirst()) {

            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            String dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));

            paymentStatusLabel.setText("NEXT PAYMENT DUE · " + status.toUpperCase(Locale.getDefault()));
            nextDueAmount.setText(String.format(Locale.getDefault(), "Tsh %,.0f", amount));
            nextDueDate.setText("Due " + dueDate);

            applyCountdown(dueDate);

        } else {

            paymentStatusLabel.setText("NEXT PAYMENT DUE");
            nextDueAmount.setText("No active loans");
            nextDueDate.setText("Apply for a loan to get started");
            nextDueCountdown.setText("");

            nextDueCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorTextSecondary));

        }

        cursor.close();

    }



    private void applyCountdown(String dueDate) {

        try {

            Date due = DUE_DATE_FORMAT.parse(dueDate);

            long diffMillis = due.getTime() - System.currentTimeMillis();
            long daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis);

            if (diffMillis < 0) {

                nextDueCountdown.setText("Overdue by " + Math.abs(daysLeft) + " day(s)");
                nextDueCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorError));

            } else if (daysLeft == 0) {

                nextDueCountdown.setText("Due today");
                nextDueCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorError));

            } else {

                nextDueCountdown.setText(daysLeft + " day(s) remaining");
                nextDueCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));

            }

        } catch (ParseException e) {

            nextDueCountdown.setText("");

        }

    }



    private void loadRecentLoans() {

        // Clear out any rows added on a previous call, keep the empty-state label

        for (int i = recentLoansContainer.getChildCount() - 1; i >= 0; i--) {

            View child = recentLoansContainer.getChildAt(i);

            if (child != recentLoansEmpty) {
                recentLoansContainer.removeView(child);
            }

        }

        Cursor cursor = databaseHelper.getRecentLoans(sessionManager.getEmail(), 5);

        if (cursor.getCount() == 0) {

            recentLoansEmpty.setVisibility(View.VISIBLE);

        } else {

            recentLoansEmpty.setVisibility(View.GONE);

            LayoutInflater inflater = LayoutInflater.from(this);

            while (cursor.moveToNext()) {

                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                int termMonths = cursor.getInt(cursor.getColumnIndexOrThrow("term_months"));
                String purpose = cursor.getString(cursor.getColumnIndexOrThrow("purpose"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                String appliedDate = cursor.getString(cursor.getColumnIndexOrThrow("applied_date"));
                String dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));

                if (recentLoansContainer.getChildCount() > 1) {

                    View divider = new View(this);

                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 2));

                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.colorBackground));

                    recentLoansContainer.addView(divider);

                }

                View row = inflater.inflate(R.layout.item_recent_loan, recentLoansContainer, false);

                ((TextView) row.findViewById(R.id.loanPurpose)).setText(purpose);
                ((TextView) row.findViewById(R.id.loanDate)).setText("Applied " + appliedDate);
                ((TextView) row.findViewById(R.id.loanAmount)).setText(
                        String.format(Locale.getDefault(), "Tsh %,.0f", amount));

                TextView statusView = row.findViewById(R.id.loanStatus);
                statusView.setText(status.toUpperCase(Locale.getDefault()));
                statusView.setTextColor(ContextCompat.getColor(this, statusColor(status)));

                row.setOnClickListener(v -> showLoanReceipt(amount, termMonths, purpose, status, appliedDate, dueDate));

                recentLoansContainer.addView(row);

            }

        }

        cursor.close();

    }



    private int statusColor(String status) {

        switch (status) {

            case "Paid":
                return R.color.colorPrimary;

            case "Rejected":
                return R.color.colorError;

            case "Approved":
                return R.color.colorPrimaryDarkMode;

            default:
                return R.color.colorSecondary;

        }

    }



    private void showLoanReceipt(
            double amount,
            int termMonths,
            String purpose,
            String status,
            String appliedDate,
            String dueDate
    ) {

        String receipt = "Amount: Tsh " + String.format(Locale.getDefault(), "%,.0f", amount) +
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



    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );

            }

        }

    }



    private boolean onNavItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_reports) {

            startActivity(new Intent(this, ReportActivity.class));

        } else if (id == R.id.nav_account) {

            startActivity(new Intent(this, AccountActivity.class));

        } else if (id == R.id.nav_settings) {

            startActivity(new Intent(this, SettingsActivity.class));

        } else if (id == R.id.nav_feedback) {

            startActivity(new Intent(this, UserFeedbackActivity.class));

        } else if (id == R.id.nav_complains) {

            startActivity(new Intent(this, ComplainsActivity.class));

        } else if (id == R.id.nav_help) {

            startActivity(new Intent(this, HelpActivity.class));

        } else if (id == R.id.nav_contact) {

            startActivity(new Intent(this, ContactActivity.class));

        } else if (id == R.id.nav_privacy_policy) {

            startActivity(new Intent(this, PrivacyPolicyActivity.class));

        } else if (id == R.id.nav_logout) {

            logout();

        }

        // nav_dashboard falls through here too - we're already on the Dashboard

        drawerLayout.closeDrawers();

        return true;

    }



    private void logout() {

        sessionManager.logout();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        return true;

    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_about) {

            Toast.makeText(
                    this,
                    "Ujamaa Loans - Mobile Loan Management System",
                    Toast.LENGTH_SHORT
            ).show();

            return true;

        } else if (id == R.id.action_logout) {

            logout();

            return true;

        }

        return super.onOptionsItemSelected(item);

    }



    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {

            drawerLayout.closeDrawers();

        } else {

            super.onBackPressed();

        }

    }

}
