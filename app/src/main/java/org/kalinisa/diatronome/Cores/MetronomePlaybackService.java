package org.kalinisa.diatronome.Cores;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.kalinisa.diatronome.R;

public class MetronomePlaybackService extends Service
{
  @Override
  public void onCreate()
  {
    super.onCreate();
    startForeground();
  }

  @Override
  public IBinder onBind(Intent intent) {
    // We don't provide binding, so return null
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    super.onStartCommand(intent, flags, startId);
    MetronomeCore.getInstance().play();
    return START_STICKY;
    // return START_NOT_STICKY;
  }

  @Override
  public void onDestroy()
  {
    MetronomeCore.getInstance().stop();
  }

  public void startForeground()
  {
    int permission = PackageManager.PERMISSION_DENIED;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
    {
      permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE);
    }
    if (permission == PackageManager.PERMISSION_DENIED)
    {
      stopSelf();
      // Start as non service
      MetronomeCore.getInstance().play();
      return;
    }

    Intent notificationIntent = new Intent(this, org.kalinisa.diatronome.MainActivity.class);
    Notification notification = null;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
    {
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

      notification = new NotificationCompat.Builder(this, this.getChannelId())
        .setOngoing(true)
        .setSmallIcon(R.drawable.app_icon)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(Notification.CATEGORY_SERVICE)
        .setContentTitle(getString(R.string.metronome_notificationtitle))
        .setContentText("" + MetronomeCore.getInstance().getTempoBpm() + " BPM / " + MetronomeCore.getInstance().getSubDivision() + ":" + MetronomeCore.getInstance().getDivision())
        .setContentIntent(pendingIntent)
        // Create the notification to display while the service is running
        .build();
    }
    startForeground
    (
      1686, // Id, Cannot be 0
      notification
    );
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private String getChannelId()
  {
    String channelId = "MetronomePlayBackService";
    String channelName = "Metronome PlayBack";
    NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
    // omitted the LED color
    channel.setImportance(NotificationManager.IMPORTANCE_NONE);
    channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
    ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    return channelId;
  }
}
