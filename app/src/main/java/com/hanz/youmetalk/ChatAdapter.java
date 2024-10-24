package com.hanz.youmetalk;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context context;
    private List<User> chatUserList;

    public ChatAdapter(Context context, List<User> chatUserList) {
        this.context = context;
        this.chatUserList = chatUserList;
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

        // Load profile image
        if (chatUser.getImage() != null && !chatUser.getImage().isEmpty()) {
            Glide.with(context)
                    .load(chatUser.getImage())
                    .apply(new RequestOptions().placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_error))
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        // Retrieve the last message
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("Messages");

        messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastMessageContent = "";
                long lastMessageTimestamp = 0;

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot messageDetailSnapshot : messageSnapshot.child("messages").getChildren()) {
                        String fromUid = messageDetailSnapshot.child("from").getValue(String.class);
                        String toUid = messageDetailSnapshot.child("to").getValue(String.class);
                        String messageContent = messageDetailSnapshot.child("message").getValue(String.class);
                        Long timestamp = messageDetailSnapshot.child("timestamp").getValue(Long.class);

                        if (fromUid == null || toUid == null || messageContent == null || timestamp == null) {
                            continue;
                        }

                        // Check if the message involves the current user and the chat user
                        if ((fromUid.equals(currentUserId) && toUid.equals(chatUser.getId())) ||
                                (fromUid.equals(chatUser.getId()) && toUid.equals(currentUserId))) {
                            lastMessageContent = messageContent;
                            lastMessageTimestamp = timestamp;
                        }
                    }
                }

                // Display the last message content
                if (!lastMessageContent.isEmpty()) {
                    holder.lastMessage.setText(lastMessageContent);
                } else {
                    holder.lastMessage.setText("No messages yet");
                }

                // Display the timestamp if available
                if (lastMessageTimestamp != 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String dateString = sdf.format(new Date(lastMessageTimestamp));
                    holder.messageDate.setText(dateString);
                }

                if (chatUser.getLastSeenMessageTimestamp() == 0) {
                    // 用户还没有查看过这个对话
                    // 使用默认颜色 (R.color.purple_500)
                    holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.default_background));
                } else {
                    // 用户之前查看过这个对话
                    // 比较消息时间和用户上次查看消息的时间
                    if (lastMessageTimestamp > chatUser.getLastSeenMessageTimestamp()) {
                        // 有新消息
                        holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.new_message_background));
                    } else {
                        // 没有新消息，使用默认背景颜色
                        holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.default_background));
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatAdapter", "Error retrieving messages: " + error.getMessage());
            }
        });

        // Switch to MyTalkActivity when the card is clicked
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, MyTalkActivity.class);
            intent.putExtra("friendId", chatUser.getId());
            intent.putExtra("friendName", chatUser.getUserName());
            context.startActivity(intent);

            // 更新本地的 lastSeenMessageTimestamp
            long currentTimestamp = System.currentTimeMillis();
            chatUser.setLastSeenMessageTimestamp(currentTimestamp);

            // 将时间戳保存到 Firebase
            saveLastSeenTimestampToFirebase(chatUser.getId(), currentTimestamp);
        });
    }

    // 保存时间戳到 Firebase
    private void saveLastSeenTimestampToFirebase(String userId, long timestamp) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.child("lastSeenMessageTimestamp").setValue(timestamp)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firebase", "Last seen message timestamp updated successfully.");
                    } else {
                        Log.e("Firebase", "Failed to update last seen message timestamp.");
                    }
                });
    }


    @Override
    public int getItemCount() {
        return chatUserList.size();
    }

    // ViewHolder class for chat items
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView userName, lastMessage, messageDate;
        ImageView profileImage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.textViewUser);
            lastMessage = itemView.findViewById(R.id.textViewLastMessage);
            messageDate = itemView.findViewById(R.id.textViewMessageDate);
            profileImage = itemView.findViewById(R.id.imageViewUser);
        }
    }
}
