package com.hanz.youmetalk;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class MyTalkActivity extends AppCompatActivity implements MessageAdapter.OnMessageLongClickListener {

    private ActivityMyTalkBinding myTalkLayout;
    private RecyclerView recyclerViewMessageArea;
    private EditText editTextMessage;
    private FloatingActionButton fab;

    private String currentUserId, friendId, friendName, conversationId;

    private DatabaseReference reference;
    private FirebaseUser firebaseUser;

    private MessageAdapter messageAdapter;
    private List<Model> list;

    private MediaPlayer mediaPlayer; // MediaPlayer for sound effects

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myTalkLayout = ActivityMyTalkBinding.inflate(getLayoutInflater());
        setContentView(myTalkLayout.getRoot());

        // Initialize FirebaseUser
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = firebaseUser.getUid();

        // Initialize UI components
        recyclerViewMessageArea = myTalkLayout.recyclerViewTalkArea;
        recyclerViewMessageArea.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();

        editTextMessage = myTalkLayout.editTextMessage;
        fab = myTalkLayout.fab;

        // Get friend details from intent
        friendId = getIntent().getStringExtra("friendId");
        friendName = getIntent().getStringExtra("friendName");

        if (friendId == null || friendName == null) {
            Toast.makeText(this, "Friend data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        reference = FirebaseDatabase.getInstance().getReference();
        conversationId = getConversationId(currentUserId, friendId);  // Get conversation ID
        loadMessages();

        messageAdapter = new MessageAdapter(list, currentUserId, this); // Pass listener for long clicks
        recyclerViewMessageArea.setAdapter(messageAdapter);

        // Set up the message send button (floating action button)
        fab.setOnClickListener(view -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                editTextMessage.setText("");
                playNotificationSound(R.raw.send_message); // Play sound when message is sent
            }
        });

        // Set up the back button (in the toolbar or navigation)
        myTalkLayout.imageViewBack.setOnClickListener(v -> onBackPressed());  // Handle toolbar back button
    }

    // Override the default back button behavior to navigate to the MainActivity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);  // Prevent creating new MainActivity instances
        startActivity(intent);
        finish();  // Close current activity
    }

    // Ensure MediaPlayer resources are properly released
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    // Play a sound effect
    private void playNotificationSound(int sound) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, sound);
        }
        mediaPlayer.start();
    }

    // Create a unique conversation ID between two users
    private String getConversationId(String userId1, String userId2) {
        return userId1.compareTo(userId2) > 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

    // Load messages and listen for new messages using ChildEventListener
    private void loadMessages() {
        reference.child("Messages").child(conversationId).child("messages")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        Model model = snapshot.getValue(Model.class);
                        if (model != null) {
                            // Filter soft deleted messages
                            if (model.getDeleted_for() == null || !model.getDeleted_for().contains(currentUserId)) {
                                model.setMessageId(snapshot.getKey());  // Set the message ID
                                list.add(model);
                                messageAdapter.notifyDataSetChanged();  // Notify adapter of new data
                                recyclerViewMessageArea.scrollToPosition(list.size() - 1);  // Scroll to bottom

                                // If the message is from the friend and not read, mark it as read
                                if (model.getFrom().equals(friendId) && !model.isRead()) {
                                    markMessageAsRead(snapshot.getKey());
                                }
                            }
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error loading messages", error.toException());
                    }
                });
    }

    // Mark a message as read in the Firebase database
    private void markMessageAsRead(String messageId) {
        reference.child("Messages").child(conversationId).child("messages").child(messageId)
                .child("isRead").setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("MessageStatus", "Message marked as read");
                    } else {
                        Log.e("MessageStatus", "Failed to mark message as read");
                    }
                });
    }

    // Send a message and save it in Firebase
    private void sendMessage(String message) {
        String key = reference.child("Messages").child(conversationId).child("messages").push().getKey();
        if (key != null) {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("message", message);
            messageMap.put("from", currentUserId);
            messageMap.put("to", friendId);
            messageMap.put("isRead", false);
            messageMap.put("timestamp", ServerValue.TIMESTAMP);

            reference.child("Messages").child(conversationId).child("messages").child(key)
                    .setValue(messageMap).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("MessageStatus", "Message sent successfully");
                        } else {
                            Log.e("MessageStatus", "Failed to send message");
                        }
                    });
        }
    }

    // Handle long clicks on messages for deleting
    @Override
    public void onMessageLongClick(String messageId, View anchorView) {
        showCustomDialog(messageId);
    }

    // Show a dialog to confirm the message deletion
    private void showCustomDialog(String messageId) {
        String[] options = {"Delete"};
        int[] icons = {R.drawable.ic_delete};

        // Create an adapter for the dialog
        IconTextAdapter adapter = new IconTextAdapter(this, options, icons);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setAdapter(adapter, (dialog, which) -> {
            if (which == 0) {  // If the user selects "Delete"
                deleteMessage(messageId);
            }
        });
        builder.setTitle("Select an action");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Soft delete a message by adding the current user to the "deleted_for" field
    private void deleteMessage(String messageId) {
        reference.child("Messages").child(conversationId).child("messages").child(messageId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Model model = dataSnapshot.getValue(Model.class);
                        if (model != null) {
                            List<String> deletedFor = model.getDeleted_for() != null ? model.getDeleted_for() : new ArrayList<>();
                            if (!deletedFor.contains(currentUserId)) {
                                deletedFor.add(currentUserId);  // Add current user to soft delete list
                            }

                            reference.child("Messages").child(conversationId).child("messages").child(messageId)
                                    .child("deleted_for").setValue(deletedFor)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            removeMessageFromList(messageId);  // Update the list and UI
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

    // Remove the deleted message from the list and notify the adapter
    private void removeMessageFromList(String messageId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getMessageId().equals(messageId)) {
                list.remove(i);  // Remove the message from the list
                messageAdapter.notifyItemRemoved(i);  // Notify the adapter
                break;
            }
        }
    }
}
