package com.hanz.youmetalk;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context context;
    private List<User> chatUserList;
    private HashMap<String, Integer> unreadMessageCounts;
    private HashMap<String, Boolean> hasUnreadMessages;
    private HashMap<String, String> lastMessages;
    private HashMap<String, Long> lastMessageTimestamps;

    // the variables for notification
    private static final String CHANNEL_ID = "message_channel";
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String LAST_NOTIFIED_TIMESTAMP_KEY = "lastNotifiedTimestamp";
    private long lastNotifiedTimestamp;

    public ChatAdapter(Context context, List<User> chatUserList) {
        this.context = context;
        this.chatUserList = chatUserList;
        unreadMessageCounts = new HashMap<>();
        hasUnreadMessages = new HashMap<>();
        lastMessages = new HashMap<>();
        lastMessageTimestamps = new HashMap<>();

        // load lastNotifiedTimestamp from SharedPreferences
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        lastNotifiedTimestamp = settings.getLong(LAST_NOTIFIED_TIMESTAMP_KEY, 0);

        listenForMessageUpdates();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_card, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        User chatUser = chatUserList.get(position);
        holder.userName.setText(chatUser.getUserName());

        if (chatUser.getImage() != null && !chatUser.getImage().isEmpty()) {
            Glide.with(context)
                    .load(chatUser.getImage())
                    .apply(new RequestOptions().placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_error))
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        updateMessageStatus(chatUser.getId(), holder);

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, MyTalkActivity.class);
            intent.putExtra("friendId", chatUser.getId());
            intent.putExtra("friendName", chatUser.getUserName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatUserList.size();
    }

    // define Firebase listener
    private void listenForMessageUpdates() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("Messages");

        messageRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                updateUnreadMessages(snapshot, currentUserId);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                updateUnreadMessages(snapshot, currentUserId);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) { }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatAdapter", "Error listening to messages: " + error.getMessage());
            }
        });
    }

    // Update Unread Message, Last Message, timestamp
    private void updateUnreadMessages(DataSnapshot snapshot, String currentUserId) {
        String chatUserId = snapshot.getKey().contains(currentUserId)
                ? snapshot.getKey().replace(currentUserId, "").replace("_", "")
                : null;

        if (chatUserId != null) {
            // update message status for chat user
            loadLastMessageAndUnreadCount(chatUserId);
        }
    }

    private void loadLastMessageAndUnreadCount(String chatUserId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("Messages");

        messageRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadMessageCount = 0;
                boolean hasUnreadMessage = false;
                String lastMessageContent = "";
                long lastMessageTimestamp = 0;
                long latestMessageTimestamp = lastNotifiedTimestamp;

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot messageDetailSnapshot : messageSnapshot.child("messages").getChildren()) {
                        String fromUid = messageDetailSnapshot.child("from").getValue(String.class);
                        String toUid = messageDetailSnapshot.child("to").getValue(String.class);
                        String messageContent = messageDetailSnapshot.child("message").getValue(String.class);
                        Long timestamp = messageDetailSnapshot.child("timestamp").getValue(Long.class);
                        Boolean isRead = messageDetailSnapshot.child("isRead").getValue(Boolean.class);

                        if (fromUid == null || toUid == null || messageContent == null || timestamp == null || isRead == null) {
                            continue;
                        }

                        if ((fromUid.equals(currentUserId) && toUid.equals(chatUserId)) ||
                                (fromUid.equals(chatUserId) && toUid.equals(currentUserId))) {
                            // Update the last message content and timestamp
                            if (timestamp > lastMessageTimestamp) {
                                lastMessageContent = messageContent;
                                lastMessageTimestamp = timestamp;
                            }

                            if (fromUid.equals(chatUserId) && !isRead) {
                                hasUnreadMessage = true;
                                unreadMessageCount++;
                            }

                            // check if there are any unnoticed unread message
                            if (timestamp > latestMessageTimestamp) {
                                latestMessageTimestamp = timestamp;
                            }
                        }
                    }
                }

                // save the last message content and timestamp
                lastMessages.put(chatUserId, lastMessageContent);
                lastMessageTimestamps.put(chatUserId, lastMessageTimestamp);

                // update unread count and background color
                unreadMessageCounts.put(chatUserId, unreadMessageCount);
                hasUnreadMessages.put(chatUserId, hasUnreadMessage);

                // validate sending notification
                if (latestMessageTimestamp > lastNotifiedTimestamp) {
                    if (unreadMessageCount > 0) {
                        sendNotification(unreadMessageCount);
                        lastNotifiedTimestamp = latestMessageTimestamp;

                        // save lastNotifiedTimestamp to SharedPreferences
                        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putLong(LAST_NOTIFIED_TIMESTAMP_KEY, lastNotifiedTimestamp);
                        editor.apply();
                    }
                }

                // update recyclerview
                int position = findUserPosition(chatUserId);
                if (position != -1) {
                    notifyItemChanged(position);  // only update the change item
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatAdapter", "Error retrieving messages: " + error.getMessage());
            }
        });
    }

    // check the user position
    private int findUserPosition(String chatUserId) {
        for (int i = 0; i < chatUserList.size(); i++) {
            if (chatUserList.get(i).getId().equals(chatUserId)) {
                return i;
            }
        }
        return -1;  // user not in the list
    }

    // update the last message,count, background color
    private void updateMessageStatus(String chatUserId, ChatViewHolder holder) {
        int unreadMessageCount = unreadMessageCounts.getOrDefault(chatUserId, 0);
        boolean hasUnreadMessage = hasUnreadMessages.getOrDefault(chatUserId, false);

        // update the messages count
        if (unreadMessageCount > 0) {
            holder.unreadCountTextView.setText(String.valueOf(unreadMessageCount));
            holder.unreadCountTextView.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCountTextView.setVisibility(View.GONE);
        }

        // update the last message
        String lastMessageContent = lastMessages.getOrDefault(chatUserId, "No messages yet");
        int maxLength = 15;  // Set the max length for the last message

        assert lastMessageContent != null;
        if (lastMessageContent.length() > maxLength) {
            lastMessageContent = lastMessageContent.substring(0, maxLength) + "...";
        }
        holder.lastMessage.setText(lastMessageContent);

        // update timestamp
        long lastMessageTimestamp = lastMessageTimestamps.getOrDefault(chatUserId, 0L);
        if (lastMessageTimestamp != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateString = sdf.format(new Date(lastMessageTimestamp));
            holder.messageDate.setText(dateString);
        } else {
            holder.messageDate.setText("");
        }

        // update the background color
        if (hasUnreadMessage) {
            Log.d("ChatAdapter", "Setting unread background for user: " + chatUserId);
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.new_message_background));
        } else {
            Log.d("ChatAdapter", "Setting default background for user: " + chatUserId);
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.default_background));
        }
    }

    // send notification
    private void sendNotification(int unreadMessageCount) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Message Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            Log.d("ChatAdapter", "Notification Channel Created");
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("You have " + unreadMessageCount + " unread messages")
                .setContentText("Check your messages.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        Log.d("ChatAdapter", "Sending Notification for " + unreadMessageCount + " unread messages");
        notificationManager.notify(1, notificationBuilder.build());
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView userName, lastMessage, messageDate, unreadCountTextView;
        ImageView profileImage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.textViewUser);
            lastMessage = itemView.findViewById(R.id.textViewLastMessage);
            messageDate = itemView.findViewById(R.id.textViewMessageDate);
            profileImage = itemView.findViewById(R.id.imageViewUser);
            unreadCountTextView = itemView.findViewById(R.id.textViewUnreadCount);
        }
    }

    // remove the chats
    @SuppressLint("NotifyDataSetChanged")
    public void removeChatsWithFriend(String friendId) {
        List<User> updatedUserList = new ArrayList<>();
        for (User user : chatUserList) {
            if (!user.getId().equals(friendId)) {
                updatedUserList.add(user);
            }
        }

        Log.d("ChatAdapter", "Removing friend from chat list: " + friendId);
        chatUserList.clear();
        chatUserList.addAll(updatedUserList);

        unreadMessageCounts.remove(friendId);
        lastMessages.remove(friendId);
        lastMessageTimestamps.remove(friendId);
        hasUnreadMessages.remove(friendId);

        Log.d("ChatAdapter", "Chat list size after removal: " + chatUserList.size());
        notifyDataSetChanged();

        Log.d("ChatAdapter", "Chat list size after removal: " + chatUserList.size());
        notifyDataSetChanged();  // Notify the adapter to refresh the view
    }


}
