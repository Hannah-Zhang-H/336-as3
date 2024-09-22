package com.hanz.youmetalk;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.hanz.youmetalk.databinding.ActivityLoginBinding;
import com.hanz.youmetalk.databinding.ActivitySignUpBinding;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding signUpLayout;
    private CircleImageView imageViewCircle;
    private TextInputEditText editTextEmailSignup, editTextPasswordSignup, editTextTextUserNameSignup;
    private Button buttonRegister;


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

        // Assign values
        imageViewCircle = signUpLayout.imageViewCircle;
        editTextEmailSignup = signUpLayout.editTextEmailSignup;
        editTextPasswordSignup = signUpLayout.editTextPasswordSignup;
        editTextTextUserNameSignup = signUpLayout.editTextUserNameSignup;
        buttonRegister = signUpLayout.buttonRegister;

    }
}