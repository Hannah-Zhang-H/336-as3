package com.hanz.youmetalk;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.hanz.youmetalk.databinding.ActivityLoginBinding;
import com.hanz.youmetalk.databinding.ActivityMainBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding loginLayout;
    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonSignin;
    private TextView textViewSignup, textViewForget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflating the layout takes the XML source and makes View objects
        loginLayout = ActivityLoginBinding.inflate(getLayoutInflater());
        // Get the root View
        setContentView(loginLayout.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Assign values
        editTextEmail = loginLayout.editTextEmail;
        editTextPassword = loginLayout.editTextPassword;
        buttonSignin = loginLayout.buttonSignin;
        textViewSignup = loginLayout.textViewSignup;
        textViewForget = loginLayout.textViewForgetPassword;
    }
}