package com.hanz.youmetalk;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.hanz.youmetalk.databinding.ActivityMainBinding;
import com.hanz.youmetalk.databinding.ActivityProfileBinding;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding profileLayout;
    FirebaseAuth auth;
    private CircleImageView imageViewProfile;
    private TextInputEditText editTextUserNameUpdate;
    private Button buttonUpdate;

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

        buttonUpdate.setOnClickListener(view -> {

        });
    }
}