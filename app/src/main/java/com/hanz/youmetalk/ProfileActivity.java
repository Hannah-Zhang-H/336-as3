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
import com.hanz.youmetalk.databinding.ActivityMainBinding;
import com.hanz.youmetalk.databinding.ActivityProfileBinding;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import java.io.ByteArrayOutputStream;


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
            try {
                updateProfile();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public void getUserInfo() {
        reference.child("Users").child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 确保数据存在
                if (snapshot.exists()) {
                    String name = snapshot.child("userName").getValue(String.class);
                    image = snapshot.child("image").getValue(String.class);

                    // 防止空指针异常
                    if (name != null) {
                        editTextUserNameUpdate.setText(name);
                    }

                    if (image != null && !image.equals("null")) {
                        Glide.with(ProfileActivity.this)
                                .load(image)
                                .placeholder(R.drawable.account)
                                .error(R.drawable.account)
                                .diskCacheStrategy(DiskCacheStrategy.ALL) // 使用 Glide 的全缓存策略
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

    public void updateProfile() throws FileNotFoundException {
        String userName = editTextUserNameUpdate.getText().toString().trim();

        if (userName.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonUpdate.setEnabled(false); // 禁用按钮以防止重复上传
        reference.child("Users").child(firebaseUser.getUid()).child("userName").setValue(userName);

        if (imageControl) {
            try {
                // 1. 将 imageUri 转换为 Bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4; // 压缩比例
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);

                // 2. 将 Bitmap 转换为字节数组
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageData = baos.toByteArray();

                // 3. 上传压缩后的图片
                UUID randomID = UUID.randomUUID();
                String imageName = "images/" + randomID + ".jpg";
                StorageReference imageRef = storageReference.child(imageName);

                imageRef.putBytes(imageData).addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String filePath = uri.toString();
                        reference.child("Users").child(auth.getUid()).child("image").setValue(filePath)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                                    navigateToMainActivity(userName);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                                    buttonUpdate.setEnabled(true); // 重新启用按钮
                                });
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                    buttonUpdate.setEnabled(true); // 重新启用按钮
                });

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show();
                buttonUpdate.setEnabled(true); // 重新启用按钮
            }
        } else {
            reference.child("Users").child(auth.getUid()).child("image").setValue("null")
                    .addOnCompleteListener(task -> navigateToMainActivity(userName));
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

