package com.cam.water_reminder.model;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cam.water_reminder.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Edit_Acc extends AppCompatActivity {

    private EditText editName, editEmail, editPhone;
    private Button btnSave;
    private DatabaseReference databaseRef;
    private ImageButton btn_back;

    private String originalName, originalEmail, originalPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_acc);

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        btnSave = findViewById(R.id.btn_save);
        btn_back = findViewById(R.id.btn_back);

        // Thiết lập sự kiện quay lại khi nhấn nút "Back"
        btn_back.setOnClickListener(v -> showExitConfirmationDialog());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        // Load thông tin người dùng ban đầu
        loadUserInfo();

        btnSave.setOnClickListener(v -> {
            // Hiện thông báo xác nhận trước khi lưu
            showSaveConfirmationDialog();
        });
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        originalName = dataSnapshot.child("username").getValue(String.class);
                        originalEmail = dataSnapshot.child("email").getValue(String.class);
                        originalPhone = dataSnapshot.child("phone").getValue(String.class);

                        editName.setText(originalName);
                        editEmail.setText(originalEmail);
                        editPhone.setText(originalPhone != null ? originalPhone : "");
                    } else {
                        Toast.makeText(Edit_Acc.this, "User information not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(Edit_Acc.this, "Error reading data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showExitConfirmationDialog() {
        if (hasChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle("Exited without saving")
                    .setMessage("Do you want to save changes before exiting?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        saveChanges();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        finish(); // Close the activity without saving
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            finish(); // No changes, exit
        }
    }

    private void showSaveConfirmationDialog() {
        if (hasChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm save")
                    .setMessage("Are you sure you want to save these changes?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        saveChanges(); // Save changes
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            Toast.makeText(this, "There are no changes to save", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveChanges() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cập nhật thông tin người dùng trong Firebase
        databaseRef.child("username").setValue(name);
        databaseRef.child("email").setValue(email);
        databaseRef.child("phone").setValue(phone);

        // After successfully saving information
        Toast.makeText(this, "Account information has been updated", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);  // Mark successful operation
        finish();  // Close Activity
    }

    private boolean hasChanges() {
        String currentName = editName.getText().toString().trim();
        String currentEmail = editEmail.getText().toString().trim();
        String currentPhone = editPhone.getText().toString().trim();

        return !currentName.equals(originalName) || !currentEmail.equals(originalEmail) || !currentPhone.equals(originalPhone);
    }
}
