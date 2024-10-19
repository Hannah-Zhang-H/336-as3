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

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;

    public ContactAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.userName.setText(user.getUserName());

        String imageUrl = user.getImage();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            GlideApp.with(context)
                    .load(imageUrl)  // get url from user
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.profile_placeholder)
                            .error(R.drawable.profile_error))
                    .into(holder.profileImage);
        } else {
            // set place holder if image url is null
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        // start MyTalkActivity if user click the card
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, MyTalkActivity.class);
            intent.putExtra("userName", "CurrentUserName");
            intent.putExtra("friendName", user.getUserName());
            intent.putExtra("friendId", user.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView userName;
        public ImageView profileImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.textViewUser);
            profileImage = itemView.findViewById(R.id.imageViewUser);
        }
    }
}
