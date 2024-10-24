package com.hanz.youmetalk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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


    public ChatAdapter(Context context, List<User> chatUserList) {
        this.context = context;
        this.chatUserList = chatUserList;
        unreadMessageCounts = new HashMap<>();
        hasUnreadMessages = new HashMap<>();
        lastMessages = new HashMap<>();
        lastMessageTimestamps = new HashMap<>();

        // add firebase listener
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

        // update the last message and unread count
        updateMessageStatus(chatUser.getId(), holder);

        // click card start talk activity
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

    // add firebase listener
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

    // update the unread message, last message and record the timestamp
    private void updateUnreadMessages(DataSnapshot snapshot, String currentUserId) {
        String chatUserId = snapshot.getKey().contains(currentUserId)
                ? snapshot.getKey().replace(currentUserId, "").replace("_", "")
                : null;

        if (chatUserId != null) {
            // update the message status of specific user
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
                            lastMessageContent = messageContent;
                            lastMessageTimestamp = timestamp;

                            if (fromUid.equals(chatUserId) && !isRead) {
                                hasUnreadMessage = true;
                                unreadMessageCount++;
                            }
                        }
                    }
                }

                // save the last message content and time stamp
                lastMessages.put(chatUserId, lastMessageContent);
                lastMessageTimestamps.put(chatUserId, lastMessageTimestamp);

                // update the unread count and background color
                unreadMessageCounts.put(chatUserId, unreadMessageCount);
                hasUnreadMessages.put(chatUserId, hasUnreadMessage);

                // find the user in the list and refresh
                int position = findUserPosition(chatUserId);
                if (position != -1) {
                    notifyItemChanged(position);  // only refresh the item changed
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatAdapter", "Error retrieving messages: " + error.getMessage());
            }
        });
    }

    // check the postion of the user
    private int findUserPosition(String chatUserId) {
        for (int i = 0; i < chatUserList.size(); i++) {
            if (chatUserList.get(i).getId().equals(chatUserId)) {
                return i;
            }
        }
        return -1;  // user not in the list
    }


    // update the last message, unread count, background
    private void updateMessageStatus(String chatUserId, ChatViewHolder holder) {
        int unreadMessageCount = unreadMessageCounts.getOrDefault(chatUserId, 0);
        boolean hasUnreadMessage = hasUnreadMessages.getOrDefault(chatUserId, false);

        // update unread message
        if (unreadMessageCount > 0) {
            holder.unreadCountTextView.setText(String.valueOf(unreadMessageCount));
            holder.unreadCountTextView.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCountTextView.setVisibility(View.GONE);
        }

        // update last message
        String lastMessageContent = lastMessages.getOrDefault(chatUserId, "No messages yet");
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

        // update background color
        if (hasUnreadMessage) {
            Log.d("ChatAdapter", "Setting unread background for user: " + chatUserId);
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.new_message_background));
        } else {
            Log.d("ChatAdapter", "Setting default background for user: " + chatUserId);
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.default_background));
        }
    }

    // ViewHolder class for chat items
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

    // Method to remove chats related to a specific friend
    @SuppressLint("NotifyDataSetChanged")
    public void removeChatsWithFriend(String friendId) {
        List<User> updatedUserList = new ArrayList<>();
        for (User user : chatUserList) {
            // Keep only the users that are not the deleted friend
            if (!user.getId().equals(friendId)) {
                updatedUserList.add(user);
            }
        }

        Log.d("ChatAdapter", "Removing friend from chat list: " + friendId);  // 打印日志以检查删除的好友
        chatUserList.clear();
        chatUserList.addAll(updatedUserList);

        // Also remove this friend's data from the message status maps
        unreadMessageCounts.remove(friendId);
        lastMessages.remove(friendId);
        lastMessageTimestamps.remove(friendId);
        hasUnreadMessages.remove(friendId);


        Log.d("ChatAdapter", "Chat list size after removal: " + chatUserList.size());  // 打印新的列表长度
        notifyDataSetChanged();  // Notify the adapter to refresh the view
    }


}
