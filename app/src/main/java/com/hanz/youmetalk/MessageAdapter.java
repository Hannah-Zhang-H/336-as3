
package com.hanz.youmetalk;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    List<Model> list;
    String userName;

    boolean status;
    int send;
    int receive;

    public MessageAdapter(List<Model> list, String userName) {
        this.list = list;
        this.userName = userName;

        status = false;
        send = 1;
        receive = 2;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == send) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_send_card, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_receive_card, parent, false);

        }

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Model model = list.get(position);

        // 设置消息文本
        holder.textView.setText(model.getMessage());

        // 检查是否有头像 URL
        String imageUrl = model.getImage();
        Log.d("MessageAdapter", "Image URL: " + imageUrl); // 调试信息

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // 使用 Glide 加载头像
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.profile_placeholder)  // 占位符
                    .error(R.drawable.profile_error)              // 错误图片
                    .into(holder.profileImage);
        } else {
            // 没有头像时使用占位符
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView textView;
        ImageView profileImage; // 新增用于显示头像的 ImageView

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            if (status) {
                textView = itemView.findViewById(R.id.textViewSend);
                profileImage = itemView.findViewById(R.id.imageViewSend); // 发送消息的头像
            } else {
                textView = itemView.findViewById(R.id.textViewReceived);
                profileImage = itemView.findViewById(R.id.imageViewReceived); // 接收消息的头像
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        // If the users themselves send the messages
        if (list.get(position).getFrom().equals(userName)) {
            status = true;
            return send;
        } else {
            status = false;
            return receive;
        }
    }
}
