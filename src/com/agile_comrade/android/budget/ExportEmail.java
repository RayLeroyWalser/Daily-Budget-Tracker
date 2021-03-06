package com.agile_comrade.android.budget;

import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.agile_comrade.android.budget.R;

public class ExportEmail {
	private Context mContext;
	private static final int NOTIFY_EMAIL = 1;
	private static final String EMAIL_SUBJECT = "[Daily Budget Tracker] Yesterday's calorie history:";
	
	public ExportEmail(Context context) {
		mContext = context;
	}
	public void createNotification() {
		// Create a notification in the top bar, so that the user knows that he/she can send the result email.
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(ns);
		int icon = R.drawable.status; 
		CharSequence contentTitle = "Daily Budget Tracker: Email notice";
		CharSequence contentText = "Yesterday's results are available.";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, contentTitle, when);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		
		Intent email = Intent.createChooser(createEmail(), "Send mail");
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, email, 0);

		notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
		
		mNotificationManager.notify(NOTIFY_EMAIL, notification);
	}
		public Intent createEmail() {
		// This intent creates the email with fields filled out.
		SharedPreferences settings = mContext.getSharedPreferences(DailyBudgetTracker.PREFS_NAME, 0);
    	String email = settings.getString(DailyBudgetTracker.EMAIL, "");
    	
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
		i.putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT);
		i.putExtra(Intent.EXTRA_TEXT, getHistory());
		return i;
	}
	
	public void startChooser() {
		// Brings up the chooser menu to select email client.
		Intent email = Intent.createChooser(createEmail(), "Send mail");
		mContext.startActivity(email);
	}
	
	public String getHistory() {
		// Return the body of text for the result email.
		// This is the previous day's history.
		SharedPreferences settings = mContext.getSharedPreferences(DailyBudgetTracker.PREFS_NAME, 0);
		int budget = settings.getInt(DailyBudgetTracker.BUDGET, 2000);
		int total = 0;
		StringBuffer history = new StringBuffer();

		// create string of food and value for previous day's consumption
		TrackingDatabase db_helper = new TrackingDatabase(mContext);;
		SQLiteDatabase db = db_helper.getReadableDatabase();
		
		// First determine the end of the previous day
		long t_time = System.currentTimeMillis();
		Calendar today = Calendar.getInstance();
		today.setTimeInMillis(t_time);
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		t_time = today.getTimeInMillis();	
		
		// Then get the start of the previous
		today.add(Calendar.DATE, -1);
		long y_time = today.getTimeInMillis();
		
        Cursor cursor = db.rawQuery("SELECT * FROM Track " +
        							"WHERE DateTime > " + y_time + " AND DateTime < " + t_time, null);
        if (cursor.moveToFirst()) {
            do {
            	String name = cursor.getString(1);
            	int value = Integer.parseInt(cursor.getString(2));
            	total += value;
				history.append(name + ", " + value + "\n");
            } while (cursor.moveToNext());
        }
        db.close();

        // Header for the result email.
		history.insert(0, "Yesterday, you consumed " + total + " out of " + budget + " calories.\n\nIt consisted of:\n");
		return history.toString();
	}
	
}
