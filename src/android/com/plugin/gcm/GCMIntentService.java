package com.plugin.gcm;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	public static final int NOTIFICATION_ID = 237;
	private static final String TAG = "GCMIntentService";

	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
        Log.d(TAG, "onMessage - context: " + context);

        // Extract the payload from the message
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
            if (!PushPlugin.sendExtras(extras, false))
            {
                try
                {
                    JSONObject obj = PushPlugin.convertBundleToJson(extras);
					String text = "";
					if (obj.has("payload"))
					{
						JSONObject subobj = obj.getJSONObject("payload").getJSONObject("alert");
						if (subobj.has("alert"))
						{
							text = subobj.getString("body");
						}
						else
						{
							subobj = subobj.getJSONObject("data");
							text = subobj.getString("message");
						}
					}
					else
					{
						JSONObject subobj = obj.getJSONObject("data");
						text = subobj.getString("message");
					}
                    createNotification(context, text, text, 1);
                }
                catch (JSONException ex)
                {
					JSONObject obj = PushPlugin.convertBundleToJson(extras);
                    Log.d(TAG, "JSONObject received does not contain needed data (payload.alert.body, nor data.message), ignoring. Full message: "+obj.toString());
                }
            }
        }
	}

	public static void createNotification(Context context, String title, String message, int count)
	{
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(context);

		Intent notificationIntent = new Intent(context, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(context)
				.setDefaults(Notification.DEFAULT_ALL)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(title)
				.setTicker(title)
				.setContentIntent(contentIntent);

		if (message != null)
			mBuilder.setContentText(message);
		else
			mBuilder.setContentText("<missing message content>");

		if (count != 0)
			mBuilder.setNumber(count);

		mNotificationManager.notify((String) appName, NOTIFICATION_ID, mBuilder.build());
	}

	public static void cancelNotification(Context context)
	{
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel((String)getAppName(context), NOTIFICATION_ID);
	}

	private static String getAppName(Context context)
	{
		CharSequence appName =
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());

		return (String)appName;
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
