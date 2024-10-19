package com.hanz.youmetalk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hanz.youmetalk.databinding.ActivitySignUpBinding;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding signUpLayout;
    private CircleImageView imageViewCircle;
    private TextInputEditText editTextEmailSignup, editTextPasswordSignup, editTextTextUserNameSignup, editTextYouMeIdSignup;
    private Button buttonRegister;

    boolean imageControl = false;

    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference reference;

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    private ActivityResultLauncher<Intent> imageChooserLauncher;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signUpLayout = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(signUpLayout.getRoot());

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // Assign UI elements
        imageViewCircle = signUpLayout.imageViewCircle;
        editTextEmailSignup = signUpLayout.editTextEmailSignup;
        editTextPasswordSignup = signUpLayout.editTextPasswordSignup;
        editTextTextUserNameSignup = signUpLayout.editTextUserNameSignup;
        editTextYouMeIdSignup = signUpLayout.editTextYouMeIdSignup;
        buttonRegister = signUpLayout.buttonRegister;

        // Initialize image chooser launcher
        imageChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            imageUri = data.getData();
                            Picasso.get().load(imageUri).into(imageViewCircle);
                            imageControl = true;
                        }
                    }
                }
        );

        // Set click listeners
        imageViewCircle.setOnClickListener(view -> imageChooser());

        buttonRegister.setOnClickListener(view -> {
            String email = editTextEmailSignup.getText().toString();
            String password = editTextPasswordSignup.getText().toString();
            String userName = editTextTextUserNameSignup.getText().toString();
            String youMeId = editTextYouMeIdSignup.getText().toString();

            if (email.isEmpty() || password.isEmpty() || userName.isEmpty() || youMeId.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            } else {
                checkYouMeIdUnique(youMeId, email, password, userName);
            }
        });
    }

    // 检查 YouMeID 是否唯一
    private void checkYouMeIdUnique(String youMeId, String email, String password, String userName) {
        reference.child("Users").orderByChild("youMeId").equalTo(youMeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Toast.makeText(SignUpActivity.this, "YouMeID already exists. Please choose another.", Toast.LENGTH_SHORT).show();
                } else {
                    signup(email, password, userName, youMeId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SignUpActivity.this, "Error checking YouMeID uniqueness.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 注册流程，只有在 YouMeID 唯一时才调用
    private void signup(String email, String password, String userName, String youMeId) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = auth.getUid();
                if (uid != null) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("userName", userName);
                    userMap.put("youMeId", youMeId);

                    if (imageControl) {
                        uploadUserImage(userMap, uid);
                    } else {
                        userMap.put("image", "null");
                        reference.child("Users").child(uid).setValue(userMap).addOnSuccessListener(unused -> {
                            Toast.makeText(SignUpActivity.this, "Successfully registered and stored to database.", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(SignUpActivity.this, "Failed to store to database.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } else {
                Toast.makeText(SignUpActivity.this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadUserImage(Map<String, Object> userMap, String uid) {
        UUID randomID = UUID.randomUUID();
        String imageName = "images/" + randomID + ".jpg";
        storageReference.child(imageName).putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            storageReference.child(imageName).getDownloadUrl().addOnSuccessListener(uri -> {
                String filePath = uri.toString();
                userMap.put("image", filePath);
                reference.child("Users").child(uid).setValue(userMap).addOnSuccessListener(unused -> {
                    Toast.makeText(SignUpActivity.this, "Successfully registered and stored to database.", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                }).addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "Failed to store to database.", Toast.LENGTH_SHORT).show();
                });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(SignUpActivity.this, "Image upload failed.", Toast.LENGTH_SHORT).show();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
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
