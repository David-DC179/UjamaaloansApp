package com.example.ujamaloansapp;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class Sign_up_Activity extends AppCompatActivity {


    EditText name,phone,email,password,confirmPassword;

    Button registerBtn;


    DatabaseHelper databaseHelper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_up);



        databaseHelper = new DatabaseHelper(this);



        name = findViewById(R.id.name);

        phone = findViewById(R.id.phone);

        email = findViewById(R.id.email);

        password = findViewById(R.id.password);

        confirmPassword = findViewById(R.id.confirmPassword);


        registerBtn = findViewById(R.id.registerBtn);





        registerBtn.setOnClickListener(v -> {


            String userName = name.getText().toString();
            String userPhone = phone.getText().toString();
            String userEmail = email.getText().toString();
            String userPassword = password.getText().toString();
            String confirm = confirmPassword.getText().toString();



            if(userName.isEmpty() ||
                    userPhone.isEmpty() ||
                    userEmail.isEmpty() ||
                    userPassword.isEmpty()){


                Toast.makeText(
                        this,
                        "Fill all fields",
                        Toast.LENGTH_SHORT
                ).show();


            }


            else if(!userPassword.equals(confirm)){


                Toast.makeText(
                        this,
                        "Password not matching",
                        Toast.LENGTH_SHORT
                ).show();


            }


            else{


                boolean insert = databaseHelper.registerUser(
                        userName,
                        userPhone,
                        userEmail,
                        userPassword
                );


                if(insert){


                    Toast.makeText(
                            this,
                            "Registration Successful",
                            Toast.LENGTH_SHORT
                    ).show();


                }

                else{


                    Toast.makeText(
                            this,
                            "Registration Failed",
                            Toast.LENGTH_SHORT
                    ).show();


                }



            }


        });



    }


}