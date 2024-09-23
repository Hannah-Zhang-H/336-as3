package com.hanz.youmetalk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hanz.youmetalk.databinding.ActivityLoginBinding;
import com.hanz.youmetalk.databinding.ActivityMainBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding loginLayout;
    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonSignin;
    private TextView textViewSignup, textViewForget;

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
        auth = FirebaseAuth.getInstance();


        // Add click listener to buttonSignin
        buttonSignin.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();
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