package com.mytracker.gpstracker.familytracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mytracker.gpstracker.familytracker.view.Dialogs;

import timber.log.Timber;

import static com.mytracker.gpstracker.familytracker.model.Constants.REF_REQUESTS;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_USERS;

public class RequestService extends Service {
	DatabaseReference requestRef;
	FirebaseUser user;

	Notification.Builder builder;
	NotificationManager notificationManager;

	private final String CHANNEL_ID = "1000";
	private final int CHANNEL_ID_INT = 1000;
	private final String CHANNEL_NAME = "requestChannel";

	BroadcastReceiver receiver;
	IntentFilter intentFilter;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		}
		intentFilter = new IntentFilter("request_action");
		user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null || user.getPhoneNumber() == null) {
			stopSelf();
			return;
		}

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
			}
		};
		initNotification();

		registerReceiver(receiver, intentFilter);

		requestRef = FirebaseDatabase.getInstance()
				.getReference().child(REF_USERS)
				.child(user.getPhoneNumber())
				.child(REF_REQUESTS);

		requestRef.addChildEventListener(new ChildEventListener() {
			@Override
			public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
				Timber.d("phoone2: %s", String.valueOf(dataSnapshot.getKey()));
				showNotification(dataSnapshot.getKey());
			}

			@Override
			public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

			}

			@Override
			public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

			}

			@Override
			public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

			}

			@Override
			public void onCancelled( DatabaseError databaseError) {

			}
		});
	}


	private void showNotification(String phone) {
		Intent intent = new Intent(this, MyNavigationTutorial.class);
		intent.setAction("request_action");
		intent.putExtra("phone", phone);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent
				.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);


		builder.setContentTitle("New Request")
		.setContentText(phone)
		.setAutoCancel(true)
		.setSmallIcon(R.mipmap.ic_launcher)
		.setContentIntent(pendingIntent);

		notificationManager.notify(phone, Integer.parseInt(CHANNEL_ID), builder.build());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	private void initNotification(){
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			builder = new Notification.Builder(this, CHANNEL_ID);
			createChannel();
		} else {
			builder = new Notification.Builder(this);
		}
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private void createChannel(){
		int priority;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			priority = Notification.PRIORITY_DEFAULT;
		} else {
			priority = NotificationManager.IMPORTANCE_DEFAULT;
		}
		NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, priority);
		channel.enableLights(true);
		channel.enableVibration(true);
		notificationManager.createNotificationChannel(channel);
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		notificationManager.cancelAll();
		unregisterReceiver(receiver);
	}
}
