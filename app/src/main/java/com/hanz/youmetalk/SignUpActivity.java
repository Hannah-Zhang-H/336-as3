package com.hanz.youmetalk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hanz.youmetalk.databinding.ActivitySignUpBinding;
import com.squareup.picasso.Picasso;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding signUpLayout;
    private CircleImageView imageViewCircle;
    private TextInputEditText editTextEmailSignup, editTextPasswordSignup, editTextTextUserNameSignup;
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
        // Inflating the layout takes the XML source and makes View objects
        signUpLayout = ActivitySignUpBinding.inflate(getLayoutInflater());
        // Get the root View
        setContentView(signUpLayout.getRoot());

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Init ActivityResultLauncher
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
                    } else {
                        imageControl = false;
                    }
                });


        // Assign values
        imageViewCircle = signUpLayout.imageViewCircle;
        editTextEmailSignup = signUpLayout.editTextEmailSignup;
        editTextPasswordSignup = signUpLayout.editTextPasswordSignup;
        editTextTextUserNameSignup = signUpLayout.editTextUserNameSignup;
        buttonRegister = signUpLayout.buttonRegister;
        auth = FirebaseAuth.getInstance();

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        imageViewCircle.setOnClickListener(view -> {
            imageChooser();
        });

        buttonRegister.setOnClickListener(view -> {
            String email = editTextEmailSignup.getText().toString();
            String password = editTextPasswordSignup.getText().toString();
            String userName = editTextTextUserNameSignup.getText().toString();

            if (email.isEmpty() || password.isEmpty() || userName.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            } else {
                signup(email, password, userName);
            }
        });

    }

    private void signup(String email, String password, String userName) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if(task.isSuccessful())
            {
                reference.child("Users").child(auth.getUid()).child("userName").setValue(userName);

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
//                intent.putExtra("userName", userName);
                startActivity(intent);
                finish();
            }
            else Toast.makeText(this, "Invalid input???.", Toast.LENGTH_SHORT).show();

        });
    }

    public void imageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imageChooserLauncher.launch(intent);
    }




}