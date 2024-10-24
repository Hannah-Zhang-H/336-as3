package com.hanz.youmetalk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddFriendActivity extends AppCompatActivity {
    private TextInputEditText editTextYouMeID;
    private Button buttonSearch, buttonAddFriend;
    private CircleImageView profileImageView;
    private TextView textViewUserName, textViewYouMeID;
    private TextView labelUserName, labelYouMeID; // Added labels
    private ImageView imageViewBack;
    private DatabaseReference reference;
    private FirebaseAuth auth;
    private String searchedUserId;
    private String userName, userImageUrl, youMeId;
    private String currentUserYouMeId;

    private static final String KEY_USER_NAME = "keyUserName";
    private static final String KEY_YOUME_ID = "keyYouMeId";
    private static final String KEY_IMAGE_URL = "keyImageUrl";
    private static final String KEY_SEARCHED_USER_ID = "keySearchedUserId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        // view binding
        editTextYouMeID = findViewById(R.id.editTextYouMeID);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonAddFriend = findViewById(R.id.buttonAddFriend);
        profileImageView = findViewById(R.id.imageViewProfile);
        textViewUserName = findViewById(R.id.textViewUserName);
        textViewYouMeID = findViewById(R.id.textViewYouMeID);
        labelUserName = findViewById(R.id.labelUserName);
        labelYouMeID = findViewById(R.id.labelYouMeID);
        imageViewBack = findViewById(R.id.imageViewBack);

        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference();

        loadCurrentUserYouMeId();

        hideUserInfo();

        // back to main activity
        imageViewBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddFriendActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // close current activity
        });

        buttonSearch.setOnClickListener(view -> searchUser());

        buttonAddFriend.setOnClickListener(view -> sendFriendRequest());

        // retrieve the state
        if (savedInstanceState != null) {
            userName = savedInstanceState.getString(KEY_USER_NAME);
            youMeId = savedInstanceState.getString(KEY_YOUME_ID);
            userImageUrl = savedInstanceState.getString(KEY_IMAGE_URL);
            searchedUserId = savedInstanceState.getString(KEY_SEARCHED_USER_ID);

            if (userName != null && youMeId != null && userImageUrl != null) {
                restoreUserInfo(userName, youMeId, userImageUrl);
            }
        }
    }

    // read the YouMeID of current user
    private void loadCurrentUserYouMeId() {
        String currentUserId = auth.getCurrentUser().getUid();
        reference.child("Users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserYouMeId = snapshot.child("youMeId").getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddFriendActivity.this, "Failed to load user info.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchUser() {
        final String searchYouMeId = editTextYouMeID.getText().toString().trim();
        if (searchYouMeId.isEmpty()) {
            Toast.makeText(this, "Please enter a YouMeID", Toast.LENGTH_SHORT).show();
            return;
        }

        // ensure a user cannot send friend to themselves
        if (searchYouMeId.equals(currentUserYouMeId)) {
            Toast.makeText(this, "You cannot add yourself as a friend.", Toast.LENGTH_SHORT).show();
            return;
        }

        reference.child("Users").orderByChild("youMeId").equalTo(searchYouMeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        User foundUser = userSnapshot.getValue(User.class);
                        searchedUserId = userSnapshot.getKey();

                        if (foundUser != null) {
                            userName = foundUser.getUserName();
                            youMeId = foundUser.getYouMeId();
                            userImageUrl = foundUser.getImage();

                            textViewUserName.setText(userName);
                            textViewYouMeID.setText(youMeId);

                            // load profile image
                            if (userImageUrl != null && !userImageUrl.equals("null")) {
                                Glide.with(AddFriendActivity.this).load(userImageUrl).into(profileImageView);
                            } else {
                                profileImageView.setImageResource(R.drawable.profile_placeholder);  // 设置默认头像
                            }

                            showUserInfo();
                        }
                    }
                } else {
                    Toast.makeText(AddFriendActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                    clearUserInfo();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddFriendActivity.this, "Search failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFriendRequest() {
        if (searchedUserId == null) {
            Toast.makeText(this, "No user selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference currentUserFriendsRef = reference.child("Users").child(currentUserId).child("Friends");

        // check if the searched user already in the friend list
        currentUserFriendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(searchedUserId)) {
                    // if the user is already in the friend list, show error message
                    Toast.makeText(AddFriendActivity.this, "The user is your friend already.", Toast.LENGTH_SHORT).show();
                } else {
                    // if not existing friend, check if the request is already sent
                    DatabaseReference requestRef = reference.child("FriendRequest").child(searchedUserId);

                    requestRef.orderByChild("from").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // check the request status
                                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                                    String status = requestSnapshot.child("status").getValue(String.class);

                                    if ("waiting".equals(status)) {
                                        Toast.makeText(AddFriendActivity.this, "Friend request already sent and pending.", Toast.LENGTH_SHORT).show();
                                        return;
                                    } else if ("accepted".equals(status)) {
                                        Toast.makeText(AddFriendActivity.this, "This user has already accepted your request.", Toast.LENGTH_SHORT).show();
                                        return;
                                    } else if ("declined".equals(status)) {
                                        // resend request if the previous request is declined
                                        Map<String, Object> requestMap = new HashMap<>();
                                        requestMap.put("from", currentUserId);
                                        requestMap.put("status", "waiting");

                                        requestSnapshot.getRef().setValue(requestMap).addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(AddFriendActivity.this, "Friend request re-sent!", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(AddFriendActivity.this, "Failed to send request. Try again.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        return;
                                    }
                                }
                            } else {
                                // send new friend request
                                Map<String, Object> requestMap = new HashMap<>();
                                requestMap.put("from", currentUserId);
                                requestMap.put("status", "waiting");

                                requestRef.push().setValue(requestMap).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(AddFriendActivity.this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(AddFriendActivity.this, "Failed to send request. Try again.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(AddFriendActivity.this, "Failed to check friend request status.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddFriendActivity.this, "Failed to check friend list.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void clearUserInfo() {
        hideUserInfo();
        textViewUserName.setText("");
        textViewYouMeID.setText("");
        profileImageView.setImageResource(R.drawable.profile_placeholder);  // clear profile image
    }

    // hide the user data and disable the button
    private void hideUserInfo() {
        profileImageView.setVisibility(View.INVISIBLE);
        textViewUserName.setVisibility(View.INVISIBLE);
        textViewYouMeID.setVisibility(View.INVISIBLE);
        labelUserName.setVisibility(View.INVISIBLE);
        labelYouMeID.setVisibility(View.INVISIBLE);
        buttonAddFriend.setVisibility(View.INVISIBLE);
        buttonAddFriend.setEnabled(false);
    }

    // display user data and enable the button
    private void showUserInfo() {
        profileImageView.setVisibility(View.VISIBLE);
        textViewUserName.setVisibility(View.VISIBLE);
        textViewYouMeID.setVisibility(View.VISIBLE);
        labelUserName.setVisibility(View.VISIBLE);
        labelYouMeID.setVisibility(View.VISIBLE);
        buttonAddFriend.setVisibility(View.VISIBLE);
        buttonAddFriend.setEnabled(true);
    }

    private void restoreUserInfo(String userName, String youMeId, String userImageUrl) {
        textViewUserName.setText(userName);
        textViewYouMeID.setText(youMeId);

        // load profile image
        if (userImageUrl != null && !userImageUrl.equals("null")) {
            Glide.with(AddFriendActivity.this).load(userImageUrl).into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.profile_placeholder);  // set placeholder
        }

        showUserInfo();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_USER_NAME, userName);
        outState.putString(KEY_YOUME_ID, youMeId);
        outState.putString(KEY_IMAGE_URL, userImageUrl);
        outState.putString(KEY_SEARCHED_USER_ID, searchedUserId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        userName = savedInstanceState.getString(KEY_USER_NAME);
        youMeId = savedInstanceState.getString(KEY_YOUME_ID);
        userImageUrl = savedInstanceState.getString(KEY_IMAGE_URL);
        searchedUserId = savedInstanceState.getString(KEY_SEARCHED_USER_ID);

        if (userName != null && youMeId != null && userImageUrl != null) {
            restoreUserInfo(userName, youMeId, userImageUrl);
        }
    }
}
