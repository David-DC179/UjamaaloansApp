package com.example.ujamaloansapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 201;
    private static final int ENABLE_BLUETOOTH_REQUEST_CODE = 202;

    TextView bluetoothStatus;
    Button toggleBluetoothBtn;

    TextView syncStatus;
    TextView lastSyncText;
    Button syncNowBtn;

    BluetoothAdapter bluetoothAdapter;

    SessionManager sessionManager;
    FirestoreSyncManager firestoreSyncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        bluetoothStatus = findViewById(R.id.bluetoothStatus);
        toggleBluetoothBtn = findViewById(R.id.toggleBluetoothBtn);

        syncStatus = findViewById(R.id.syncStatus);
        lastSyncText = findViewById(R.id.lastSyncText);
        syncNowBtn = findViewById(R.id.syncNowBtn);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        sessionManager = new SessionManager(this);
        firestoreSyncManager = new FirestoreSyncManager(this);

        toggleBluetoothBtn.setOnClickListener(v -> enableBluetooth());
        syncNowBtn.setOnClickListener(v -> syncNow());

        refreshBluetoothStatus();
        refreshSyncStatus();

    }



    @Override
    protected void onResume() {

        super.onResume();

        refreshBluetoothStatus();
        refreshSyncStatus();

    }



    private void refreshSyncStatus() {

        if (NetworkUtils.isConnected(this)) {
            syncStatus.setText("Online - ready to sync");
        } else {
            syncStatus.setText("Offline - changes are saved on this device and will sync when you're back online");
        }

        long lastSync = sessionManager.getLastSyncTime();

        if (lastSync == 0) {

            lastSyncText.setText("Never synced");

        } else {

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            lastSyncText.setText("Last synced " + format.format(new Date(lastSync)));

        }

    }



    private void syncNow() {

        syncNowBtn.setEnabled(false);
        syncStatus.setText("Syncing...");

        firestoreSyncManager.syncNow(sessionManager.getEmail(), new FirestoreSyncManager.SyncCallback() {

            @Override
            public void onSyncSuccess(int loansPushed, int feedbackPushed, int loansPulled, int feedbackPulled) {

                runOnUiThread(() -> {

                    sessionManager.setLastSyncTime(System.currentTimeMillis());

                    syncStatus.setText("Sync complete");
                    Toast.makeText(
                            SettingsActivity.this,
                            "Synced: " + loansPushed + " loan(s) and " + feedbackPushed +
                                    " feedback item(s) uploaded, " + loansPulled + " loan(s) and " +
                                    feedbackPulled + " feedback item(s) downloaded",
                            Toast.LENGTH_LONG
                    ).show();

                    syncNowBtn.setEnabled(true);
                    refreshSyncStatus();

                });

            }

            @Override
            public void onSyncFailed(String reason) {

                runOnUiThread(() -> {

                    syncStatus.setText(reason);
                    syncNowBtn.setEnabled(true);

                });

            }

        });

    }



    private void refreshBluetoothStatus() {

        if (bluetoothAdapter == null) {

            bluetoothStatus.setText("Bluetooth is not supported on this device");
            toggleBluetoothBtn.setEnabled(false);

            return;

        }

        if (hasBluetoothPermission() && bluetoothAdapter.isEnabled()) {

            bluetoothStatus.setText("Bluetooth is enabled");
            toggleBluetoothBtn.setText("Disable Bluetooth");

        } else {

            bluetoothStatus.setText("Bluetooth is disabled");
            toggleBluetoothBtn.setText("Enable Bluetooth");

        }

    }



    private boolean hasBluetoothPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;

        }

        return true;

    }



    private void enableBluetooth() {

        if (bluetoothAdapter == null) {

            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();

            return;

        }

        if (!hasBluetoothPermission()) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_PERMISSION_REQUEST_CODE
            );

            return;

        }

        if (!bluetoothAdapter.isEnabled()) {

            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, ENABLE_BLUETOOTH_REQUEST_CODE);

        } else {

            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show();

        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {

            refreshBluetoothStatus();

        }

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {

            refreshBluetoothStatus();

        }

    }

}
