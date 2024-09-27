package com.hanz.youmetalk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hanz.youmetalk.databinding.ActivityMyTalkBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyTalkActivity extends AppCompatActivity {

    private ActivityMyTalkBinding myTalkLayout;
    private RecyclerView recyclerViewMessageArea;
    private ImageView imageViewBack;
    private TextView textViewChatFriendName;
    private EditText editTextMessage;
    private FloatingActionButton fab;

    String userName, friendName;

    FirebaseDatabase database;
    DatabaseReference reference;

    MessageAdapter messageAdapter;
    List<Model> list;


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
        recyclerViewMessageArea.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();


        imageViewBack = myTalkLayout.imageViewBack;
        textViewChatFriendName = myTalkLayout.textViewChatFriendName;
        editTextMessage = myTalkLayout.editTextMessage;
        fab = myTalkLayout.fab;



        userName = getIntent().getStringExtra("userName");
        friendName = getIntent().getStringExtra("friendName");

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        textViewChatFriendName.setText(friendName);

        imageViewBack.setOnClickListener(view ->
                startActivity(new Intent(this, MainActivity.class))
        );

        fab.setOnClickListener(view -> {
            String message = editTextMessage.getText().toString();
            if (!message.isEmpty()) {
                sendMessage(message);
                editTextMessage.setText("");
            }
        });

        getMessage();
    }

    private void getMessage() {
        reference.child("Messages").child(userName).child(friendName).addChildEventListener(new ChildEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Model model = snapshot.getValue(Model.class);
                list.add(model);
                messageAdapter.notifyDataSetChanged();
                recyclerViewMessageArea.scrollToPosition(list.size()-1);



            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        messageAdapter = new MessageAdapter(list, userName);
        recyclerViewMessageArea.setAdapter(messageAdapter);
    }


    private void sendMessage(String message) {
        // Make sure each message has a unique identifier, otherwise whenever the user delete one message,
        //  the same message on friend's phone will also get deleted.
        String key = reference.child("Messages").child(userName).child(friendName).push().getKey();
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("message", message);
        messageMap.put("from", userName);

        // Save the data into the database
        reference.child("Messages").child(userName).child(friendName).child(key).setValue(messageMap).addOnCompleteListener(task -> {
            if(task.isSuccessful())
            {
                reference.child("Messages").child(friendName).child(userName).child(key).setValue(messageMap);
            }
        });

    }
}