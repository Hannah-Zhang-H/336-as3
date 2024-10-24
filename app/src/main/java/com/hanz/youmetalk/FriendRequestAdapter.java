package com.hanz.youmetalk;

import android.content.Context;
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

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {
    private Context context;
    private List<FriendRequest> friendRequestList;

    public FriendRequestAdapter(Context context, List<FriendRequest> friendRequestList) {
        this.context = context;
        this.friendRequestList = friendRequestList;
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

        // only show the request has waiting status
        if (!"waiting".equals(request.getStatus())) {
            holder.itemView.setVisibility(View.GONE);
            return;
        }

        // get the Uid for the user who send the request
        String fromUid = request.getFrom();
        if (fromUid == null) {
            return;
        }

        // fetch the user date by using Uid
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

            }
        });

        holder.buttonAccept.setOnClickListener(v -> {
            acceptFriendRequest(request);
        });

        holder.buttonDecline.setOnClickListener(v -> {
            declineFriendRequest(request);
        });
    }

    @Override
    public int getItemCount() {
        return friendRequestList.size();
    }

    class FriendRequestViewHolder extends RecyclerView.ViewHolder {
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

    // reload the view after accept the request
    private void acceptFriendRequest(FriendRequest request) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null || request.getFrom() == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId).child("Friends");

        //Update the friend field and friendrequest table
        userRef.child(request.getFrom()).setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference().child("Users").child(request.getFrom()).child("Friends");
                senderRef.child(currentUserId).setValue(true).addOnCompleteListener(senderTask -> {
                    if (senderTask.isSuccessful()) {
                        updateRequestStatusAndReload(request, "accepted");



                    }
                });
            }
        });
    }

    // reload the view after decline the request
    private void declineFriendRequest(FriendRequest request) {
        updateRequestStatusAndReload(request, "declined");
        // Remind the user that they have declined the adding new friend request
        Toast.makeText(context, R.string.decline_add_friend, Toast.LENGTH_SHORT).show();
    }

    private void updateRequestStatusAndReload(FriendRequest request, String status) {
        if (request.getRequestId() == null) {
            return;
        }

        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference()
                .child("FriendRequest")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(request.getRequestId());

        requestRef.child("status").setValue(status).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reloadFriendRequests();
            }
        });
    }

    // reload the friend request list
    private void reloadFriendRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference().child("FriendRequest").child(currentUserId);

        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
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

            }
        });
    }
}
