package com.example.ujamaloansapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    DrawerLayout drawerLayout;
    NavigationView navView;
    Toolbar toolbar;
    TextView userWelcome;

    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);

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


        // Wire the quick-action buttons

        Button applyLoanBtn = findViewById(R.id.applyLoanBtn);
        Button myLoansBtn = findViewById(R.id.myLoansBtn);
        Button accountBtn = findViewById(R.id.accountBtn);
        Button settingsBtn = findViewById(R.id.settingsBtn);
        Button feedbackBtn = findViewById(R.id.feedbackBtn);

        applyLoanBtn.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        myLoansBtn.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        accountBtn.setOnClickListener(v -> startActivity(new Intent(this, AccountActivity.class)));
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        feedbackBtn.setOnClickListener(v -> startActivity(new Intent(this, UserFeedbackActivity.class)));

        requestNotificationPermission();

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
