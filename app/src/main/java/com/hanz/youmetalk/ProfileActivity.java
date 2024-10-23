package com.hanz.youmetalk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hanz.youmetalk.databinding.ActivityProfileBinding;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    // Initialize ExecutorService for background tasks
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ProgressBar progressBar; // ProgressBar
    private TextView progressText;  // ProgressBar text to remind the user
    private ActivityProfileBinding profileLayout;
    private CircleImageView imageViewProfile;
    private TextInputEditText editTextUserNameUpdate, editTextYouMeIdUpdate;
    private Button buttonUpdate;

    FirebaseAuth auth;
    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference reference;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    private ActivityResultLauncher<Intent> imageChooserLauncher;
    Uri imageUri;
    boolean imageControl = false;

    String currentImage, currentYouMeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileLayout = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(profileLayout.getRoot());

        // Initialize Firebase components
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();

        // Assign UI elements
        imageViewProfile = profileLayout.imageViewCircleProfile;
        editTextUserNameUpdate = profileLayout.editTextUserNameUpdate;
        editTextYouMeIdUpdate = profileLayout.editTextYouMeIdUpdate;
        buttonUpdate = profileLayout.buttonUpdate;
        progressBar = profileLayout.uploadProgressBarProfile;  // Initialize the ProgressBar
        progressText = profileLayout.uploadProgressTextProfile; // Initialize the ProgressText

        // Initialize ActivityResultLauncher for image choosing
        imageChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            imageUri = data.getData();
                            Picasso.get().load(imageUri).into(imageViewProfile);
                            imageControl = true;
                        }
                    }
                });

        // Get current user information
        getUserInfo();
        imageViewProfile.setOnClickListener(view -> imageChooser());
        buttonUpdate.setOnClickListener(view -> {
            try {
                checkAndUpdateProfile();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void getUserInfo() {
        reference.child("Users").child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("userName").getValue(String.class);
                    currentYouMeId = snapshot.child("youMeId").getValue(String.class);
                    currentImage = snapshot.child("image").getValue(String.class);

                    if (name != null) {
                        editTextUserNameUpdate.setText(name);
                    }

                    if (currentYouMeId != null) {
                        editTextYouMeIdUpdate.setText(currentYouMeId);
                    }

                    if (currentImage != null && !currentImage.equals("null") && !isDestroyed() && !isFinishing()) {
                        Glide.with(ProfileActivity.this)
                                .load(currentImage)
                                .placeholder(R.drawable.account)
                                .error(R.drawable.account)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imageViewProfile);
                    } else {
                        imageViewProfile.setImageResource(R.drawable.account);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load user info.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void checkAndUpdateProfile() throws FileNotFoundException {
        String userName = editTextUserNameUpdate.getText().toString().trim();
        String youMeId = editTextYouMeIdUpdate.getText().toString().trim();

        if (userName.isEmpty() || youMeId.isEmpty()) {
            Toast.makeText(this, "Username or YouMeID cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if YouMeID is unique before proceeding
        reference.child("Users").orderByChild("youMeId").equalTo(youMeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && !currentYouMeId.equals(youMeId)) {
                    // YouMeID is already used by another user, stop the update process
                    Toast.makeText(ProfileActivity.this, "YouMeID is already taken.", Toast.LENGTH_SHORT).show();
                } else {
                    // Proceed with the update if YouMeID is unique
                    updateProfile(userName, youMeId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to check YouMeID.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void updateProfile(String userName, String youMeId) {
        if (!userName.equals(currentYouMeId)) {
            reference.child("Users").child(firebaseUser.getUid()).child("userName").setValue(userName);
        }

        if (!youMeId.equals(currentYouMeId)) {
            reference.child("Users").child(firebaseUser.getUid()).child("youMeId").setValue(youMeId);
        }

        if (imageControl) {
            progressBar.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);

            executorService.execute(() -> {
                UUID randomID = UUID.randomUUID();
                String imageName = "images/" + randomID + ".jpg";
                StorageReference imageRef = storageReference.child(imageName);

                imageRef.putFile(imageUri).addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    runOnUiThread(() -> {
                        progressBar.setProgress((int) progress);
                        progressText.setText("Upload Progress: " + (int) progress + "%");
                    });
                }).addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String filePath = uri.toString();
                        reference.child("Users").child(firebaseUser.getUid()).child("image").setValue(filePath)
                                .addOnSuccessListener(unused -> runOnUiThread(() -> {
                                    Toast.makeText(ProfileActivity.this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                    progressText.setVisibility(View.GONE);
                                }))
                                .addOnFailureListener(e -> runOnUiThread(() -> {
                                    Toast.makeText(ProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                    progressText.setVisibility(View.GONE);
                                }));
                    });
                }).addOnFailureListener(e -> runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                }));
            });
        } else {
            Toast.makeText(this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
        }
    }


    public void imageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imageChooserLauncher.launch(intent);
    }
}
