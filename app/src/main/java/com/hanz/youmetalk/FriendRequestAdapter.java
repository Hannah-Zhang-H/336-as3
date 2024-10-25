package com.hanz.youmetalk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;

/**
 * The FriendRequestAdapter class provides a RecyclerView adapter to display and manage friend requests in the YouMeTalk application.
 * It manages a list of incoming friend requests, allowing the current user to accept or decline requests directly from the interface.

 * Key features:
 * - Fetches user information for each friend request, displaying the requester's name, YouMeID, and profile picture.
 * - Allows the user to accept or decline friend requests with immediate updates to Firebase,
 *   including adding or removing friends and updating the friend request status.
 * - Notifies the main activity or fragment when a request is accepted or declined, triggering an adapter switch if needed.
 * - Utilizes Glide for efficient image loading and caching, ensuring smooth image display.

 * This class includes methods to:
 * - Accept or decline requests.
 * - Update request statuses and refresh the view after a change.
 * - Listen for request acceptance to notify the main context to switch adapters.
 */
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {
    private final Context context;
    private final List<FriendRequest> friendRequestList;
    private final FriendRequestListener listener;

    // Define interface to notify listener about the request acceptance
    public interface FriendRequestListener {
        void onFriendRequestAccepted();
    }

    public FriendRequestAdapter(Context context, List<FriendRequest> friendRequestList, FriendRequestListener listener) {
        this.context = context;
        this.friendRequestList = friendRequestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        FriendRequest request = friendRequestList.get(position);

        // only show the request that has a "waiting" status
        if (!"waiting".equals(request.getStatus())) {
            holder.itemView.setVisibility(View.GONE);
            return;
        }

        // get the UID for the user who sent the request
        String fromUid = request.getFrom();
        if (fromUid == null) {
            return;
        }

        // fetch the user data by using UID
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(fromUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String fromUserName = dataSnapshot.child("userName").getValue(String.class);
                    String fromYouMeId = dataSnapshot.child("youMeId").getValue(String.class);
                    String fromImageUrl = dataSnapshot.child("image").getValue(String.class);

                    holder.textFromUser.setText(fromUserName != null ? fromUserName : "Unknown");
                    holder.textFromYouMeId.setText(fromYouMeId != null ? fromYouMeId : "Unknown");

                    if (fromImageUrl != null && !fromImageUrl.isEmpty()) {
                        Glide.with(context)
                                .load(fromImageUrl)
                                .placeholder(R.drawable.profile_placeholder)
                                .error(R.drawable.profile_error)
                                .into(holder.profileImageView);
                    } else {
                        holder.profileImageView.setImageResource(R.drawable.profile_placeholder);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FRA db err", databaseError.getMessage());
            }
        });

        holder.buttonAccept.setOnClickListener(v -> acceptFriendRequest(request));

        holder.buttonDecline.setOnClickListener(v -> declineFriendRequest(request));
    }

    @Override
    public int getItemCount() {
        return friendRequestList.size();
    }

    static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        TextView textFromUser, textFromYouMeId;
        ImageView profileImageView;
        Button buttonAccept, buttonDecline;

        public FriendRequestViewHolder(View itemView) {
            super(itemView);
            textFromUser = itemView.findViewById(R.id.textFromUser);
            textFromYouMeId = itemView.findViewById(R.id.textFromYouMeId);
            profileImageView = itemView.findViewById(R.id.imageViewProfile);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonDecline = itemView.findViewById(R.id.buttonDecline);
        }
    }

    private void acceptFriendRequest(FriendRequest request) {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        if (request.getFrom() == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId).child("Friends");

        // Update the friend field and friend request table
        userRef.child(request.getFrom()).setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference().child("Users").child(request.getFrom()).child("Friends");
                senderRef.child(currentUserId).setValue(true).addOnCompleteListener(senderTask -> {
                    if (senderTask.isSuccessful()) {
                        updateRequestStatusAndNotify(request);
                    }
                });
            }
        });
    }

    // Notify the listener and update the status
    private void updateRequestStatusAndNotify(FriendRequest request) {
        if (request.getRequestId() == null) {
            return;
        }

        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference()
                .child("FriendRequest")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .child(request.getRequestId());

        requestRef.child("status").setValue("accepted").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Show success message
                Toast.makeText(context, "Friend successfully added!", Toast.LENGTH_SHORT).show();

                // Notify the listener to switch adapter
                if (listener != null) {
                    listener.onFriendRequestAccepted();  // Notify the activity/fragment to switch adapter
                }
            }
        });
    }

    // Reload the view after declining the request
    private void declineFriendRequest(FriendRequest request) {
        updateRequestStatusAndReload(request);
        // Remind the user that they have declined the adding new friend request
        Toast.makeText(context, R.string.decline_add_friend, Toast.LENGTH_SHORT).show();

        // Notify the listener to switch back to contact list after declining the request
        if (listener != null) {
            listener.onFriendRequestAccepted();  // Reuse the same method to switch adapter
        }
    }

    private void updateRequestStatusAndReload(FriendRequest request) {
        if (request.getRequestId() == null) {
            return;
        }

        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference()
                .child("FriendRequest")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .child(request.getRequestId());

        requestRef.child("status").setValue("declined").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reloadFriendRequests();
            }
        });
    }

    // Reload the friend request list
    private void reloadFriendRequests() {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference().child("FriendRequest").child(currentUserId);

        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendRequestList.clear();  // clear the list

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    FriendRequest friendRequest = requestSnapshot.getValue(FriendRequest.class);
                    if (friendRequest != null && "waiting".equals(friendRequest.getStatus())) {
                        friendRequest.setRequestId(requestSnapshot.getKey());
                        friendRequestList.add(friendRequest);
                    }
                }

                notifyDataSetChanged();  // refresh RecyclerView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Reload Friend Requests", error.getMessage());
            }
        });
    }
}
