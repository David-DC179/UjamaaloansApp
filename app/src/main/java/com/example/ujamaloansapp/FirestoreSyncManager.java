package com.example.ujamaloansapp;

import android.content.Context;
import android.database.Cursor;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps the local SQLite database as the offline source of truth and mirrors it to
// Firestore ("users", "loans", "feedback" collections) whenever a connection is available.
// Local rows applied while offline are marked synced=0 until they're pushed up here.
public class FirestoreSyncManager {

    public interface SyncCallback {
        void onSyncSuccess(int loansPushed, int feedbackPushed, int loansPulled, int feedbackPulled);
        void onSyncFailed(String reason);
    }

    private final Context context;
    private final DatabaseHelper databaseHelper;
    private final FirebaseFirestore firestore;

    public FirestoreSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.databaseHelper = new DatabaseHelper(this.context);
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void syncNow(String userEmail, SyncCallback callback) {

        if (userEmail == null) {
            callback.onSyncFailed("No signed-in user to sync");
            return;
        }

        if (!NetworkUtils.isConnected(context)) {
            callback.onSyncFailed("No internet connection. Changes stay saved offline and will sync automatically once you're back online.");
            return;
        }

        List<Task<?>> pushTasks = new ArrayList<>();

        pushTasks.add(pushUserProfile(userEmail));

        List<Task<Void>> loanPushTasks = pushUnsyncedLoans(userEmail);
        List<Task<Void>> feedbackPushTasks = pushUnsyncedFeedback(userEmail);

        pushTasks.addAll(loanPushTasks);
        pushTasks.addAll(feedbackPushTasks);

        int loansPushed = loanPushTasks.size();
        int feedbackPushed = feedbackPushTasks.size();

        Tasks.whenAllComplete(pushTasks)
                .addOnSuccessListener(results -> pullRemoteData(userEmail, loansPushed, feedbackPushed, callback))
                .addOnFailureListener(e -> callback.onSyncFailed("Sync failed: " + e.getMessage()));

    }



    private Task<Void> pushUserProfile(String userEmail) {

        Cursor cursor = databaseHelper.getUserByIdentifier(userEmail);

        Map<String, Object> data = new HashMap<>();

        if (cursor.moveToFirst()) {

            data.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")));
            data.put("phone", cursor.getString(cursor.getColumnIndexOrThrow("phone")));
            data.put("email", userEmail);
            data.put("updatedAt", FieldValue.serverTimestamp());

        }

        cursor.close();

        if (data.isEmpty()) {
            return Tasks.forResult(null);
        }

        return firestore.collection("users").document(userEmail).set(data, SetOptions.merge());

    }



    private List<Task<Void>> pushUnsyncedLoans(String userEmail) {

        List<Task<Void>> tasks = new ArrayList<>();

        Cursor cursor = databaseHelper.getUnsyncedLoans(userEmail);

        while (cursor.moveToNext()) {

            long localId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
            int termMonths = cursor.getInt(cursor.getColumnIndexOrThrow("term_months"));
            String purpose = cursor.getString(cursor.getColumnIndexOrThrow("purpose"));
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            String appliedDate = cursor.getString(cursor.getColumnIndexOrThrow("applied_date"));
            String dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));

            Map<String, Object> data = new HashMap<>();

            data.put("userEmail", userEmail);
            data.put("amount", amount);
            data.put("termMonths", termMonths);
            data.put("purpose", purpose);
            data.put("status", status);
            data.put("appliedDate", appliedDate);
            data.put("dueDate", dueDate);
            data.put("updatedAt", FieldValue.serverTimestamp());

            DocumentReference ref = firestore.collection("loans").document();

            Task<Void> task = ref.set(data)
                    .addOnSuccessListener(unused -> databaseHelper.markLoanSynced(localId, ref.getId()));

            tasks.add(task);

        }

        cursor.close();

        return tasks;

    }



    private List<Task<Void>> pushUnsyncedFeedback(String userEmail) {

        List<Task<Void>> tasks = new ArrayList<>();

        Cursor cursor = databaseHelper.getUnsyncedFeedback(userEmail);

        while (cursor.moveToNext()) {

            long localId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
            int rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

            Map<String, Object> data = new HashMap<>();

            data.put("userEmail", userEmail);
            data.put("type", type);
            data.put("message", message);
            data.put("rating", rating);
            data.put("date", date);
            data.put("updatedAt", FieldValue.serverTimestamp());

            DocumentReference ref = firestore.collection("feedback").document();

            Task<Void> task = ref.set(data)
                    .addOnSuccessListener(unused -> databaseHelper.markFeedbackSynced(localId, ref.getId()));

            tasks.add(task);

        }

        cursor.close();

        return tasks;

    }



    // After pushing local changes up, pull down anything that exists in Firestore but not
    // yet locally (e.g. a loan applied for this account from another device).
    private void pullRemoteData(String userEmail, int loansPushed, int feedbackPushed, SyncCallback callback) {

        firestore.collection("loans")
                .whereEqualTo("userEmail", userEmail)
                .get()
                .addOnSuccessListener(loanSnapshot -> {

                    int loansPulled = pullLoans(userEmail, loanSnapshot);

                    firestore.collection("feedback")
                            .whereEqualTo("userEmail", userEmail)
                            .get()
                            .addOnSuccessListener(feedbackSnapshot -> {

                                int feedbackPulled = pullFeedback(userEmail, feedbackSnapshot);

                                callback.onSyncSuccess(loansPushed, feedbackPushed, loansPulled, feedbackPulled);

                            })
                            .addOnFailureListener(e -> callback.onSyncFailed("Pull failed: " + e.getMessage()));

                })
                .addOnFailureListener(e -> callback.onSyncFailed("Pull failed: " + e.getMessage()));

    }



    private int pullLoans(String userEmail, QuerySnapshot snapshot) {

        int pulled = 0;

        for (QueryDocumentSnapshot doc : snapshot) {

            if (databaseHelper.loanExistsByFirestoreId(doc.getId())) {
                continue;
            }

            Double amount = doc.getDouble("amount");
            Long termMonths = doc.getLong("termMonths");
            String purpose = doc.getString("purpose");
            String status = doc.getString("status");
            String appliedDate = doc.getString("appliedDate");
            String dueDate = doc.getString("dueDate");

            if (amount == null || termMonths == null) {
                continue;
            }

            databaseHelper.insertLoanFromRemote(
                    doc.getId(), userEmail, amount, termMonths.intValue(),
                    purpose, status, appliedDate, dueDate
            );

            pulled++;

        }

        return pulled;

    }



    private int pullFeedback(String userEmail, QuerySnapshot snapshot) {

        int pulled = 0;

        for (QueryDocumentSnapshot doc : snapshot) {

            if (databaseHelper.feedbackExistsByFirestoreId(doc.getId())) {
                continue;
            }

            String type = doc.getString("type");
            String message = doc.getString("message");
            Long rating = doc.getLong("rating");
            String date = doc.getString("date");

            databaseHelper.insertFeedbackFromRemote(
                    doc.getId(), userEmail, type, message,
                    rating == null ? 0 : rating.intValue(), date
            );

            pulled++;

        }

        return pulled;

    }

}
