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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.Nullable;
import com.hanz.youmetalk.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    RecyclerView recyclerView;
    FirebaseUser user;
    DatabaseReference reference;
    FirebaseDatabase database;

    String userName;
    List<User> friendList;
    ContactAdapter contactAdapter;

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
        contactAdapter = new ContactAdapter(this, friendList);

        // Set default adapter
        recyclerView.setAdapter(contactAdapter);
        loadContactData();

        // Set BottomNavigationView click listener
        BottomNavigationView bottomNavigationView = mainLayout.bottomNavigation;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_chat) {
                // Add chat adapter logic here if needed
                return true;
            } else if (itemId == R.id.action_contact) {
                recyclerView.setAdapter(contactAdapter);
                loadContactData(); // Load contact data
                return true;
            } else if (itemId == R.id.action_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    // Load the contact data from Firebase
    private void loadContactData() {
        // Clear the list before adding new data
        friendList.clear();

        reference.child("Users").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                User newUser = snapshot.getValue(User.class);

                if (newUser != null) {
                    // Use Firebase unique key as user ID
                    newUser.setId(snapshot.getKey());

                    // Get the image URL from the snapshot and set it to the User object
                    String imageUrl = snapshot.child("image").getValue(String.class);
                    newUser.setImage(imageUrl);

                    // Avoid adding the current user to the friend list
                    if (!newUser.getId().equals(user.getUid())) {
                        friendList.add(newUser);
                        contactAdapter.notifyDataSetChanged();  // Notify adapter to refresh RecyclerView
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle user data changes if necessary
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle user removal if necessary
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // Inflate the menu for profile and logout options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.chat_menu, menu); // Ensure you have chat_menu.xml in res/menu
        return true;
    }

    // Handle menu item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            // Navigate to Profile Activity
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_signout) {
            // Sign out and go to Login Activity
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
