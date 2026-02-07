package com.gfg.evapp;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;


public class TripHistoryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);

        recyclerView = findViewById(R.id.recyclerTrips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = new DBHelper(this);

        SharedPreferences sp = getSharedPreferences("EVAPP", MODE_PRIVATE);
        String email = sp.getString("userEmail", "");

        Cursor cursor = db.getTripsByUser(email);

        TripAdapter adapter = new TripAdapter(this, cursor);
        recyclerView.setAdapter(adapter);
    }
}
