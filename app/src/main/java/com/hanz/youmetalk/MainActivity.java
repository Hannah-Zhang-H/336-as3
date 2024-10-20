package com.hanz.youmetalk;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hanz.youmetalk.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ContactAdapter.OnFriendRequestCardClickListener {

    FirebaseAuth auth;
    RecyclerView recyclerView;
    FirebaseUser user;
    DatabaseReference reference;
    FirebaseDatabase database;

    List<User> friendList;
    List<FriendRequest> friendRequestList;
    ContactAdapter contactAdapter;
    FriendRequestAdapter friendRequestAdapter;
    ChatAdapter chatAdapter;

    private static final String CURRENT_ADAPTER_KEY = "current_adapter_key";
    private String currentAdapter = "chatAdapter"; // set ChatAdapter as default adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout
        ActivityMainBinding mainLayout = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainLayout.getRoot());

        // Set up the toolbar
        setSupportActionBar(mainLayout.toolbar);

        // Initialize RecyclerView
        recyclerView = mainLayout.recycleView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Firebase initialization
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        // Initialize lists and adapters
        friendList = new ArrayList<>();
        friendRequestList = new ArrayList<>();
        contactAdapter = new ContactAdapter(this, friendList, 0, this);
        friendRequestAdapter = new FriendRequestAdapter(this, friendRequestList);
        chatAdapter = new ChatAdapter(this, new ArrayList<>());

        // restore state
        if (savedInstanceState != null) {
            currentAdapter = savedInstanceState.getString(CURRENT_ADAPTER_KEY, "chatAdapter");
        }

        BottomNavigationView bottomNavigationView = mainLayout.bottomNavigation;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_chat) {
                currentAdapter = "chatAdapter";
                loadChatUsers();
                return true;
            } else if (itemId == R.id.action_contact) {
                currentAdapter = "contactAdapter";
                loadFriendRequestsAndContacts();
                return true;
            } else if (itemId == R.id.action_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });

        // load the current adapter
        if ("chatAdapter".equals(currentAdapter)) {
            loadChatUsers();
        } else if ("contactAdapter".equals(currentAdapter)) {
            loadFriendRequestsAndContacts();
        } else if ("friendRequestAdapter".equals(currentAdapter)) {
            switchToFriendRequests();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current adapter
        outState.putString(CURRENT_ADAPTER_KEY, currentAdapter);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // restore current adapter
        currentAdapter = savedInstanceState.getString(CURRENT_ADAPTER_KEY, "chatAdapter");

        // restore the adapter based on saved state
        if ("chatAdapter".equals(currentAdapter)) {
            loadChatUsers();
        } else if ("contactAdapter".equals(currentAdapter)) {
            // update the data
            contactAdapter.notifyDataSetChanged();
        } else if ("friendRequestAdapter".equals(currentAdapter)) {
            switchToFriendRequests();
        }
    }

    // load the users current user chatting with
    private void loadChatUsers() {
        List<User> chatUserList = new ArrayList<>();
        String currentUserId = auth.getCurrentUser().getUid();

        reference.child("Messages").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    String messageKey = messageSnapshot.getKey();

                    if (messageKey != null && messageKey.contains(currentUserId)) {
                        String[] uids = messageKey.split("_");
                        String chattingPartnerUid = uids[0].equals(currentUserId) ? uids[1] : uids[0];

                        reference.child("Users").child(chattingPartnerUid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                User chatUser = userSnapshot.getValue(User.class);
                                if (chatUser != null) {
                                    chatUser.setId(userSnapshot.getKey());
                                    String imageUrl = userSnapshot.child("image").getValue(String.class);
                                    chatUser.setImage(imageUrl);

                                    chatUserList.add(chatUser);
                                    chatAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle error
                            }
                        });
                    }
                }

                // switch to ChatAdapter
                chatAdapter = new ChatAdapter(MainActivity.this, chatUserList);
                recyclerView.setAdapter(chatAdapter);
                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void loadFriendRequestsAndContacts() {
        loadFriendRequestSummary();
        loadContactData();
    }

    private void loadContactData() {
        // 清空列表，避免重复加载
        friendList.clear();

        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference friendsRef = reference.child("Users").child(currentUserId).child("Friends");

        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    String friendUid = friendSnapshot.getKey();

                    reference.child("Users").child(friendUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            User friend = userSnapshot.getValue(User.class);
                            if (friend != null) {
                                friend.setId(userSnapshot.getKey());
                                String imageUrl = userSnapshot.child("image").getValue(String.class);
                                friend.setImage(imageUrl);

                                // 添加朋友到列表
                                friendList.add(friend);

                                // 确保只更新数据而不是重新创建适配器
                                contactAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle error
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });

        // 这里不需要重新创建适配器，只需要刷新现有的适配器数据
        recyclerView.setAdapter(contactAdapter);
    }

    private void loadFriendRequestSummary() {
        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference requestRef = reference.child("FriendRequest").child(currentUserId);

        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendRequestList.clear();
                int waitingRequestCount = 0;

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    FriendRequest friendRequest = requestSnapshot.getValue(FriendRequest.class);
                    if (friendRequest != null && "waiting".equals(friendRequest.getStatus())) {
                        friendRequest.setRequestId(requestSnapshot.getKey());
                        friendRequestList.add(friendRequest);
                        waitingRequestCount++;
                    }
                }

                contactAdapter = new ContactAdapter(MainActivity.this, friendList, waitingRequestCount, MainActivity.this);
                recyclerView.setAdapter(contactAdapter);
                contactAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    @Override
    public void onFriendRequestCardClick() {
        switchToFriendRequests();
    }

    private void switchToFriendRequests() {
        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference requestRef = reference.child("FriendRequest").child(currentUserId);

        requestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendRequestList.clear();
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    FriendRequest friendRequest = requestSnapshot.getValue(FriendRequest.class);
                    if (friendRequest != null && "waiting".equals(friendRequest.getStatus())) {
                        friendRequest.setRequestId(requestSnapshot.getKey());
                        friendRequestList.add(friendRequest);
                    }
                }
                friendRequestAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(friendRequestAdapter);
                currentAdapter = "friendRequestAdapter";  // update current adapter
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_signout) {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_addFriend) {
            startActivity(new Intent(this, AddFriendActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}
