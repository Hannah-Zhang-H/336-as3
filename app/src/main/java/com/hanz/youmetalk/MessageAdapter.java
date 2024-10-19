package com.hanz.youmetalk;

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

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Model> list;
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;

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

    public class MessageViewHolder extends RecyclerView.ViewHolder {
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
