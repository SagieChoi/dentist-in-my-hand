package com.toothfairy.dentist;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pending = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "default");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground);

            String channelName = "전날 알람 채널";
            String description = "그 전날 알림을 띄운다";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel("default", channelName, importance);
            channel.setDescription(description);

            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        } else {
            builder.setSmallIcon(R.mipmap.ic_launcher);
        }
        builder.setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setTicker("{Time to watch some cool stuff!}")
                .setContentTitle("상태바 드래그시 보이는 타이틀")
                .setContentText("상태바 드래그시 보이는 서브타이틀")
                .setContentInfo("INFO")
                .setContentIntent(pending);

        if (notificationManager != null) {
            // 노티피케이션 동작시킴
            notificationManager.notify(1234, builder.build());

        }
    }
}