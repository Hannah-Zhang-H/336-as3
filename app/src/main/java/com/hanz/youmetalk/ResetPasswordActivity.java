package com.hanz.youmetalk;

import android.os.Bundle;
import android.widget.Button;
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
import com.google.firebase.auth.FirebaseAuth;
import com.hanz.youmetalk.databinding.ActivityResetPasswordBinding;
import com.hanz.youmetalk.databinding.ActivitySignUpBinding;

public class ResetPasswordActivity extends AppCompatActivity {
    private ActivityResetPasswordBinding resetPasswordLayout;
    private TextInputEditText editTextForget;
    private Button buttonForget;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflating the layout takes the XML source and makes View objects
        resetPasswordLayout = ActivityResetPasswordBinding.inflate(getLayoutInflater());
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
        buttonForget = resetPasswordLayout.buttonForget;

        auth = FirebaseAuth.getInstance();

        buttonForget.setOnClickListener(view -> {
            String email = editTextForget.getText().toString();
            if(!email.isEmpty()) passwordReset(email);
        });

    }

    public  void passwordReset(String email)
    {
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if(task.isSuccessful())
            {
                Toast.makeText(this, "Please check your email to reset the password.", Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(this, "There is a problem with your email.", Toast.LENGTH_SHORT).show();

        });
    }
}