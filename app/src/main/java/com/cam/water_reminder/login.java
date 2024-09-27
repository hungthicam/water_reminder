package com.cam.water_reminder;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cam.water_reminder.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private CheckBox rememberMeCheckBox;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.input_username);
        passwordEditText = findViewById(R.id.input_pass);
        loginButton = findViewById(R.id.sign_in_button);
        rememberMeCheckBox = findViewById(R.id.remember_me_checkbox);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if the UID is already remembered
        String savedUID = sharedPreferences.getString("uid", null);
        if (savedUID != null) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && savedUID.equals(user.getUid())) {
                // UID found, verify if session is still valid
                checkSessionValidity(user);
            } else {
                // If user is null or UID doesn't match, clear stored UID
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("uid");
                editor.apply();
            }
        }

        // Toggle password visibility
        ImageButton togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        togglePasswordVisibility.setOnClickListener(v -> {
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        // Redirect to signup activity
        TextView signUpRedirect = findViewById(R.id.sign_up_redirect);
        signUpRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, signup.class);
            startActivity(intent);
        });

        TextView forgotPassword = findViewById(R.id.forgot_password);
        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });


        // Login button event
        loginButton.setOnClickListener(v -> loginUser());
    }
    private void checkSessionValidity(FirebaseUser user) {
        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Token hợp lệ, tiếp tục cho phép người dùng truy cập vào MainActivity
                Intent intent = new Intent(login.this, MainActivity.class);
                intent.putExtra("uid", user.getUid());
                startActivity(intent);
                finish();
            } else {
                // Token không hợp lệ (do mật khẩu thay đổi hoặc phiên hết hạn)
                FirebaseAuth.getInstance().signOut();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("uid");  // Xóa UID lưu trữ
                editor.apply();

                Toast.makeText(login.this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                // Chuyển hướng người dùng về màn hình đăng nhập
            }
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(login.this, "Please enter complete information", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing in...");
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Log in successfully
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(login.this, "Successful login!", Toast.LENGTH_SHORT).show();

                            String uid = user.getUid(); // Get UID from Firebase

                            // Save UID after successful login to SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("uid", uid);  // Always save UID
                            editor.putBoolean("remember", rememberMeCheckBox.isChecked());  // Save status "Remember Me"
                            editor.apply();

                            // Pass UID to MainActivity via Intent (Optional)
                            Intent intent = new Intent(login.this, MainActivity.class);
                            intent.putExtra("uid", uid);  // Pass UID via Intent
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        // Login failed
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        Toast.makeText(login.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
