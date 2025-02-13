package com.hanz.youmetalk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.hanz.youmetalk.databinding.ActivityMyTalkBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MyTalkActivity manages chat interactions between the current user and a friend, supporting real-time messaging,
 * message deletion, and status checks.
 *
 * Key Features:
 * - Firebase Database Integration: Real-time message sync and user relationship management.
 * - MessageAdapter: Displays chat messages with long-click options like message deletion.
 * - Notifications: Plays sounds for sent and received messages and alerts for friend status changes.
 *
 * Main Functions:
 * - `sendMessage(String message)`: Sends messages if the friend relationship is active.
 * - `loadMessages()`: Loads and manages message read/unread status.
 * - `checkFriendStatus()`: Validates friend status before chat actions.
 * - `deleteMessage(String messageId)`: Soft deletes messages for the current user.
 */


public class MyTalkActivity extends AppCompatActivity implements MessageAdapter.OnMessageLongClickListener {

    private RecyclerView recyclerViewMessageArea;
    private EditText editTextMessage;

    private String currentUserId;
    private String friendId;
    private String conversationId;

    private DatabaseReference reference;

    private MessageAdapter messageAdapter;
    private List<Model> list;
    private boolean isConversationOpen = false; // Check is conservation is open


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.hanz.youmetalk.databinding.ActivityMyTalkBinding myTalkLayout = ActivityMyTalkBinding.inflate(getLayoutInflater());
        setContentView(myTalkLayout.getRoot());

        // Initialize FirebaseUser
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = firebaseUser.getUid();

        recyclerViewMessageArea = myTalkLayout.recyclerViewTalkArea;
        recyclerViewMessageArea.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();

        TextView textViewChatFriendName = myTalkLayout.textViewChatFriendName;
        editTextMessage = myTalkLayout.editTextMessage;
        FloatingActionButton fab = myTalkLayout.fab;

        friendId = getIntent().getStringExtra("friendId");
        String friendName = getIntent().getStringExtra("friendName");

        if (friendId == null || friendName == null) {
            Toast.makeText(this, "Friend data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        textViewChatFriendName.setText(friendName);

        messageAdapter = new MessageAdapter(list, currentUserId, this); // Pass the listener
        recyclerViewMessageArea.setAdapter(messageAdapter);


        fab.setOnClickListener(view -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                // Before sending the message, check if the friend is still valid
                checkFriendStatusBeforeSending(message);
            }
        });

        conversationId = getConversationId(currentUserId, friendId);

        // Check if the friend is still in the friend list before loading messages
        checkFriendStatus();

        loadMessages();

        // Set up back press handling
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Finish the activity
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    // Play send message sound effect
    private void playNotificationSound(int sound) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, sound);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            Log.e("Audio", "Notification sound played successfully");
        } else {
            Log.e("Audio", "Failed to play notification sound");
        }
    }

    private String getConversationId(String userId1, String userId2) {
        return userId1.compareTo(userId2) > 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

    private void loadMessages() {
        reference.child("Messages").child(conversationId).child("messages")
                .addChildEventListener(new ChildEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        Model model = snapshot.getValue(Model.class);
                        if (model != null) {
                            // Check soft deletion
                            if (model.getDeleted_for() == null || !model.getDeleted_for().contains(currentUserId)) {
                                model.setMessageId(snapshot.getKey()); // Set message ID
                                list.add(model);
                                messageAdapter.notifyDataSetChanged();
                                recyclerViewMessageArea.scrollToPosition(list.size() - 1);


                                // Only play sound if the message is from the friend, is unread, and the conversation is not open
                                if (model.getFrom().equals(friendId) && !model.isRead() && !isConversationOpen) {
                                    playNotificationSound(R.raw.new_message);
                                }
                            }
                        }
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
                        Log.e("Firebase", "Error loading messages", error.toException());
                    }
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        isConversationOpen = true;
        markMessagesAsRead();  // Mark all unread messages as read when the user views the chat

    }

    private void sendMessage(String message) {
        String key = reference.child("Messages").child(conversationId).child("messages").push().getKey();
        if (key != null) {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("message", message);
            messageMap.put("from", currentUserId);
            messageMap.put("to", friendId);
            messageMap.put("isRead", false);  // Mark message as unread
            messageMap.put("timestamp", ServerValue.TIMESTAMP);

            reference.child("Messages").child(conversationId).child("messages").child(key)
                    .setValue(messageMap).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("MessageStatus", "Message sent successfully");
                            editTextMessage.setText("");  // Clear the input field after sending
                            playNotificationSound(R.raw.send_message); // Play sound effect
                        } else {
                            Log.e("MessageStatus", "Failed to send message");
                        }
                    });
        }
    }

    // Check if the friend is still valid before sending a message
    private void checkFriendStatusBeforeSending(String message) {
        DatabaseReference friendRef = reference.child("Users").child(currentUserId).child("Friends").child(friendId);
        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // If the friend still exists, send the message
                    sendMessage(message);
                } else {
                    // If the friend no longer exists, show a warning and do not send the message
                    Toast.makeText(MyTalkActivity.this, "This friend has been deleted. You cannot send messages.", Toast.LENGTH_SHORT).show();
                    // Function to delete friend request record
                    deleteFriendRequest(currentUserId, friendId);
                    finish(); // Return to main activity
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error checking friend status before sending message", databaseError.toException());
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
                Toast.makeText(MyTalkActivity.this, "Failed to remove friend request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Check if the friend is still in the list when entering the chat
    private void checkFriendStatus() {
        DatabaseReference friendRef = reference.child("Users").child(currentUserId).child("Friends").child(friendId);
        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // If the friend no longer exists, show a warning and return to the main activity
                    Toast.makeText(MyTalkActivity.this, "This friend has been deleted.", Toast.LENGTH_SHORT).show();
                    finish(); // Return to main activity
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error checking friend status", databaseError.toException());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        markMessagesAsRead();
        isConversationOpen = false;
    }

    private void markMessagesAsRead() {
        reference.child("Messages").child(conversationId).child("messages")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Model model = messageSnapshot.getValue(Model.class);

                            if (model != null && !model.isRead() && model.getFrom().equals(friendId)) {
                                // Mark the message as read
                                reference.child("Messages").child(conversationId).child("messages")
                                        .child(Objects.requireNonNull(messageSnapshot.getKey())).child("isRead").setValue(true)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Log.d("MessageStatus", "Message marked as read");
                                            } else {
                                                Log.e("MessageStatus", "Failed to mark message as read");
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error marking messages as read", error.toException());
                    }
                });
    }


    @Override
    public void onMessageLongClick(String messageId, View anchorView) {
        showCustomDialog(messageId);
    }

    private void showCustomDialog(String messageId) {
        String[] options = {"Delete"};

        // Instantiate IconTextAdapter
        IconTextAdapter adapter = new IconTextAdapter(this, options, new int[]{R.drawable.ic_delete});

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setAdapter(adapter, (dialog, which) -> {
            if (which == 0) { // If user clicks delete
                deleteMessage(messageId);
            }
        });
        builder.setTitle("Select an action");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteMessage(String messageId) {
        reference.child("Messages").child(conversationId).child("messages").child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Model model = dataSnapshot.getValue(Model.class);

                if (model != null) {
                    List<String> deletedFor = model.getDeleted_for() != null ? model.getDeleted_for() : new ArrayList<>();

                    if (!deletedFor.contains(currentUserId)) {
                        deletedFor.add(currentUserId); // Add currentUser to soft delete list
                    }

                    // Update database
                    reference.child("Messages").child(conversationId).child("messages").child(messageId).child("deleted_for").setValue(deletedFor)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // Delete the message and update the view
                                    for (int i = 0; i < list.size(); i++) {
                                        if (list.get(i).getMessageId().equals(messageId)) {
                                            list.remove(i); // Remove the message
                                            messageAdapter.notifyItemRemoved(i); // Update RecyclerView
                                            break;
                                        }
                                    }
                                } else {
                                    Toast.makeText(MyTalkActivity.this, "Error deleting message", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error deleting message", databaseError.toException());
            }
        });
    }
}
