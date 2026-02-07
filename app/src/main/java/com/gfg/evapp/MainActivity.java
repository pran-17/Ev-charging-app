package com.gfg.evapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText etBattery, etDistance, etSource, etDestination;
    TextView tvResult;
    Button btnCheck, btnMap, btnHistory;
    ImageView imgEV;
    Spinner spVehicle, spDriveMode;

    DBHelper db;

    int FULL_RANGE = 300;
    int avgSpeed = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔐 LOGIN CHECK
        SharedPreferences sp = getSharedPreferences("EVAPP", MODE_PRIVATE);
        if (!sp.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userEmail = sp.getString("userEmail", "unknown");

        setContentView(R.layout.activity_main);

        // 🔹 UI INIT
        etBattery = findViewById(R.id.etBattery);
        etDistance = findViewById(R.id.etDistance); // kWh
        etSource = findViewById(R.id.etSource);
        etDestination = findViewById(R.id.etDestination);
        tvResult = findViewById(R.id.tvResult);
        btnCheck = findViewById(R.id.btnCheck);
        btnMap = findViewById(R.id.btnMap);
        btnHistory = findViewById(R.id.btnHistory);
        imgEV = findViewById(R.id.imgEV);
        spVehicle = findViewById(R.id.spVehicle);
        spDriveMode = findViewById(R.id.spDriveMode);

        db = new DBHelper(this);

        // 🚗 VEHICLE SPINNER
        String[] vehicles = {"EV Car", "EV Bike", "EV Bus"};
        spVehicle.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, vehicles));

        // 🚦 DRIVE MODE SPINNER
        String[] driveModes = {"Eco", "Normal", "Sport", "Race"};
        spDriveMode.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, driveModes));

        // 🚗 VEHICLE IMAGE + RANGE
        spVehicle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                String v = parent.getItemAtPosition(pos).toString();
                if (v.equals("EV Bike")) {
                    imgEV.setImageResource(R.drawable.ev_bike);
                    FULL_RANGE = 120;
                } else if (v.equals("EV Bus")) {
                    imgEV.setImageResource(R.drawable.ev_bus);
                    FULL_RANGE = 250;
                } else {
                    imgEV.setImageResource(R.drawable.ev_charge);
                    FULL_RANGE = 300;
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 🔹 CHECK TRIP
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

            String vehicle = spVehicle.getSelectedItem().toString();
            String driveMode = spDriveMode.getSelectedItem().toString();

            // ⚡ Efficiency
            int efficiency = vehicle.equals("EV Bike") ? 8 :
                    vehicle.equals("EV Bus") ? 3 : 6;

            // 🚦 SPEED
            switch (driveMode) {
                case "Eco": avgSpeed = 40; break;
                case "Sport": avgSpeed = 80; break;
                case "Race": avgSpeed = 100; break;
                default: avgSpeed = 60;
            }

            // 🔋 ENERGY
            double usableEnergy = (batteryPercent / 100.0) * batteryCapacity;

            if (batteryPercent < 20) usableEnergy *= 0.8;
            if (driveMode.equals("Race")) usableEnergy *= 0.75;
            if (driveMode.equals("Eco")) usableEnergy *= 1.1;

            int range = (int) (usableEnergy * efficiency);

            // ⏱ ETA
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
                            "\nSpeed: " + avgSpeed + " km/h" +
                            "\nETA: " + eta
            );

            // 💾 SAVE TO DB ✅ FIXED
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
        });

        // 🗺 MAP
        btnMap.setOnClickListener(v -> {
            Intent i = new Intent(this, MapsActivity.class);
            i.putExtra("source", etSource.getText().toString());
            i.putExtra("destination", etDestination.getText().toString());
            i.putExtra("battery", etBattery.getText().toString());
            startActivity(i);
        });

        // 📜 HISTORY
        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, TripHistoryActivity.class))
        );
    }
}
