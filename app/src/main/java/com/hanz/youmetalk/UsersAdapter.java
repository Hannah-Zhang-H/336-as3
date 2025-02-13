package com.hanz.youmetalk;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * UsersAdapter is a RecyclerView adapter for displaying a list of users in the YouMeTalk application.
 * <p>
 * This adapter provides the functionality to display each user's profile image and name in a card view.
 * Tapping a user card navigates to MyTalkActivity, passing the user's name, friend ID, and friend name.
 * <p>
 * Attributes:
 * - `userList`: List of users to display.
 * - `context`: Context for managing resources.
 * - `currentUserName`: Name of the current user.
 * - `reference`: DatabaseReference for Firebase data retrieval.
 * - `database`: FirebaseDatabase instance.
 * <p>
 * Key Methods:
 * - `onCreateViewHolder()`: Inflates the layout for user cards.
 * - `onBindViewHolder()`: Binds each user’s data to the view holder, loading profile image and setting click listener.
 * - `getItemCount()`: Returns the size of the user list.
 */

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    List<User> userList;
    Context context;
    String currentUserName;

    DatabaseReference reference;
    FirebaseDatabase database;

    public UsersAdapter(Context context, List<User> userList, String currentUserName) {
        this.context = context;
        this.userList = userList;
        this.currentUserName = currentUserName;

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        String friendId = user.getId();
        // fetch user data
        reference.child("Users").child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String friendName = snapshot.child("userName").getValue(String.class);
                String imageURL = snapshot.child("image").getValue(String.class);

                // set username
                holder.textViewUser.setText(friendName != null ? friendName : "Unknown");

                // set user profile image
                if (imageURL != null && imageURL.equals("null")) {
                    holder.imageViewUser.setImageResource(R.drawable.account);
                } else if (imageURL != null) {
                    Picasso.get().load(imageURL).into(holder.imageViewUser);
                }

                //pass friendId and friendName to MyTalkActivity
                holder.cardView.setOnClickListener(view -> {
                    Intent intent = new Intent(context, MyTalkActivity.class);
                    intent.putExtra("userName", currentUserName);
                    intent.putExtra("friendId", friendId);
                    intent.putExtra("friendName", friendName != null ? friendName : "Unknown");
                    context.startActivity(intent);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewUser;
        private final CircleImageView imageViewUser;
        private final CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewUser = itemView.findViewById(R.id.textViewUser);
            imageViewUser = itemView.findViewById(R.id.imageViewUser);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
