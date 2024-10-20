package com.hanz.youmetalk;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
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

        // Set BottomNavigationView click listener
        BottomNavigationView bottomNavigationView = mainLayout.bottomNavigation;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_chat) {
                return true;
            } else if (itemId == R.id.action_contact) {
                loadFriendRequestsAndContacts();  // Load both contacts and friend requests when "Contact" is clicked
                return true;
            } else if (itemId == R.id.action_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    // Load both contacts and friend requests
    private void loadFriendRequestsAndContacts() {
        loadFriendRequestSummary();  // Load friend request summary
        loadContactData();  // Load contact data
    }

    // Load the contact data (friends) from Firebase
    private void loadContactData() {
        friendList.clear();  // Clear the existing friend list

        String currentUserId = auth.getCurrentUser().getUid();
        // Reference to the current user's Friends node
        DatabaseReference friendsRef = reference.child("Users").child(currentUserId).child("Friends");

        // Get the list of friend UIDs from the Friends node
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    // Get each friend's UID
                    String friendUid = friendSnapshot.getKey();

                    // Fetch the friend's details from the Users node using the UID
                    DatabaseReference userRef = reference.child("Users").child(friendUid);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            User friend = userSnapshot.getValue(User.class);
                            if (friend != null) {
                                // Set the friend's UID
                                friend.setId(userSnapshot.getKey());

                                // Set the friend's image URL if available
                                String imageUrl = userSnapshot.child("image").getValue(String.class);
                                friend.setImage(imageUrl);

                                // Add the friend to the list and update the adapter
                                friendList.add(friend);
                                contactAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle potential errors here
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle potential errors here
            }
        });
    }

    // Load friend request summary and filter only "waiting" requests
    private void loadFriendRequestSummary() {
        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference requestRef = reference.child("FriendRequest").child(currentUserId);

        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendRequestList.clear();  // Clear the existing list
                int waitingRequestCount = 0;  // count the waiting status friend request

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

    // Switch to the FriendRequestAdapter
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
                    if (friendRequest != null) {
                        friendRequest.setRequestId(requestSnapshot.getKey());
                        friendRequestList.add(friendRequest);
                    }
                }
                friendRequestAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(friendRequestAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
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
