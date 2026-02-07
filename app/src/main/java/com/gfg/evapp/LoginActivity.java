package com.gfg.evapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    LinearLayout loginLayout, signupLayout;
    EditText etLoginEmail, etLoginPassword;
    EditText etSignupEmail, etSignupPassword;
    Button btnLogin, btnSignup;
    TextView tvGoSignup, tvGoLogin;

    DBHelper db;

    Animation slideUp, slideDown, fadeIn;
    ObjectAnimator borderPulse;   // 🔥 NEW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔐 AUTO LOGIN CHECK
        SharedPreferences sp = getSharedPreferences("EVAPP", MODE_PRIVATE);
        if (sp.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        db = new DBHelper(this);

        // 🎬 LOAD ANIMATIONS
        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        // 🧩 LAYOUTS (BOXES)
        loginLayout = findViewById(R.id.loginLayout);
        signupLayout = findViewById(R.id.signupLayout);

        // 🔐 LOGIN VIEWS
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // 📝 SIGNUP VIEWS
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        btnSignup = findViewById(R.id.btnSignup);

        tvGoSignup = findViewById(R.id.tvGoSignup);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        // 🎬 INITIAL ENTRY ANIMATION (ONLY BOX)
        loginLayout.startAnimation(fadeIn);
        startBorderPulse(loginLayout);

        // 🔁 SWITCH TO SIGNUP
        tvGoSignup.setOnClickListener(v -> {

            stopBorderPulse();

            loginLayout.startAnimation(slideDown);
            loginLayout.setVisibility(View.GONE);

            signupLayout.setVisibility(View.VISIBLE);
            signupLayout.startAnimation(slideUp);

            startBorderPulse(signupLayout);
        });

        // 🔁 SWITCH TO LOGIN
        tvGoLogin.setOnClickListener(v -> {

            stopBorderPulse();

            signupLayout.startAnimation(slideDown);
            signupLayout.setVisibility(View.GONE);

            loginLayout.setVisibility(View.VISIBLE);
            loginLayout.startAnimation(slideUp);

            startBorderPulse(loginLayout);
        });

        // 🔐 LOGIN BUTTON
        btnLogin.setOnClickListener(v -> {

            String email = etLoginEmail.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (db.loginUser(email, pass)) {

                SharedPreferences.Editor editor =
                        getSharedPreferences("EVAPP", MODE_PRIVATE).edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putString("userEmail", email);
                editor.apply();

                startActivity(new Intent(this, MainActivity.class));
                finish();

            } else {
                Toast.makeText(this,
                        "Invalid credentials. Please Sign Up first.",
                        Toast.LENGTH_LONG).show();
            }
        });

        // 📝 SIGNUP BUTTON
        btnSignup.setOnClickListener(v -> {

            String email = etSignupEmail.getText().toString().trim();
            String pass = etSignupPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (db.registerUser(email, pass)) {

                Toast.makeText(this,
                        "Registration successful. Please login.",
                        Toast.LENGTH_LONG).show();

                stopBorderPulse();

                signupLayout.startAnimation(slideDown);
                signupLayout.setVisibility(View.GONE);

                loginLayout.setVisibility(View.VISIBLE);
                loginLayout.startAnimation(slideUp);

                startBorderPulse(loginLayout);

            } else {
                Toast.makeText(this,
                        "User already exists",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // 🔥 BORDER COLOR PULSE ANIMATION
    private void startBorderPulse(View target) {

        borderPulse = ObjectAnimator.ofArgb(
                target,
                "backgroundTint",
                0xFF7E57C2,
                0xFFFF4081
        );
        borderPulse.setDuration(1200);
        borderPulse.setRepeatMode(ObjectAnimator.REVERSE);
        borderPulse.setRepeatCount(ObjectAnimator.INFINITE);
        borderPulse.start();
    }

    private void stopBorderPulse() {
        if (borderPulse != null) {
            borderPulse.cancel();
        }
    }
}
