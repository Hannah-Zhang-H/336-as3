package com.hanz.youmetalk;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hanz.youmetalk.databinding.ActivityMyTalkBinding;

public class MyTalkActivity extends AppCompatActivity {

    private ActivityMyTalkBinding myTalkLayout;
    private RecyclerView recyclerViewMessageArea;
    private ImageView imageViewBack;
    private TextView textViewChatFriendName;
    private EditText editTextMessage;
    private FloatingActionButton fab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myTalkLayout = ActivityMyTalkBinding.inflate(getLayoutInflater());
        setContentView(myTalkLayout.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerViewMessageArea = myTalkLayout.recyclerViewTalkArea;
        imageViewBack = myTalkLayout.imageViewBack;
        textViewChatFriendName = myTalkLayout.textViewChatFriendName;
        editTextMessage = myTalkLayout.editTextMessage;
        fab = myTalkLayout.fab;

    }
}