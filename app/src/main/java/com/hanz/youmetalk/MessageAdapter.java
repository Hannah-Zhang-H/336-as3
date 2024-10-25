package com.hanz.youmetalk;

import static com.hanz.youmetalk.MainActivity.REQUEST_CODE_DELETE_FRIEND;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * MessageAdapter manages the display of chat messages in a RecyclerView, distinguishing sent and received messages.
 *
 * Key Features:
 * - Differentiates sent/received messages by loading appropriate layouts.
 * - Loads sender profile images and user info from Firebase.
 * - Supports profile image clicks for viewing profiles or deleting friends.
 * - Long-click listener enables additional message management options.
 *
 * Main Methods:
 * - `onBindViewHolder()`: Binds data to message views, including profile image and long-click actions.
 * - `getItemViewType()`: Determines view type based on sent/received status.
 */


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private final List<Model> list;
    private final String currentUserId;
    private final OnMessageLongClickListener longClickListener;

    private static final int SEND_TYPE = 1;
    private static final int RECEIVE_TYPE = 2;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(String messageId, View anchorView);
    }

    public MessageAdapter(List<Model> list, String currentUserId, OnMessageLongClickListener longClickListener) {
        this.list = list;
        this.currentUserId = currentUserId;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == SEND_TYPE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_send_card, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_receive_card, parent, false);
        }
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Model model = list.get(position);
        holder.textView.setText(model.getMessage());

        String senderId = model.getFrom();

        // get the sender profile image
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(senderId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String imageUrl = dataSnapshot.child("image").getValue(String.class);
                    String friendName = dataSnapshot.child("userName").getValue(String.class);
                    String friendYouMeId = dataSnapshot.child("youMeId").getValue(String.class);

                    // load profile image
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(holder.itemView.getContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.profile_placeholder)
                                .error(R.drawable.profile_error)
                                .into(holder.profileImage);
                    } else {
                        holder.profileImage.setImageResource(R.drawable.profile_placeholder);
                    }

                    // Click the friend image, go to friend profile activity, users can choose to delete the friend
                    // Set click listener for profile image (only for received messages)
                    if (!senderId.equals(currentUserId)) {
                        holder.profileImage.setOnClickListener(v -> {
                            // Intent to navigate to FriendProfileActivity
                            Intent intent = new Intent(holder.itemView.getContext(), FriendProfileActivity.class);
                            intent.putExtra("friendId", senderId);  // Pass the friend's ID
                            intent.putExtra("friendName", friendName);  // Pass the friend's name
                            intent.putExtra("friendImage", imageUrl);  // Pass the friend's profile image URL
                            intent.putExtra("friendYouMeId", friendYouMeId);  // Pass the friend's profile image URL
                            ((Activity) holder.itemView.getContext()).startActivityForResult(intent, REQUEST_CODE_DELETE_FRIEND);  // Use the request code for result
                        });

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors here
            }
        });

        // long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(model.getMessageId(), holder.itemView); // Pass the view being long-pressed
            }
            return true;
        });


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView profileImage;

        public MessageViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == SEND_TYPE) {
                textView = itemView.findViewById(R.id.textViewSend);
                profileImage = itemView.findViewById(R.id.imageViewSend);
            } else {
                textView = itemView.findViewById(R.id.textViewReceived);
                profileImage = itemView.findViewById(R.id.imageViewReceived);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Model model = list.get(position);
        if (model.getFrom().equals(currentUserId)) {
            return SEND_TYPE;
        } else {
            return RECEIVE_TYPE;
        }
    }
}
