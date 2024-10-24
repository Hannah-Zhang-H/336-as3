package com.hanz.youmetalk;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MessageChecker extends Worker {

    private static final String CHANNEL_ID = "message_channel";

    public MessageChecker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        checkForUnreadMessages();
        return Result.success();
    }

    // check the unread messages in Firebase
    private void checkForUnreadMessages() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference messagesRef = database.getReference("Messages");

        String currentUserId = auth.getCurrentUser().getUid();
        if (currentUserId == null) {
            return; // if no user signed in, do not check
        }

        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadCount = 0;

                // check all messages for unread status
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot messageDetailSnapshot : messageSnapshot.child("messages").getChildren()) {
                        String toUid = messageDetailSnapshot.child("to").getValue(String.class);
                        Boolean isRead = messageDetailSnapshot.child("isRead").getValue(Boolean.class);

                        // Count if the message is unread and directed to the current user
                        if (toUid != null && toUid.equals(currentUserId) && isRead != null && !isRead) {
                            unreadCount++;
                        }
                    }
                }

                // if has unread message send notification and show the count
                if (unreadCount > 0) {
                    sendNotification(unreadCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // send notification，show the unread messages count
    private void sendNotification(int unreadCount) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Message Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // start mainActivity when click the notification
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // create notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)  // 设置通知图标
                .setContentTitle("You have " + unreadCount + " unread message(s)")
                .setContentText("Check your messages.")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // send notification
        notificationManager.notify(1, notificationBuilder.build());
    }
}
