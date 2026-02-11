package com.gfg.evapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    EditText etBattery, etDistance, etSource, etDestination;
    TextView tvResult;
    Button btnCheck, btnMap, btnHistory;
    ImageView imgEV;
    Spinner spVehicle, spDriveMode;

    DBHelper db;
    FirebaseFirestore firestore;

    int FULL_RANGE = 300;
    int avgSpeed = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getSharedPreferences("EVAPP", MODE_PRIVATE);
        if (!sp.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        String userEmail = sp.getString("userEmail", "unknown");

        firestore = FirebaseFirestore.getInstance();
        db = new DBHelper(this);

        etBattery = findViewById(R.id.etBattery);
        etDistance = findViewById(R.id.etDistance);
        etSource = findViewById(R.id.etSource);
        etDestination = findViewById(R.id.etDestination);
        tvResult = findViewById(R.id.tvResult);
        btnCheck = findViewById(R.id.btnCheck);
        btnMap = findViewById(R.id.btnMap);
        btnHistory = findViewById(R.id.btnHistory);
        imgEV = findViewById(R.id.imgEV);
        spVehicle = findViewById(R.id.spVehicle);
        spDriveMode = findViewById(R.id.spDriveMode);

        // 🚗 Vehicle Spinner
        String[] vehicles = {"EV Car", "EV Bike", "EV Bus"};
        spVehicle.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, vehicles));

        // 🚦 Drive Mode Spinner
        String[] driveModes = {"Eco", "Normal", "Sport", "Race"};
        spDriveMode.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, driveModes));

        btnCheck.setOnClickListener(v -> {

            if (etBattery.getText().toString().isEmpty() ||
                    etDistance.getText().toString().isEmpty() ||
                    etSource.getText().toString().isEmpty() ||
                    etDestination.getText().toString().isEmpty()) {

                Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show();
                return;
            }

            int batteryPercent = Integer.parseInt(etBattery.getText().toString());
            int batteryCapacity = Integer.parseInt(etDistance.getText().toString());

            if (batteryPercent > 100 || batteryPercent <= 0) {
                Toast.makeText(this, "Battery % must be between 1-100", Toast.LENGTH_SHORT).show();
                return;
            }

            String vehicle = spVehicle.getSelectedItem().toString();
            String driveMode = spDriveMode.getSelectedItem().toString();

            int efficiency = vehicle.equals("EV Bike") ? 8 :
                    vehicle.equals("EV Bus") ? 3 : 6;

            switch (driveMode) {
                case "Eco": avgSpeed = 40; break;
                case "Sport": avgSpeed = 80; break;
                case "Race": avgSpeed = 100; break;
                default: avgSpeed = 60;
            }

            double usableEnergy = (batteryPercent / 100.0) * batteryCapacity;

            if (batteryPercent < 20) usableEnergy *= 0.8;
            if (driveMode.equals("Race")) usableEnergy *= 0.75;
            if (driveMode.equals("Eco")) usableEnergy *= 1.1;

            int range = (int) (usableEnergy * efficiency);

            double timeHrs = (double) range / avgSpeed;
            int hrs = (int) timeHrs;
            int mins = (int) ((timeHrs - hrs) * 60);
            String eta = hrs + " hrs " + mins + " mins";

            String result = "Trip Possible ✅";

            tvResult.setText(
                    result +
                            "\nVehicle: " + vehicle +
                            "\nDrive Mode: " + driveMode +
                            "\nRange: " + range + " km" +
                            "\nETA: " + eta
            );

            // ✅ Save to SQLite
            db.insertTrip(
                    userEmail,
                    batteryPercent,
                    batteryCapacity,
                    etSource.getText().toString(),
                    etDestination.getText().toString(),
                    result,
                    "Not Selected",
                    eta
            );

            // 🔥 Save to Firestore
            Map<String, Object> tripData = new HashMap<>();
            tripData.put("userEmail", userEmail);
            tripData.put("vehicle", vehicle);
            tripData.put("driveMode", driveMode);
            tripData.put("batteryPercent", batteryPercent);
            tripData.put("batteryCapacity", batteryCapacity);
            tripData.put("range", range);
            tripData.put("eta", eta);
            tripData.put("source", etSource.getText().toString());
            tripData.put("destination", etDestination.getText().toString());
            tripData.put("result", result);
            tripData.put("timestamp", FieldValue.serverTimestamp());

            firestore.collection("Trips")
                    .add(tripData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this,
                                "Saved to Firestore ☁",
                                Toast.LENGTH_LONG).show();
                        Log.d("FIRESTORE_SUCCESS", "Saved with ID: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Firestore Failed!",
                                Toast.LENGTH_LONG).show();
                        Log.e("FIRESTORE_ERROR", e.getMessage());
                    });

        });

        btnMap.setOnClickListener(v -> {
            Intent i = new Intent(this, MapsActivity.class);
            i.putExtra("source", etSource.getText().toString());
            i.putExtra("destination", etDestination.getText().toString());
            i.putExtra("battery", etBattery.getText().toString());
            i.putExtra("driveMode", spDriveMode.getSelectedItem().toString());
            startActivity(i);
        });

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, TripHistoryActivity.class))
        );
    }
}
