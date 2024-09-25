package com.hanz.youmetalk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    List<String> userList;
    Context context;
    String userName;

    public UsersAdapter(Context context, List<String> userList, String userName) {
        this.context = context;
        this.userList = userList;
        this.userName = userName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_card, parent, false);


        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

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
