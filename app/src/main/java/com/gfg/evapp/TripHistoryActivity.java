package com.gfg.evapp;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TripHistoryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DBHelper db;
    TripAdapter adapter;
    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);

        recyclerView = findViewById(R.id.recyclerTrips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = new DBHelper(this);

        loadTripHistory();
    }

    // ✅ STEP 3: LOAD + REFRESH HISTORY
    private void loadTripHistory() {

        SharedPreferences sp = getSharedPreferences("EVAPP", MODE_PRIVATE);
        String email = sp.getString("userEmail", "");

        cursor = db.getTripsByUser(email);

        if (cursor != null && cursor.getCount() > 0) {

            adapter = new TripAdapter(this, cursor);
            recyclerView.setAdapter(adapter);

        } else {
            Toast.makeText(this,
                    "No Trips Found",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ✅ Refresh when coming back from Maps or Booking
    @Override
    protected void onResume() {
        super.onResume();
        loadTripHistory();
    }

    // ✅ Prevent memory leak
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) {
            cursor.close();
        }
    }
}
