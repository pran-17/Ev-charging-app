package com.gfg.evapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    int batteryPercentage = 50;
    final int FULL_RANGE = 300;

    LatLng sourceLocation;
    int maxRangeKm;
    Polyline activeRoute;

    // 🚗 Drive mode
    String driveMode = "Normal";
    int driveSpeed = 60; // km/h

    // ⚡ Charging cost
    final int CHARGER_POWER_KW = 7;
    final int COST_PER_UNIT = 8; // ₹ per kWh

    Set<String> addedPlaces = new HashSet<>();

    DBHelper db;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        db = new DBHelper(this);

        // ✅ GET LOGGED-IN USER EMAIL
        SharedPreferences sp = getSharedPreferences("EVAPP", MODE_PRIVATE);
        userEmail = sp.getString("userEmail", "unknown");

        // 🔋 Battery
        try {
            batteryPercentage = Integer.parseInt(
                    getIntent().getStringExtra("battery")
            );
        } catch (Exception ignored) {}

        // 🚦 Drive mode
        driveMode = getIntent().getStringExtra("driveMode");
        if (driveMode == null) driveMode = "Normal";

        switch (driveMode) {
            case "Eco":
                driveSpeed = 40;
                break;
            case "Sport":
                driveSpeed = 80;
                break;
            case "Race":
                driveSpeed = 100;
                break;
            default:
                driveSpeed = 60;
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        String sourceName = getIntent().getStringExtra("source");
        String destinationName = getIntent().getStringExtra("destination");

        LatLng source = getLocationFromName(sourceName);
        LatLng destination = getLocationFromName(destinationName);

        if (source == null || destination == null) {
            LatLng india = new LatLng(20.5937, 78.9629);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 5));
            Toast.makeText(this, "Invalid location input", Toast.LENGTH_LONG).show();
            return;
        }

        sourceLocation = source;
        maxRangeKm = (batteryPercentage * FULL_RANGE) / 100;

        // Markers
        mMap.addMarker(new MarkerOptions().position(source).title("Source"));
        mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));

        // Main route
        mMap.addPolyline(new PolylineOptions()
                .add(source, destination)
                .width(8)
                .color(Color.BLUE));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(source, 10));

        // Manual chargers
        addManualChargingStations(source);

        // API chargers along route corridor
        for (LatLng p : getRoutePoints(source, destination)) {
            fetchChargingStationsHybrid(p);
        }

        setupMarkerClick();
    }

    // 🔹 Route corridor points
    private List<LatLng> getRoutePoints(LatLng start, LatLng end) {
        List<LatLng> points = new ArrayList<>();
        int steps = 6;
        for (int i = 0; i <= steps; i++) {
            double lat = start.latitude + (end.latitude - start.latitude) * i / steps;
            double lng = start.longitude + (end.longitude - start.longitude) * i / steps;
            points.add(new LatLng(lat, lng));
        }
        return points;
    }

    // 🔹 Geocoder
    private LatLng getLocationFromName(String name) {
        try {
            Geocoder g = new Geocoder(this, Locale.getDefault());
            List<Address> list = g.getFromLocationName(name, 1);
            if (list != null && !list.isEmpty()) {
                return new LatLng(list.get(0).getLatitude(), list.get(0).getLongitude());
            }
        } catch (Exception ignored) {}
        return null;
    }

    // 🔹 API chargers
    private void fetchChargingStationsHybrid(LatLng searchPoint) {

        new Thread(() -> {
            try {
                String urlStr =
                        "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                                "?location=" + searchPoint.latitude + "," + searchPoint.longitude +
                                "&radius=30000" +
                                "&keyword=ev%20charging%20station" +
                                "&key=" + getString(R.string.google_maps_key);

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(urlStr).openConnection();
                conn.connect();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder json = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) json.append(line);

                JSONObject response = new JSONObject(json.toString());
                if (!response.getString("status").equals("OK")) return;

                JSONArray results = response.getJSONArray("results");

                runOnUiThread(() -> {
                    for (int i = 0; i < results.length(); i++) {
                        try {
                            JSONObject place = results.getJSONObject(i);
                            String placeId = place.getString("place_id");
                            if (addedPlaces.contains(placeId)) continue;
                            addedPlaces.add(placeId);

                            JSONObject loc = place.getJSONObject("geometry")
                                    .getJSONObject("location");

                            LatLng charger = new LatLng(
                                    loc.getDouble("lat"),
                                    loc.getDouble("lng")
                            );

                            float[] dist = new float[1];
                            Location.distanceBetween(
                                    sourceLocation.latitude,
                                    sourceLocation.longitude,
                                    charger.latitude,
                                    charger.longitude,
                                    dist
                            );

                            float km = dist[0] / 1000;
                            boolean reachable = km <= maxRangeKm;

                            mMap.addMarker(new MarkerOptions()
                                    .position(charger)
                                    .title(place.optString("name", "Charging Station"))
                                    .snippet(String.format("%.1f km • %s",
                                            km,
                                            reachable ? "Reachable ✅" : "Not Reachable ❌"))
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            reachable ?
                                                    BitmapDescriptorFactory.HUE_GREEN :
                                                    BitmapDescriptorFactory.HUE_RED
                                    )));
                        } catch (Exception ignored) {}
                    }
                });

            } catch (Exception e) {
                Log.e("PLACES_API", "Error");
            }
        }).start();
    }

    // 🔹 Manual chargers
    private void addManualChargingStations(LatLng source) {
        List<LatLng> manual = Arrays.asList(
                new LatLng(source.latitude + 0.05, source.longitude + 0.04),
                new LatLng(source.latitude - 0.04, source.longitude + 0.03),
                new LatLng(source.latitude + 0.03, source.longitude - 0.04)
        );

        for (LatLng s : manual) {
            mMap.addMarker(new MarkerOptions()
                    .position(s)
                    .title("Manual Charging Station")
                    .snippet("Offline")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE
                    )));
        }
    }

    // 🔹 Marker click → ETA + COST + SAVE
    private void setupMarkerClick() {

        mMap.setOnMarkerClickListener(marker -> {

            if (marker.getTitle().equals("Source") ||
                    marker.getTitle().equals("Destination")) {
                return false;
            }

            LatLng charger = marker.getPosition();

            float[] dist = new float[1];
            Location.distanceBetween(
                    sourceLocation.latitude,
                    sourceLocation.longitude,
                    charger.latitude,
                    charger.longitude,
                    dist
            );

            float km = dist[0] / 1000;
            boolean reachable = km <= maxRangeKm;

            // ✅ ETA
            double timeHours = km / driveSpeed;
            int hrs = (int) timeHours;
            int mins = (int) ((timeHours - hrs) * 60);
            String eta = hrs + " hrs " + mins + " mins";

            // ✅ COST
            int costPerHour = CHARGER_POWER_KW * COST_PER_UNIT;

            String msg =
                    "Distance: " + String.format("%.1f", km) + " km\n" +
                            "Status: " + (reachable ? "Reachable ✅" : "Not Reachable ❌") + "\n" +
                            "Drive Mode: " + driveMode + "\n" +
                            "ETA: " + eta + "\n" +
                            "Charging Cost (1 hr): ₹" + costPerHour;

            new AlertDialog.Builder(this)
                    .setTitle(marker.getTitle())
                    .setMessage(msg)
                    .setPositiveButton("Show Route", (d, w) -> {

                        if (activeRoute != null) activeRoute.remove();

                        activeRoute = mMap.addPolyline(new PolylineOptions()
                                .add(sourceLocation, charger)
                                .width(8)
                                .color(Color.MAGENTA));

                        db.insertTrip(
                                userEmail,
                                batteryPercentage,
                                FULL_RANGE,
                                "Source",
                                marker.getTitle(),
                                reachable ? "Charger Reachable" : "Charger Not Reachable",
                                marker.getTitle(),
                                eta
                        );

                        Toast.makeText(this,
                                "Route shown • ETA: " + eta,
                                Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });
    }
}
