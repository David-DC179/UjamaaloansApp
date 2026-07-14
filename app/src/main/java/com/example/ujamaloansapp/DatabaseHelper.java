package com.example.ujamaloansapp;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "UjamaaLoans.db";
    private static final int DATABASE_VERSION = 3;


    private static final String TABLE_USERS = "users";
    private static final String TABLE_LOANS = "loans";
    private static final String TABLE_FEEDBACK = "feedback";


    public DatabaseHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }



    @Override
    public void onCreate(SQLiteDatabase db) {


        String createUsers = "CREATE TABLE " + TABLE_USERS +
                "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "phone TEXT," +
                "email TEXT UNIQUE," +
                "password TEXT" +
                ")";

        String createLoans = "CREATE TABLE " + TABLE_LOANS +
                "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_email TEXT," +
                "amount REAL," +
                "term_months INTEGER," +
                "purpose TEXT," +
                "status TEXT," +
                "applied_date TEXT," +
                "due_date TEXT," +
                "firestore_id TEXT," +
                "synced INTEGER DEFAULT 0" +
                ")";

        String createFeedback = "CREATE TABLE " + TABLE_FEEDBACK +
                "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_email TEXT," +
                "type TEXT," +
                "message TEXT," +
                "rating INTEGER," +
                "date TEXT," +
                "firestore_id TEXT," +
                "synced INTEGER DEFAULT 0" +
                ")";


        db.execSQL(createUsers);
        db.execSQL(createLoans);
        db.execSQL(createFeedback);

    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOANS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDBACK);

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



    // Login Check (matches email OR phone)

    public boolean loginUser(
            String identifier,
            String password
    ){


        SQLiteDatabase db = this.getReadableDatabase();


        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE (email=? OR phone=?) AND password=?",
                new String[]{identifier,identifier,password}
        );


        boolean result = cursor.getCount() > 0;


        cursor.close();


        return result;


    }



    // Look up a user by email or phone (used to resolve session identity)

    public Cursor getUserByIdentifier(String identifier){

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM users WHERE email=? OR phone=?",
                new String[]{identifier, identifier}
        );

    }



    public boolean updateUserProfile(String email, String name, String phone){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("phone", phone);

        int result = db.update(
                TABLE_USERS,
                values,
                "email=?",
                new String[]{email}
        );

        return result > 0;

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



    // Loans

    public long insertLoan(
            String userEmail,
            double amount,
            int termMonths,
            String purpose,
            String appliedDate,
            String dueDate
    ){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_email", userEmail);
        values.put("amount", amount);
        values.put("term_months", termMonths);
        values.put("purpose", purpose);
        values.put("status", "Pending");
        values.put("applied_date", appliedDate);
        values.put("due_date", dueDate);

        return db.insert(TABLE_LOANS, null, values);

    }



    // _id alias is required by SimpleCursorAdapter/CursorAdapter

    public Cursor getLoansCursor(String userEmail){

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id AS _id, amount, term_months, purpose, status, applied_date, due_date " +
                        "FROM loans WHERE user_email=? ORDER BY id DESC",
                new String[]{userEmail}
        );

    }



    // --- Firestore sync support (loans) ---

    // Locally created loans not yet pushed to Firestore
    public Cursor getUnsyncedLoans(String userEmail){

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, user_email, amount, term_months, purpose, status, applied_date, due_date " +
                        "FROM loans WHERE user_email=? AND synced=0",
                new String[]{userEmail}
        );

    }

    public void markLoanSynced(long localId, String firestoreId){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("synced", 1);
        values.put("firestore_id", firestoreId);

        db.update(TABLE_LOANS, values, "id=?", new String[]{String.valueOf(localId)});

    }

    public boolean loanExistsByFirestoreId(String firestoreId){

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM loans WHERE firestore_id=?",
                new String[]{firestoreId}
        );

        boolean exists = cursor.getCount() > 0;

        cursor.close();

        return exists;

    }

    // A loan that originated on another device, pulled down from Firestore
    public long insertLoanFromRemote(
            String firestoreId,
            String userEmail,
            double amount,
            int termMonths,
            String purpose,
            String status,
            String appliedDate,
            String dueDate
    ){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_email", userEmail);
        values.put("amount", amount);
        values.put("term_months", termMonths);
        values.put("purpose", purpose);
        values.put("status", status);
        values.put("applied_date", appliedDate);
        values.put("due_date", dueDate);
        values.put("firestore_id", firestoreId);
        values.put("synced", 1);

        return db.insert(TABLE_LOANS, null, values);

    }



    // Feedback / Complaints (shared table, distinguished by type)

    public long insertFeedback(
            String userEmail,
            String type,
            String message,
            int rating,
            String date
    ){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_email", userEmail);
        values.put("type", type);
        values.put("message", message);
        values.put("rating", rating);
        values.put("date", date);

        return db.insert(TABLE_FEEDBACK, null, values);

    }



    // --- Firestore sync support (feedback) ---

    public Cursor getUnsyncedFeedback(String userEmail){

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, user_email, type, message, rating, date " +
                        "FROM feedback WHERE user_email=? AND synced=0",
                new String[]{userEmail}
        );

    }

    public void markFeedbackSynced(long localId, String firestoreId){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("synced", 1);
        values.put("firestore_id", firestoreId);

        db.update(TABLE_FEEDBACK, values, "id=?", new String[]{String.valueOf(localId)});

    }

    public boolean feedbackExistsByFirestoreId(String firestoreId){

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM feedback WHERE firestore_id=?",
                new String[]{firestoreId}
        );

        boolean exists = cursor.getCount() > 0;

        cursor.close();

        return exists;

    }

    public long insertFeedbackFromRemote(
            String firestoreId,
            String userEmail,
            String type,
            String message,
            int rating,
            String date
    ){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_email", userEmail);
        values.put("type", type);
        values.put("message", message);
        values.put("rating", rating);
        values.put("date", date);
        values.put("firestore_id", firestoreId);
        values.put("synced", 1);

        return db.insert(TABLE_FEEDBACK, null, values);

    }

}
