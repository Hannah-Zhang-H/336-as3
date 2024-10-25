package com.hanz.youmetalk;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.hanz.youmetalk.databinding.ActivityResetPasswordBinding;

import java.util.Objects;

/**
 * ResetPasswordActivity handles password reset requests by sending a reset link to the user's email.
 * <p>
 * Key Features:
 * - Firebase Authentication: Sends password reset emails to verified users.
 * - Edge-to-Edge UI: Applies padding for system bars to optimize the UI.
 * <p>
 * Main Methods:
 * - `passwordReset(String email)`: Sends a reset email to the provided address and provides feedback on success or failure.
 */

public class ResetPasswordActivity extends AppCompatActivity {
    private TextInputEditText editTextForget;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflating the layout takes the XML source and makes View objects
        com.hanz.youmetalk.databinding.ActivityResetPasswordBinding resetPasswordLayout = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        // Get the root View
        setContentView(resetPasswordLayout.getRoot());

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Assign values
        editTextForget = resetPasswordLayout.editTextForget;
        Button buttonForget = resetPasswordLayout.buttonForget;

        auth = FirebaseAuth.getInstance();

        buttonForget.setOnClickListener(view -> {
            String email = Objects.requireNonNull(editTextForget.getText()).toString();
            if (!email.isEmpty()) passwordReset(email);
        });

    }

    public void passwordReset(String email) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Please check your email to reset the password.", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(this, "There is a problem with your email.", Toast.LENGTH_SHORT).show();

        });
    }
}