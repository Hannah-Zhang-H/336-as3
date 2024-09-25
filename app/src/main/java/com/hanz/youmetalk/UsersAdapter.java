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

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    List<String> userList;
    Context context;
    String userName;

    DatabaseReference reference;
    FirebaseDatabase database;

    public UsersAdapter(Context context, List<String> userList, String userName) {
        this.context = context;
        this.userList = userList;
        this.userName = userName;

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
        reference.child("Users").child(userList.get(position)).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String friendName = snapshot.child("userName").getValue().toString();
                String imageURL = snapshot.child("image").getValue().toString();

                holder.textViewUser.setText(friendName);

                if(imageURL.equals("null"))
                {
                    holder.imageViewUser.setImageResource(R.drawable.account);
                }
                else Picasso.get().load(imageURL).into(holder.imageViewUser);

                holder.cardView.setOnClickListener(view -> {
                    Intent intent = new Intent(context, MyTalkActivity.class);
                    intent.putExtra("userName", userName);
                    intent.putExtra("friendName", friendName);
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


    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewUser;
        private CircleImageView imageViewUser;
        private CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);


            textViewUser = itemView.findViewById(R.id.textViewUser);
            imageViewUser = itemView.findViewById(R.id.imageViewUser);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
