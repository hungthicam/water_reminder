package com.cam.water_reminder;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cam.water_reminder.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetPasswordButton;
    private FirebaseAuth mAuth;
    private ImageButton btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailEditText = findViewById(R.id.email_edit_text);
        resetPasswordButton = findViewById(R.id.reset_password_button);
        btn_back = findViewById(R.id.btn_back);

        btn_back.setOnClickListener(v -> {
            // Hiện thông báo xác nhận trước khi lưu
            finish();
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Reset password button event
        resetPasswordButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send reset password email
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, "Password reset email has been sent!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Failed to send email.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
