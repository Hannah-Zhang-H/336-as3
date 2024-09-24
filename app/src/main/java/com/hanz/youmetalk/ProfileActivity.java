package com.hanz.youmetalk;

import android.content.Intent;
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
import com.hanz.youmetalk.databinding.ActivityMainBinding;
import com.hanz.youmetalk.databinding.ActivityProfileBinding;
import com.squareup.picasso.Picasso;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding profileLayout;
    private CircleImageView imageViewProfile;
    private TextInputEditText editTextUserNameUpdate;
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

    String image;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflating the layout takes the XML source and makes View objects
        profileLayout = ActivityProfileBinding.inflate(getLayoutInflater());
        // Get the root View
        setContentView(profileLayout.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageViewProfile = profileLayout.imageViewCircleProfile;
        editTextUserNameUpdate = profileLayout.editTextUserNameUpdate;
        buttonUpdate = profileLayout.buttonUpdate;

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();

        // Init ActivityResultLauncher
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

        // Call this function to display the current information of the user
        getUserInfo();


        imageViewProfile.setOnClickListener(view -> {
            imageChooser();
        });

        buttonUpdate.setOnClickListener(view -> {
            updateProfile();
        });
    }


    public void getUserInfo() {
        reference.child("Users").child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("userName").getValue().toString();
                image = snapshot.child("image").getValue().toString();
                editTextUserNameUpdate.setText(name);

                if (image.equals("null")) imageViewProfile.setImageResource(R.drawable.account);
                else {
                    Picasso.get().load(image).into(imageViewProfile);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    public void updateProfile() {
        String userName = editTextUserNameUpdate.getText().toString();
        reference.child("Users").child(firebaseUser.getUid()).child("userName").setValue(userName);

        if(imageControl)
        {
            UUID randomID = UUID.randomUUID();
            String imageName = "images/" + randomID + ".jpg";
            storageReference.child(imageName).putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                StorageReference myStorageRef = firebaseStorage.getReference(imageName);
                myStorageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String filePath = uri.toString();
                    reference.child("Users").child(auth.getUid()).child("image").setValue(filePath).addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Successfully stored to database.", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to store to database.", Toast.LENGTH_SHORT).show();

                    });

                });
            });
        }
        else {
            reference.child("Users").child(auth.getUid()).child("image").setValue("null");
        }

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