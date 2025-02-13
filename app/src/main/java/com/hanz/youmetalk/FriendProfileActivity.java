package com.hanz.youmetalk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hanz.youmetalk.databinding.ActivityFriendProfileBinding;

import java.util.Objects;

/**
 * FriendProfileActivity provides a UI for displaying a friend's profile information and
 * managing friendship options, such as deleting the friend.

 * Key Functionalities:
 * 1. **Display Friend's Information**: Shows the friend's name, YouMeID, and profile picture,
 *    which are passed as extras in the Intent.
 * 2. **Delete Friendship**: Allows users to delete a friend by removing both users from each other's
 *    friend lists, deleting the chat history, and removing any outstanding friend requests between them.
 * 3. **Edge-to-Edge UI Configuration**: Configures the view to accommodate system bars for a
 *    seamless, edge-to-edge design.
 * 4. **Result Return for Friend Deletion**: After successful deletion, this activity returns the
 *    friend's ID to the previous activity to trigger UI updates.

 * This activity ensures proper cleanup of user relationships and message history in a structured,
 * user-friendly interface.
 */
public class FriendProfileActivity extends AppCompatActivity {

    private String friendId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        // Declare ViewBinding variable
        com.hanz.youmetalk.databinding.ActivityFriendProfileBinding binding = ActivityFriendProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // EdgeToEdge configuration
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the friend ID and name passed from ChatAdapter
        Intent intent = getIntent();
        String friendName = intent.getStringExtra("friendName");
        friendId = intent.getStringExtra("friendId");
        String friendYouMeID = intent.getStringExtra("friendYouMeId");


        // Display the friend's name and youMeId
        Objects.requireNonNull(binding.textInputLayoutFriendName.getEditText()).setText(friendName);
        Objects.requireNonNull(binding.textInputLayoutFriendYouMeID.getEditText()).setText(friendYouMeID);

        // Get the current user ID
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Set the delete button click event
        binding.buttonDeleteFriend.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Friend")
                .setMessage("Are you sure you want to delete this friend?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    deleteFriendship();  // Delete friend
                })
                .setNegativeButton("No", null)
                .show());

        // Load the friend's profile image (assuming the image URL is passed through the Intent)
        String friendImage = intent.getStringExtra("friendImage");
        if (friendImage != null && !friendImage.isEmpty()) {
            Glide.with(this).load(friendImage).into(binding.imageViewFriendCircleProfileFriend);
        }
    }

    private void deleteFriendship() {
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("Users");

        // Remove friend from current user's friends list
        friendsRef.child(currentUserId).child("Friends").child(friendId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Now remove current user from the friend's friends list
                        friendsRef.child(friendId).child("Friends").child(currentUserId).removeValue()
                                .addOnCompleteListener(innerTask -> {
                                    if (innerTask.isSuccessful()) {
                                        // Remove chat history between the two users
                                        deleteChatHistoryAndFriendRequest(currentUserId, friendId);  
                                    } else {
                                        Toast.makeText(FriendProfileActivity.this, "Failed to remove friendship from friend's list", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(FriendProfileActivity.this, "Failed to delete friend", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Function to delete chat history and friend request
    private void deleteChatHistoryAndFriendRequest(String currentUserId, String friendId) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("Messages");

        // Chat key is typically in the format currentUserId_friendId, delete chats in both directions
        String chatKey1 = currentUserId + "_" + friendId;
        String chatKey2 = friendId + "_" + currentUserId;

        // Remove messages between both users
        messagesRef.child(chatKey1).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messagesRef.child(chatKey2).removeValue().addOnCompleteListener(innerTask -> {
                    if (innerTask.isSuccessful()) {
                        // delete the record of friend request
                        deleteFriendRequest(currentUserId, friendId);
                    } else {
                        Toast.makeText(FriendProfileActivity.this, "Failed to remove chat history", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(FriendProfileActivity.this, "Failed to remove chat history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Function to delete friend request record
    private void deleteFriendRequest(String currentUserId, String friendId) {
        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference("FriendRequest");

        // Remove any friend request between the current user and the friend
        requestRef.child(friendId).orderByChild("from").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    requestSnapshot.getRef().removeValue();
                }

                // Once everything is deleted, return to MainActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("deletedFriendId", friendId);
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FriendProfileActivity.this, "Failed to remove friend request", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
