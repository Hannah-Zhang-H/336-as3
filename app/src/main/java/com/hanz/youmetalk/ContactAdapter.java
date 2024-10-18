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
            // 使用 Glide 加载头像，添加占位符和错误处理
            GlideApp.with(context)
                    .load(imageUrl)  // 从 User 对象获取图片链接
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.profile_placeholder)  // 加载中时的占位符
                            .error(R.drawable.profile_error))  // 加载失败时的占位符
                    .into(holder.profileImage);
        } else {
            // 如果 imageUrl 为空，设置占位符
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        // 点击用户卡片，启动 MyTalkActivity
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, MyTalkActivity.class);
            intent.putExtra("userName", "CurrentUserName"); // 当前登录用户的用户名
            intent.putExtra("friendName", user.getUserName()); // 点击的好友名称
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
