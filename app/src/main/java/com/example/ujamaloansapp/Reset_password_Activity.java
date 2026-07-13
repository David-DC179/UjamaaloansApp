package com.example.ujamaloansapp;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class Reset_password_Activity extends AppCompatActivity {


    EditText resetEmail, newPassword;

    Button resetBtn;

    DatabaseHelper databaseHelper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reset_password);



        // Connect Database
        databaseHelper = new DatabaseHelper(this);



        // Connect XML components

        resetEmail = findViewById(R.id.resetEmail);

        newPassword = findViewById(R.id.newPassword);

        resetBtn = findViewById(R.id.resetBtn);




        // Reset Password Button

        resetBtn.setOnClickListener(v -> {


            String email = resetEmail.getText().toString();

            String password = newPassword.getText().toString();




            if(email.isEmpty() || password.isEmpty()) {


                Toast.makeText(
                        this,
                        "Fill all fields",
                        Toast.LENGTH_SHORT
                ).show();


            }

            else {


                boolean update = databaseHelper.updatePassword(
                        email,
                        password
                );



                if(update) {


                    Toast.makeText(
                            this,
                            "Password Updated Successfully",
                            Toast.LENGTH_SHORT
                    ).show();


                    finish();


                }

                else {


                    Toast.makeText(
                            this,
                            "Email not found",
                            Toast.LENGTH_SHORT
                    ).show();


                }


            }



        });



    }


}