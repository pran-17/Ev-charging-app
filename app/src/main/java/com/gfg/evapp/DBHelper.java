package com.gfg.evapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "EVApp.db";
    private static final int DB_VERSION = 5; // 🔥 INCREASE VERSION

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // 🔹 USERS TABLE
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "email TEXT UNIQUE," +
                "password TEXT)");

        // 🔹 TRIP TABLE (CONNECTED TO USER)
        db.execSQL("CREATE TABLE trip (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_email TEXT," +
                "battery INTEGER," +
                "capacity INTEGER," +
                "source TEXT," +
                "destination TEXT," +
                "result TEXT," +
                "charger TEXT," +
                "eta TEXT,"+
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS trip");
        onCreate(db);
    }

    // ✅ SIGN UP USER
    public boolean registerUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("email", email);
        cv.put("password", password);

        long result = db.insert("users", null, cv);
        return result != -1;
    }

    // ✅ LOGIN CHECK
    public boolean loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT * FROM users WHERE email=? AND password=?",
                new String[]{email, password}
        );

        boolean success = c.getCount() > 0;
        c.close();
        return success;
    }

    // ✅ INSERT TRIP (THIS FIXES YOUR ERROR)
    // ✅ INSERT TRIP (UPDATED WITH ETA)
    public void insertTrip(String userEmail,
                           int battery,
                           int capacity,
                           String source,
                           String destination,
                           String result,
                           String charger,
                           String eta) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("user_email", userEmail);
        cv.put("battery", battery);
        cv.put("capacity", capacity);
        cv.put("source", source);
        cv.put("destination", destination);
        cv.put("result", result);
        cv.put("charger", charger);
        cv.put("eta", eta);   // ✅ NEW

        db.insert("trip", null, cv);
    }


    // (Optional) 🔹 GET TRIPS FOR LOGGED-IN USER
    public Cursor getTripsByUser(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM trip WHERE user_email=? ORDER BY timestamp DESC",
                new String[]{email}
        );
    }
}
