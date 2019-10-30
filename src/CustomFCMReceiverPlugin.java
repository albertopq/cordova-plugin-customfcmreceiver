package cordova.plugin.customfcmreceiver;
import android.app.Activity;
import android.util.Log;
import com.google.firebase.messaging.RemoteMessage;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import android.app.Notification;
import android.os.Bundle;
import android.graphics.BitmapFactory;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.firebase.FirebasePluginMessageReceiver;
import org.apache.cordova.firebase.OnNotificationOpenReceiver;
import org.apache.cordova.firebase.FirebasePlugin;
import android.content.Context;
import android.content.Intent;
import java.util.Map;
import java.util.Random;
import com.google.firebase.messaging.FirebaseMessagingService;


public class CustomFCMReceiverPlugin extends CordovaPlugin {

    public static CustomFCMReceiverPlugin instance = null;
    private static Activity cordovaActivity = null;
    private static Context applicationContext = null;
    static final String TAG = "CustomFCMReceiverPlugin";
    static final String javascriptNamespace = "cordova.plugin.customfcmreceiver";
    static final String defaultSmallIconName = "notification_icon";
    static final String defaultLargeIconName = "notification_icon_large";

    private CustomFCMReceiver customFCMReceiver;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.d(TAG, "initialize");
        try {
            instance = this;
            this.webView = webView;
            customFCMReceiver = new CustomFCMReceiver();
            cordovaActivity = cordova.getActivity();
            applicationContext = cordovaActivity.getApplicationContext();

        }catch (Exception e){
            handleException("Initializing plugin", e);
        }
        super.initialize(cordova, webView);
    }

    protected static void handleError(String errorMsg) {
        Log.e(TAG, errorMsg);
    }

    protected static void handleException(String description, Exception exception) {
        handleError(description + ": " + exception.toString());
    }

    private class CustomFCMReceiver extends FirebasePluginMessageReceiver {
        @Override
        public boolean onMessageReceived(RemoteMessage remoteMessage){
            Log.d("CustomFCMReceiver", "onMessageReceived");
            boolean isHandled = false;

            Map<String, String> data = remoteMessage.getData();

            if (remoteMessage.getNotification() == null) {
              boolean showNotification = (FirebasePlugin.inBackground() || !FirebasePlugin.hasNotificationsCallback());
              if (data != null && showNotification) {
                sendMessage(remoteMessage, data);
                isHandled = true;
              }

            }

            return isHandled;
        }
    }

    private void sendMessage(RemoteMessage remoteMessage, Map<String, String> data) {
      String titleField = getStringResource("custom_fcm_title");
      String bodyField = getStringResource("custom_fcm_body");
      String title = null;
      String body = null;
      String channelId = FirebasePlugin.defaultChannelId;
      String icon = defaultSmallIconName;
      Random rand = new Random();
      int n = rand.nextInt(50) + 1;
      String id = Integer.toString(n);

      if(data.containsKey(titleField)) title = data.get(titleField);
      if(data.containsKey(bodyField)) body = data.get(bodyField);
      Bundle bundle = new Bundle();
      for (String key : data.keySet()) {
          bundle.putString(key, data.get(key));
      }

      Intent intent = new Intent(applicationContext, OnNotificationOpenReceiver.class);
      NotificationManager notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
      PendingIntent pendingIntent = PendingIntent.getBroadcast(applicationContext, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
      NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(applicationContext, channelId);
        // Icon
        int defaultSmallIconResID = applicationContext.getResources().getIdentifier(defaultSmallIconName, "drawable", applicationContext.getPackageName());
        int customSmallIconResID = 0;

        if (customSmallIconResID != 0) {
            notificationBuilder.setSmallIcon(customSmallIconResID);
            Log.d(TAG, "Small icon: custom="+icon);
        }else if (defaultSmallIconResID != 0) {
            Log.d(TAG, "Small icon: default="+defaultSmallIconName);
            notificationBuilder.setSmallIcon(defaultSmallIconResID);
        } else {
            Log.d(TAG, "Small icon: application");
            notificationBuilder.setSmallIcon(applicationContext.getApplicationInfo().icon);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int defaultLargeIconResID = applicationContext.getResources().getIdentifier(defaultLargeIconName, "drawable", applicationContext.getPackageName());
            int customLargeIconResID = 0;

            int largeIconResID;
            if (customLargeIconResID != 0) {
                largeIconResID = customLargeIconResID;
                Log.d(TAG, "Large icon: custom="+icon);
            }else if (defaultLargeIconResID != 0) {
                Log.d(TAG, "Large icon: default="+defaultLargeIconName);
                largeIconResID = defaultLargeIconResID;
            } else {
                Log.d(TAG, "Large icon: application");
                largeIconResID = applicationContext.getApplicationInfo().icon;
            }
            notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(applicationContext.getResources(), largeIconResID));
        }
      notificationBuilder
              .setContentTitle(title)
              .setContentText(body)
              .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
              .setAutoCancel(true)
              .setContentIntent(pendingIntent);
      // Build notification
      Notification notification = notificationBuilder.build();
      notificationManager.notify(id.hashCode(), notification);
    }

    private String getStringResource(String name) {
        return applicationContext.getString(
                applicationContext.getResources().getIdentifier(
                        name, "string", applicationContext.getPackageName()
                )
        );
    }
}
