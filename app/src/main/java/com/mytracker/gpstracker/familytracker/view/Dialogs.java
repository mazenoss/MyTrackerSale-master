package com.mytracker.gpstracker.familytracker.view;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Dialogs {
	DatabaseReference trackingRef, reqRef;
	DatabaseReference reference;
	FirebaseUser user;

	public Dialogs() {
		user = FirebaseAuth.getInstance().getCurrentUser();
		reference = FirebaseDatabase.getInstance().getReference("Users");
		trackingRef = reference.child(user.getPhoneNumber()).child("tracking");
		reqRef = reference.child(user.getPhoneNumber()).child("requests");
	}

	public void requestDialog(final Context context, final String phone) {
		new AlertDialog.Builder(context)
				.setTitle("New Request from " + phone)
				.setMessage("Accept request?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						// 1- add to tracking (user1 and user2)
						// 2- delete from requests (user1)
						// 3- show on map
						trackingRef.child(phone).setValue("tracking");
						reference.child(phone).child("tracking").child(user.getPhoneNumber()).setValue("tracking");
						reqRef.child(phone).removeValue();
						Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						// 1- delete from requests
						reqRef.child(phone).removeValue();
						Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show();
					}
				})
				.create().show();
	}


}
