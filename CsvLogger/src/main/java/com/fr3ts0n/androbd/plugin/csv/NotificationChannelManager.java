package com.fr3ts0n.androbd.plugin.csv;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Creates the NotificationChannel on new Android phones
 *
 * Only call into this class if the phone is running Android O
 */
public class NotificationChannelManager
{
    public static final String NOTIFICATION_CHANNEL_ID = "AndrOBD_Csv_Logger";

    @TargetApi(Build.VERSION_CODES.O)
    public static void createNotificationChannel(Context context)
    {
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.lbl_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
}
