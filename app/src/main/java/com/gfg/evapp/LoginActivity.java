package com.gfg.evapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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

        // 📦 DATABASE
        db = new DBHelper(this);

        // 🧩 LAYOUTS
        loginLayout = findViewById(R.id.loginLayout);
        signupLayout = findViewById(R.id.signupLayout);

        // 🔐 LOGIN VIEWS
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoSignup = findViewById(R.id.tvGoSignup);

        // 📝 SIGNUP VIEWS
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        // 🔁 GO TO SIGNUP (SLIDE RIGHT)
        tvGoSignup.setOnClickListener(v -> {
            loginLayout.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
            loginLayout.setVisibility(View.GONE);

            signupLayout.setVisibility(View.VISIBLE);
            signupLayout.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
        });

        // 🔁 GO TO LOGIN (SLIDE LEFT)
        tvGoLogin.setOnClickListener(v -> {
            signupLayout.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
            signupLayout.setVisibility(View.GONE);

            loginLayout.setVisibility(View.VISIBLE);
            loginLayout.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
        });

        // 🔐 LOGIN BUTTON (EMAIL VALIDATION ADDED)
        btnLogin.setOnClickListener(v -> {

            String email = etLoginEmail.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Gmail validation
            if (!isValidGmail(email)) {
                etLoginEmail.setError("Enter valid Gmail (example@gmail.com)");
                etLoginEmail.requestFocus();
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

        // 📝 SIGNUP BUTTON (EMAIL VALIDATION ADDED)
        btnSignup.setOnClickListener(v -> {

            String email = etSignupEmail.getText().toString().trim();
            String pass = etSignupPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Gmail validation
            if (!isValidGmail(email)) {
                etSignupEmail.setError("Enter valid Gmail (example@gmail.com)");
                etSignupEmail.requestFocus();
                return;
            }

            if (db.registerUser(email, pass)) {

                Toast.makeText(this,
                        "Registration successful. Please login.",
                        Toast.LENGTH_LONG).show();

                signupLayout.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
                signupLayout.setVisibility(View.GONE);

                loginLayout.setVisibility(View.VISIBLE);
                loginLayout.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left));

            } else {
                Toast.makeText(this,
                        "User already exists",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ✅ STRICT GMAIL VALIDATION METHOD
    private boolean isValidGmail(String email) {
        return email.contains("@") && email.endsWith("@gmail.com");
    }
}
