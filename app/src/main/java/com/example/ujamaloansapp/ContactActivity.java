package com.example.ujamaloansapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ContactActivity extends AppCompatActivity {

    private static final String SUPPORT_PHONE = "+255700000000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact);

        Button callBtn = findViewById(R.id.callBtn);
        Button smsBtn = findViewById(R.id.smsBtn);

        callBtn.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + SUPPORT_PHONE));
            startActivity(intent);

        });

        smsBtn.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + SUPPORT_PHONE));
            intent.putExtra("sms_body", "Hello Ujamaa Loans Support, ");
            startActivity(intent);

        });

    }

}
