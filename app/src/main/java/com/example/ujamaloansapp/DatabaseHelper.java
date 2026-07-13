package com.example.ujamaloansapp;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "UjamaaLoans.db";
    private static final int DATABASE_VERSION = 1;


    private static final String TABLE_USERS = "users";


    public DatabaseHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }



    @Override
    public void onCreate(SQLiteDatabase db) {


        String createTable = "CREATE TABLE " + TABLE_USERS +
                "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "phone TEXT," +
                "email TEXT," +
                "password TEXT" +
                ")";


        db.execSQL(createTable);

    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

        onCreate(db);

    }



    // Insert User

    public boolean registerUser(
            String name,
            String phone,
            String email,
            String password
    ){

        SQLiteDatabase db = this.getWritableDatabase();


        ContentValues values = new ContentValues();

        values.put("name",name);
        values.put("phone",phone);
        values.put("email",email);
        values.put("password",password);



        long result = db.insert(
                TABLE_USERS,
                null,
                values
        );


        return result != -1;

    }



    // Login Check

    public boolean loginUser(
            String email,
            String password
    ){


        SQLiteDatabase db = this.getReadableDatabase();


        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE email=? AND password=?",
                new String[]{email,password}
        );


        boolean result = cursor.getCount() > 0;


        cursor.close();


        return result;


    }

    public boolean insertTestUser(){


        SQLiteDatabase db = this.getWritableDatabase();


        ContentValues values = new ContentValues();


        values.put("name","David");
        values.put("phone","0712345678");
        values.put("email","test@gmail.com");
        values.put("password","12345");


        long result = db.insert(
                "users",
                null,
                values
        );


        return result != -1;

    }
    public boolean updatePassword(String email, String newPassword){


        SQLiteDatabase db = this.getWritableDatabase();


        ContentValues values = new ContentValues();

        values.put("password", newPassword);



        int result = db.update(
                "users",
                values,
                "email=?",
                new String[]{email}
        );



        return result > 0;


    }

}