package com.example.ujamaloansapp;


import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class LoginActivity extends AppCompatActivity {


    EditText emailPhone, password;

    Button loginBtn;

    TextView signup, forgotPassword;


    DatabaseHelper databaseHelper;

    SessionManager sessionManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);



        // Connect Database
        databaseHelper = new DatabaseHelper(this);

        sessionManager = new SessionManager(this);



        // Temporary test user (safe to call every time: email is now UNIQUE,
        // so repeat calls just fail the insert instead of duplicating rows)
        databaseHelper.insertTestUser();




        // Connect XML components

        emailPhone = findViewById(R.id.emailPhone);

        password = findViewById(R.id.password);

        loginBtn = findViewById(R.id.loginBtn);

        signup = findViewById(R.id.signup);

        forgotPassword = findViewById(R.id.forgotPassword);





        // Login Button Click

        loginBtn.setOnClickListener(this::onClick);






        // Sign Up Click

        signup.setOnClickListener(v -> {


            Intent intent = new Intent(
                    LoginActivity.this,
                    Sign_up_Activity.class
            );


            startActivity(intent);


        });






        // Forgot Password Click

        forgotPassword.setOnClickListener(v -> {


            Intent intent = new Intent(
                    LoginActivity.this,
                    Reset_password_Activity.class
            );


            startActivity(intent);


        });



    }





    private void onClick(View v) {



        String user = emailPhone.getText().toString();

        String pass = password.getText().toString();




        if(user.isEmpty() || pass.isEmpty()) {



            Toast.makeText(
                    this,
                    "Fill all fields",
                    Toast.LENGTH_SHORT
            ).show();



        }
        else if(!NetworkUtils.isConnected(this)) {



            Toast.makeText(
                    this,
                    "No internet connection. Please connect and try again.",
                    Toast.LENGTH_SHORT
            ).show();



        }
        else {



            boolean checkLogin;

            try {

                checkLogin = databaseHelper.loginUser(
                        user,
                        pass
                );

            } catch (SQLiteException e) {

                Toast.makeText(
                        this,
                        "Database unavailable. Please try again.",
                        Toast.LENGTH_SHORT
                ).show();

                return;

            }




            if(checkLogin) {



                // Resolve the canonical email/name for the session
                Cursor cursor = databaseHelper.getUserByIdentifier(user);

                if (cursor.moveToFirst()) {

                    String canonicalEmail = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));

                    sessionManager.saveSession(canonicalEmail, name);

                }

                cursor.close();



                Toast.makeText(
                        this,
                        "Login Successful",
                        Toast.LENGTH_SHORT
                ).show();


                Intent intent = new Intent(
                        LoginActivity.this,
                        DashboardActivity.class
                );


                startActivity(intent);


                finish();



            }
            else {



                Toast.makeText(
                        this,
                        "Wrong Email or Password",
                        Toast.LENGTH_SHORT
                ).show();



            }



        }



    }


}
