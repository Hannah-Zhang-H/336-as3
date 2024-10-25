package com.hanz.youmetalk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hanz.youmetalk.databinding.ActivityLoginBinding;

import java.util.Objects;

/**
 * LoginActivity manages the user login process in YouMeTalk, including sign-in, account creation, and password reset options.
 *
 * Key Features:
 * - Redirects already signed-in users to MainActivity on start.
 * - Uses Firebase Authentication for email/password login.
 * - Provides navigation to SignUpActivity and ResetPasswordActivity.
 *
 * Main Methods:
 * - `signIn(String email, String password)`: Authenticates and redirects to MainActivity upon success.
 *
 * UI:
 * - Configures EdgeToEdge for a seamless full-screen experience.
 * - Uses view binding to simplify UI setup.
 */


public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;

    // Database
    FirebaseAuth auth;
    FirebaseUser firebaseUser;

    // When a user has logged in, then come back to the app, then should display MainActivity.
    @Override
    protected void onStart() {
        super.onStart();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflating the layout takes the XML source and makes View objects
        com.hanz.youmetalk.databinding.ActivityLoginBinding loginLayout = ActivityLoginBinding.inflate(getLayoutInflater());
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
        Button buttonSignin = loginLayout.buttonSignin;
        TextView textViewSignup = loginLayout.textViewSignup;
        TextView textViewForget = loginLayout.textViewForgetPassword;
        auth = FirebaseAuth.getInstance();


        // Add click listener to buttonSignin
        buttonSignin.setOnClickListener(view -> {
            String email = Objects.requireNonNull(editTextEmail.getText()).toString();
            String password = Objects.requireNonNull(editTextPassword.getText()).toString();
            if (!email.isEmpty() && !password.isEmpty()) signIn(email, password);
            else
                Toast.makeText(LoginActivity.this, "Please enter an email and password.", Toast.LENGTH_SHORT).show();

        });


        textViewSignup.setOnClickListener(view -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });

        textViewForget.setOnClickListener(view -> {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }


    public void signIn(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                Toast.makeText(LoginActivity.this, "Sign in successful.", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(LoginActivity.this, "Sign in not successful.", Toast.LENGTH_SHORT).show();

        });
    }


}