package com.hanz.youmetalk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.badge.BadgeDrawable;
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

public class MainActivity extends AppCompatActivity
        implements ContactAdapter.OnFriendRequestCardClickListener, FriendRequestAdapter.FriendRequestListener {

    FirebaseAuth auth;
    FirebaseUser user;
    DatabaseReference reference;
    FirebaseDatabase database;
    RecyclerView recyclerView;

    List<User> friendList;
    List<FriendRequest> friendRequestList;
    ContactAdapter contactAdapter;
    FriendRequestAdapter friendRequestAdapter;
    ChatAdapter chatAdapter;

    private static final String CURRENT_ADAPTER_KEY = "current_adapter_key";
    private String currentAdapter = "chatAdapter";  // default adapter
    public static final int REQUEST_CODE_DELETE_FRIEND = 1001;

    BottomNavigationView bottomNavigationView;
    BadgeDrawable badgeDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check notification permission for API level 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Inflate the layout
        ActivityMainBinding mainLayout = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainLayout.getRoot());

        // set Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //init Firebase
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        // init RecyclerView
        recyclerView = mainLayout.recycleView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // init lists and adapters
        friendList = new ArrayList<>();
        friendRequestList = new ArrayList<>();
        contactAdapter = new ContactAdapter(this, friendList, 0, this);
        friendRequestAdapter = new FriendRequestAdapter(this, friendRequestList, this);
        chatAdapter = new ChatAdapter(this, new ArrayList<>());

        // restore status
        if (savedInstanceState != null) {
            currentAdapter = savedInstanceState.getString(CURRENT_ADAPTER_KEY, "chatAdapter");
        }

        bottomNavigationView = mainLayout.bottomNavigation;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_chat) {
                currentAdapter = "chatAdapter";
                loadChatUsers();
                return true;
            } else if (item.getItemId() == R.id.action_contact) {
                currentAdapter = "contactAdapter";
                loadFriendRequestsAndContacts();
                return true;
            } else if (item.getItemId() == R.id.action_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // load current adapter
        if ("chatAdapter".equals(currentAdapter)) {
            loadChatUsers();
        } else if ("contactAdapter".equals(currentAdapter)) {
            loadFriendRequestsAndContacts();
        }

        // Check unread messages when activity starts
        reference.child("Messages").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                checkForUnreadMessages(snapshot, user.getUid());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Error loading messages", error.toException());
            }
        });
    }

    // Check unread messages and send notification
    private void checkForUnreadMessages(DataSnapshot snapshot, String userId) {
        int unreadMessageCount = 0;

        for (DataSnapshot messageSnapshot : snapshot.child("messages").getChildren()) {
            String toUid = messageSnapshot.child("to").getValue(String.class);
            Boolean isRead = messageSnapshot.child("isRead").getValue(Boolean.class);

            if (toUid != null && toUid.equals(userId) && isRead != null && !isRead) {
                unreadMessageCount++;
            }
        }

        if (unreadMessageCount > 0) {
            showUnreadMessageBadge(unreadMessageCount);  // Show red dot on chat tab
        } else {
            removeUnreadMessageBadge();  // Remove red dot if no unread messages
        }
    }

    // Show unread message red dot (badge) on chat tab
    private void showUnreadMessageBadge(int unreadCount) {
        badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.action_chat);
        badgeDrawable.setVisible(true);
        badgeDrawable.setNumber(unreadCount);
    }

    // Remove red dot (badge) from chat tab when there are no unread messages
    private void removeUnreadMessageBadge() {
        BadgeDrawable badge = bottomNavigationView.getBadge(R.id.action_chat);
        if (badge != null) {
            badge.setVisible(false);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_ADAPTER_KEY, currentAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentAdapter = savedInstanceState.getString(CURRENT_ADAPTER_KEY, "chatAdapter");

        if ("chatAdapter".equals(currentAdapter)) {
            loadChatUsers();
        } else if ("contactAdapter".equals(currentAdapter)) {
            loadFriendRequestsAndContacts();
        }
    }

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
                                    chatUser.setImage(userSnapshot.child("image").getValue(String.class));

                                    chatUserList.add(chatUser);
                                    chatAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) { }
                        });
                    }
                }

                chatAdapter = new ChatAdapter(MainActivity.this, chatUserList);
                recyclerView.setAdapter(chatAdapter);
                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void loadFriendRequestsAndContacts() {
        loadFriendRequestSummary();
        loadContactData();
    }

    private void loadContactData() {
        friendList.clear();

        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference friendsRef = reference.child("Users").child(currentUserId).child("Friends");

        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    String friendUid = friendSnapshot.getKey();

                    reference.child("Users").child(friendUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            User friend = userSnapshot.getValue(User.class);
                            if (friend != null) {
                                friend.setId(userSnapshot.getKey());
                                friend.setImage(userSnapshot.child("image").getValue(String.class));
                                friendList.add(friend);
                                contactAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

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
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    @Override
    public void onFriendRequestCardClick() {
        switchToFriendRequests();
    }

    private void switchToFriendRequests() {
        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference requestRef = reference.child("FriendRequest").child(currentUserId);

        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    @Override
    public void onFriendRequestAccepted() {
        loadFriendRequestsAndContacts();
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


    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_DELETE_FRIEND && resultCode == RESULT_OK && data != null) {
            String deletedFriendId = data.getStringExtra("deletedFriendId");
            if (deletedFriendId != null) {
                chatAdapter.removeChatsWithFriend(deletedFriendId);
                chatAdapter.notifyDataSetChanged();
            }
        } else {
            Log.d("MainActivity", "onActivityResult failed or data is null");
        }
    }

}
