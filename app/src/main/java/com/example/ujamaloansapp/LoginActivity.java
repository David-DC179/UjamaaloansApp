package com.example.ujamaloansapp;


import android.content.Intent;
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



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);



        // Connect Database
        databaseHelper = new DatabaseHelper(this);



        // Temporary test user
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
        else {



            boolean checkLogin = databaseHelper.loginUser(
                    user,
                    pass
            );




            if(checkLogin) {



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
                // Next lesson:
                // Open DashboardActivity here



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