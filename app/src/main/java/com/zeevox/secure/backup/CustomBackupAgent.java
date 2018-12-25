package com.zeevox.secure.backup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.backup.BackupAgentHelper;
import android.content.Intent;
import android.os.Build;

import com.zeevox.secure.App;
import com.zeevox.secure.R;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class CustomBackupAgent extends BackupAgentHelper {
    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();

        final int AUTOBACKUP_NOTIFICATION_ID = 846402;

        // Create a notification channel, but only on API 26+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_autobackup_title);
            String description = getString(R.string.notification_channel_autobackup_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("com.zeevox.secure.AUTOBACKUP", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, App.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "com.zeevox.secure.AUTOBACKUP")
                .setSmallIcon(R.drawable.ic_backup_restore)
                .setContentTitle("Restore complete")
                .setContentText("Your passwords have been restored from the cloud")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(AUTOBACKUP_NOTIFICATION_ID, mBuilder.build());
    }
}
