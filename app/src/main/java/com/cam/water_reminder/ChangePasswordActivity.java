package com.cam.water_reminder;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText oldPassEditText, newPassEditText, confirmPassEditText;
    private Button changePasswordButton, closeAccountButton;
    private TextView resultTextView, countdownTextView;
    private CheckBox showPasswordCheckbox;
    private FirebaseAuth mAuth;
    private int daysLeft = 10; // Starting with 10 days for account deletion
    private Handler countdownHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        oldPassEditText = findViewById(R.id.input_old_pass);
        newPassEditText = findViewById(R.id.input_new_pass);
        confirmPassEditText = findViewById(R.id.input_confirm_new_pass);
        changePasswordButton = findViewById(R.id.change_password_button);
        resultTextView = findViewById(R.id.change_password_result);
        ImageButton backButton = findViewById(R.id.button_back);
        showPasswordCheckbox = findViewById(R.id.show_password_checkbox);
        closeAccountButton = findViewById(R.id.close_account_button);
        countdownTextView = findViewById(R.id.account_deletion_countdown);

        // Back button functionality
        backButton.setOnClickListener(v -> finish());

        // Change password button functionality
        changePasswordButton.setOnClickListener(v -> changePassword());

        // Show/hide password functionality
        showPasswordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                oldPassEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                newPassEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                confirmPassEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                oldPassEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                newPassEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                confirmPassEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        // Account closure button functionality
        closeAccountButton.setOnClickListener(v -> confirmAccountClosure());

        // Check if there's already a deletion request and start countdown
        startCountdown();
    }

    private void confirmAccountClosure() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Account Closure")
                .setMessage("Your account will be deleted in 10 days. Do you want to continue?")
                .setPositiveButton("Yes", (dialog, which) -> startDeletionCountdown())
                .setNegativeButton("No", null)
                .show();
    }

    private void startDeletionCountdown() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            long deletionTimeMillis = System.currentTimeMillis() + (daysLeft * 24 * 60 * 60 * 1000); // 10 days from now

            // Save deletion timestamp in Firebase under user's record
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.child("deletionTimestamp").setValue(deletionTimeMillis);

            // Start the countdown
            countdownHandler = new Handler();
            countdownHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (daysLeft > 0) {
                        countdownTextView.setText("Account will be deleted in " + daysLeft + " days. Tap here to cancel.");
                        daysLeft--;
                        countdownHandler.postDelayed(this, 24 * 60 * 60 * 1000); // Decrease count every 24 hours
                    } else {
                        deleteAccount();
                    }
                }
            });

            // Allow user to cancel the deletion process
            countdownTextView.setOnClickListener(v -> cancelAccountDeletion());
        }
    }


    private void cancelAccountDeletion() {
        countdownHandler.removeCallbacksAndMessages(null);
        countdownTextView.setText("");
        Toast.makeText(this, "Account deletion canceled.", Toast.LENGTH_SHORT).show();
        // Remove deletion timestamp from Firebase
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Delete user's data from Firebase Realtime Database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userRef = database.getReference("users").child(user.getUid());
            DatabaseReference historyRef = database.getReference("history").child(user.getUid());
            DatabaseReference historyOfDrinkingWaterRef = database.getReference("historyOfDrinkingWater").child(user.getUid());
            DatabaseReference reminderHistoryRef = database.getReference("reminder_history").child(user.getUid());

            userRef.removeValue();
            historyRef.removeValue();
            historyOfDrinkingWaterRef.removeValue();
            reminderHistoryRef.removeValue();

            // Delete Firebase Auth account
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChangePasswordActivity.this, "Account deletion failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startCountdown() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return; // User is not logged in
        }

        // Reference to the user's data in Firebase Database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("deletionTimestamp");

        // Get the deletion timestamp from Firebase
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Long deletionTimestamp = task.getResult().getValue(Long.class);
                if (deletionTimestamp != null) {
                    // Calculate the remaining time
                    long currentTimeMillis = System.currentTimeMillis();
                    long timeLeft = deletionTimestamp - currentTimeMillis;

                    if (timeLeft > 0) {
                        // Time left in days
                        daysLeft = (int) (timeLeft / (24 * 60 * 60 * 1000));

                        // Start countdown if there are still days left
                        countdownHandler = new Handler();
                        countdownHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (daysLeft > 0) {
                                    countdownTextView.setText("Account will be deleted in " + daysLeft + " days. Tap here to cancel.");
                                    daysLeft--;
                                    countdownHandler.postDelayed(this, 24 * 60 * 60 * 1000); // Decrease count every 24 hours
                                } else {
                                    deleteAccount(); // Time is up, delete the account
                                }
                            }
                        });

                        // Allow user to cancel the countdown
                        countdownTextView.setOnClickListener(v -> cancelAccountDeletion());

                    } else {
                        // Time has already passed, delete the account immediately
                        deleteAccount();
                    }
                }
            } else {
                Toast.makeText(ChangePasswordActivity.this, "Failed to retrieve deletion timestamp.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void changePassword() {
        String oldPassword = oldPassEditText.getText().toString().trim();
        String newPassword = newPassEditText.getText().toString().trim();
        String confirmPassword = confirmPassEditText.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(ChangePasswordActivity.this, "Please enter complete information", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            resultTextView.setText("New password and confirmation password do not match.");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Re-authenticate the user with the old password before changing it
            mAuth.signInWithEmailAndPassword(user.getEmail(), oldPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            resultTextView.setText("Password changed successfully!");
                            resultTextView.setTextColor(getResources().getColor(android.R.color.black));
                            finish();
                        } else {
                            resultTextView.setText("Change password failed: " + updateTask.getException().getMessage());
                        }
                    });
                } else {
                    resultTextView.setText("Old password is incorrect.");
                }
            });
        } else {
            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show();
        }
    }
}
