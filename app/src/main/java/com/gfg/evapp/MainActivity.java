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

        // 🔹 UI
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

        // =========================
        // 🚗 VEHICLE SPINNER
        // =========================
        String[] vehicles = {"EV Car", "EV Bike", "EV Bus"};
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                vehicles
        );
        spVehicle.setAdapter(vehicleAdapter);

        spVehicle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {

                String selectedVehicle = parent.getItemAtPosition(position).toString();

                if (selectedVehicle.equals("EV Bike")) {
                    imgEV.setImageResource(R.drawable.ev_bike);
                    FULL_RANGE = 120;

                } else if (selectedVehicle.equals("EV Bus")) {
                    imgEV.setImageResource(R.drawable.ev_bus);
                    FULL_RANGE = 250;

                } else {
                    imgEV.setImageResource(R.drawable.ev_charge);
                    FULL_RANGE = 300;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // =========================
        // 🚦 DRIVE MODE SPINNER
        // =========================
        String[] driveModes = {"Eco", "Normal", "Sport", "Race"};
        ArrayAdapter<String> driveAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                driveModes
        );
        spDriveMode.setAdapter(driveAdapter);

        // =========================
        // 🔍 CHECK TRIP BUTTON
        // =========================
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

            // =========================
            // 💾 SAVE TO SQLITE
            // =========================
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

            // =========================
            // ☁ SAVE TO FIRESTORE
            // =========================
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
                        Log.d("FIRESTORE_SUCCESS", "Saved ID: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Firestore Failed!",
                                Toast.LENGTH_LONG).show();
                        Log.e("FIRESTORE_ERROR", e.getMessage());
                    });
        });

        // =========================
        // 🗺 MAP BUTTON
        // =========================
        btnMap.setOnClickListener(v -> {
            Intent i = new Intent(this, MapsActivity.class);
            i.putExtra("source", etSource.getText().toString());
            i.putExtra("destination", etDestination.getText().toString());
            i.putExtra("battery", etBattery.getText().toString());
            i.putExtra("driveMode", spDriveMode.getSelectedItem().toString());
            startActivity(i);
        });

        // =========================
        // 📜 HISTORY BUTTON
        // =========================
        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, TripHistoryActivity.class))
        );
    }
}
