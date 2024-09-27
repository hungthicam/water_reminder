package com.cam.water_reminder.Fragment;

import static android.app.Activity.RESULT_OK;

import com.cam.water_reminder.model.Edit_Acc;
import com.cam.water_reminder.R;
import com.cam.water_reminder.login;
import com.cam.water_reminder.ChangePasswordActivity;
import com.cam.water_reminder.SoundNotificationsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AccountFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView avatarImageView;
    private TextView nameTextView, emailTextView, phoneTextView;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private DatabaseReference databaseRef;
    private Button btn_edit, btn_logout, btn_change_password, btn_sounds_notifications;

    private Uri selectedImageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        avatarImageView = view.findViewById(R.id.avatarImageView);
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        phoneTextView = view.findViewById(R.id.phoneTextView);
        btn_logout = view.findViewById(R.id.btn_logout);
        btn_edit = view.findViewById(R.id.btn_edit);
        btn_change_password = view.findViewById(R.id.btn_change_password);
        btn_sounds_notifications = view.findViewById(R.id.btn_sounds_notifications);

        btn_edit.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Edit_Acc.class);
            startActivityForResult(intent, 2);
        });

        btn_change_password.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        btn_sounds_notifications.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SoundNotificationsActivity.class);
            startActivity(intent);
        });

        btn_logout.setOnClickListener(v -> logout());

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        avatarImageView.setOnClickListener(v -> openImagePicker());

        loadAvatarImage();
        loadUserInfo();

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2 && resultCode == RESULT_OK) {
            loadUserInfo();
        }

        // Xử lý ảnh đại diện
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Picasso.get().load(selectedImageUri).into(avatarImageView);
            uploadAvatarImage(selectedImageUri);
        }
    }

    private void loadAvatarImage() {
        String userId = getUserId();
        if (userId != null) {
            StorageReference avatarRef = storageRef.child("avatar/" + userId + ".jpg");
            avatarRef.getDownloadUrl().addOnSuccessListener(uri -> Picasso.get().load(uri).into(avatarImageView))
                    .addOnFailureListener(exception -> Toast.makeText(getContext(), "Failed to load avatar: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void uploadAvatarImage(Uri imageUri) {
        String userId = getUserId();
        if (userId != null) {
            StorageReference avatarRef = storageRef.child("avatar/" + userId + ".jpg");
            avatarRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(getContext(), "Avatar Image Uploaded", Toast.LENGTH_SHORT).show();
                loadAvatarImage();
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Avatar upload failed", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadUserInfo() {
        String userId = getUserId();
        if (userId != null) {
            DatabaseReference userRef = databaseRef.child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        String phone = dataSnapshot.child("phone").getValue(String.class);

                        nameTextView.setText("Name: " + username);
                        emailTextView.setText("Email: " + email);
                        phoneTextView.setText("Phone: " + (phone != null ? phone : ""));
                    } else {
                        Toast.makeText(getContext(), "User information not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(getContext(), "Error reading data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private String getUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void logout() {
        SharedPreferences preferences = getActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();  // Clear all stored data
        editor.apply();

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(getActivity(), login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();  // Kết thúc Activity chứa Fragment
    }
}
