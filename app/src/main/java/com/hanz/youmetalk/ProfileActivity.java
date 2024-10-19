package com.hanz.youmetalk;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
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

    String currentImage, currentYouMeID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileLayout = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(profileLayout.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageViewProfile = profileLayout.imageViewCircleProfile;
        editTextUserNameUpdate = profileLayout.editTextUserNameUpdate;
        editTextYouMeIdUpdate = profileLayout.editTextYouMeIdUpdate;
        buttonUpdate = profileLayout.buttonUpdate;

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();

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
                    } else {
                        imageControl = false;
                    }
                });

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
                    currentYouMeID = snapshot.child("youMeID").getValue(String.class);
                    currentImage = snapshot.child("image").getValue(String.class);

                    if (name != null) {
                        editTextUserNameUpdate.setText(name);
                    }

                    if (currentYouMeID != null) {
                        editTextYouMeIdUpdate.setText(currentYouMeID);
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
        String youMeID = editTextYouMeIdUpdate.getText().toString().trim();

        if (userName.isEmpty() || youMeID.isEmpty()) {
            Toast.makeText(this, "Username or YouMeID cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if YouMeID is unique before proceeding
        reference.child("Users").orderByChild("youMeID").equalTo(youMeID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && !currentYouMeID.equals(youMeID)) {
                    // YouMeID is already used by another user
                    Toast.makeText(ProfileActivity.this, "YouMeID is already taken.", Toast.LENGTH_SHORT).show();
                } else {
                    // Proceed with the update
                    try {
                        updateProfile(userName, youMeID);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to check YouMeID.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateProfile(String userName, String youMeID) throws FileNotFoundException {
        buttonUpdate.setEnabled(false); // disable the button

        // Only update the fields that have changed
        if (!userName.equals(editTextUserNameUpdate.getText().toString())) {
            reference.child("Users").child(firebaseUser.getUid()).child("userName").setValue(userName);
        }

        if (!youMeID.equals(currentYouMeID)) {
            reference.child("Users").child(firebaseUser.getUid()).child("youMeID").setValue(youMeID);
        }

        if (imageControl) {
            // Update the image only if the user has selected a new one
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4; // compress rate
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageData = baos.toByteArray();

                UUID randomID = UUID.randomUUID();
                String imageName = "images/" + randomID + ".jpg";
                StorageReference imageRef = storageReference.child(imageName);

                imageRef.putBytes(imageData).addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String filePath = uri.toString();
                        reference.child("Users").child(firebaseUser.getUid()).child("image").setValue(filePath)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(ProfileActivity.this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                                    navigateToMainActivity(userName);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                                    buttonUpdate.setEnabled(true);
                                });
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                    buttonUpdate.setEnabled(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show();
                buttonUpdate.setEnabled(true);
            }
        } else {
            // Do not update the image if no new image was selected
            navigateToMainActivity(userName);
        }
    }


    private void navigateToMainActivity(String userName) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("userName", userName);
        startActivity(intent);
        finish();
    }

    public void imageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imageChooserLauncher.launch(intent);
    }
}
