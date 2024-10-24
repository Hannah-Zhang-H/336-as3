package com.hanz.youmetalk;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SUMMARY = 0;
    private static final int VIEW_TYPE_CONTACT = 1;

    private Context context;
    private List<User> friendList;
    private int waitingFriendRequestCount;
    private OnFriendRequestCardClickListener onFriendRequestCardClickListener;

    public interface OnFriendRequestCardClickListener {
        void onFriendRequestCardClick();
    }

    public ContactAdapter(Context context, List<User> friendList, int waitingFriendRequestCount, OnFriendRequestCardClickListener listener) {
        this.context = context;
        this.friendList = friendList;
        this.waitingFriendRequestCount = waitingFriendRequestCount;
        this.onFriendRequestCardClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // show the summary of the friend request at the index 0
        if (position == 0 && waitingFriendRequestCount > 0) {
            return VIEW_TYPE_SUMMARY;
        } else {
            return VIEW_TYPE_CONTACT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SUMMARY) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_summary, parent, false);
            return new SummaryViewHolder(view, onFriendRequestCardClickListener);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.user_card, parent, false);
            return new ContactViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_SUMMARY) {
            ((SummaryViewHolder) holder).bind(waitingFriendRequestCount);
        } else {
            int actualPosition = waitingFriendRequestCount > 0 ? position - 1 : position;
            User friend = friendList.get(actualPosition);
            ((ContactViewHolder) holder).bind(friend);
        }
    }

    @Override
    public int getItemCount() {
        // check if any add friend request, if no, only show the friend list
        return waitingFriendRequestCount > 0 ? friendList.size() + 1 : friendList.size();
    }

    // ViewHolder for the summary card
    public static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView textFriendRequestSummary;

        public SummaryViewHolder(@NonNull View itemView, OnFriendRequestCardClickListener listener) {
            super(itemView);
            textFriendRequestSummary = itemView.findViewById(R.id.textFriendRequestSummary);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFriendRequestCardClick();  // notify MainActivity switch back to friendRequest
                }
            });
        }

        public void bind(int friendRequestCount) {
            textFriendRequestSummary.setText("You have " + friendRequestCount + " friend requests");
        }
    }

    // ViewHolder for the contact items
    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        public TextView userName;
        public ImageView profileImage;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.textViewUser);
            profileImage = itemView.findViewById(R.id.imageViewUser);
        }

        public void bind(User friend) {
            userName.setText(friend.getUserName());
            String imageUrl = friend.getImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.profile_placeholder)
                                .error(R.drawable.profile_error))
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.profile_placeholder);
            }

            // jump to chat activity
            itemView.setOnClickListener(view -> {
                Intent intent = new Intent(itemView.getContext(), MyTalkActivity.class);
                intent.putExtra("userName", friend.getUserName());
                intent.putExtra("friendName", friend.getUserName());
                intent.putExtra("friendId", friend.getId());

                itemView.getContext().startActivity(intent);
            });


        }
    }
}
