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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
    FirebaseUser firebaseUser;  // FirebaseUser 对象
    DatabaseReference reference;

    MessageAdapter messageAdapter;
    List<Model> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myTalkLayout = ActivityMyTalkBinding.inflate(getLayoutInflater());
        setContentView(myTalkLayout.getRoot());

        // 初始化 FirebaseUser 对象
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

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

        imageViewBack.setOnClickListener(view -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        fab.setOnClickListener(view -> {
            String message = editTextMessage.getText().toString();
            if (!message.isEmpty()) {
                sendMessage(message);
                editTextMessage.setText("");
            }
        });

        getMessage();
    }

    // 获取当前登录用户的头像
    private void getUser(Model model) {
        if (firebaseUser != null) {  // 确保 firebaseUser 不为空
            String currentUserId = firebaseUser.getUid();

            reference.child("Users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // 获取当前登录用户的头像 URL
                        String imageUrl = dataSnapshot.child("image").getValue(String.class);
                        model.setImage(imageUrl);  // 将图片 URL 设置到 Model 对象中
                        Log.d("UserImage", "User Image URL: " + imageUrl);
                    } else {
                        Log.d("UserImage", "User not found for ID: " + currentUserId);
                    }

                    // 添加消息到列表并更新 RecyclerView
                    list.add(model);
                    messageAdapter.notifyDataSetChanged();
                    recyclerViewMessageArea.scrollToPosition(list.size() - 1);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("Firebase", "Error loading user data", databaseError.toException());
                }
            });
        } else {
            Log.e("Firebase", "FirebaseUser is null");
        }
    }

    private void getMessage() {
        reference.child("Messages").child(userName).child(friendName).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Model model = snapshot.getValue(Model.class);
                if (model != null) {
                    // 调用 GetUser() 函数，获取当前登录用户的头像
                    getUser(model);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        messageAdapter = new MessageAdapter(list, userName);
        recyclerViewMessageArea.setAdapter(messageAdapter);
    }

    private void sendMessage(String message) {
        String key = reference.child("Messages").child(userName).child(friendName).push().getKey();
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("message", message);
        messageMap.put("from", userName);

        reference.child("Messages").child(userName).child(friendName).child(key).setValue(messageMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reference.child("Messages").child(friendName).child(userName).child(key).setValue(messageMap);
            }
        });
    }
}
