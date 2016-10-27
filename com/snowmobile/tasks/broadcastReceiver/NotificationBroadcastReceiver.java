package com.snowmobile.tasks.broadcastReceiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat.Builder;
import android.util.Log;
import com.snowmobile.tasks.C0220R;
import com.snowmobile.tasks.DBHelper;
import com.snowmobile.tasks.MainActivity;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    private static final int notifyID = 1;
    private DBHelper dbHelper;
    private NotificationManager mNotificationManager;

    public void onReceive(Context context, Intent intent) {
        Log.d("1", "onReceive");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.dbHelper = new DBHelper(context);
        int todayTasksNumber = this.dbHelper.findTodayTasksNumber(this.dbHelper.getAllTasks());
        if (todayTasksNumber != 0) {
            this.mNotificationManager.notify(notifyID, ((Builder) new Builder(context).setContentTitle("Your Tasks for today").setContentText("You've " + Integer.toString(todayTasksNumber) + " unfinished tasks for today.").setSmallIcon(C0220R.mipmap.ic_launcher).setContentIntent(PendingIntent.getActivity(context, notifyID, new Intent(context, MainActivity.class), 268435456)).setOngoing(true)).build());
            return;
        }
        this.mNotificationManager.cancel(notifyID);
    }
}
