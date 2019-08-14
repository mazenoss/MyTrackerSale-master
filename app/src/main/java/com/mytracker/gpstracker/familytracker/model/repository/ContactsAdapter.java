package com.mytracker.gpstracker.familytracker.model.repository;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mytracker.gpstracker.familytracker.BuildConfig;
import com.mytracker.gpstracker.familytracker.R;

import java.util.ArrayList;
import java.util.HashMap;

import timber.log.Timber;

import static com.mytracker.gpstracker.familytracker.model.Constants.REF_NAME;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_PROFILE;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder> {

	ArrayList<String> names, phones, registers;
	Context context;
	DatabaseReference usersRef;
	HashMap<String, String>  registeredUsers;
	FirebaseUser user;

	public ContactsAdapter(Context context) {
		if(BuildConfig.DEBUG) Timber.plant(new Timber.DebugTree());
		names = new ArrayList<>();
		phones = new ArrayList<>();
		registers = new ArrayList<>();
		this.context = context;
//		contacts = new Contacts(context).getContactList();
//		extractContacts();
		registeredUsers = new HashMap<>();
		initDataBase();
		user = FirebaseAuth.getInstance().getCurrentUser();
	}

//	private void extractContacts(){
//		names.addAll(contacts.values());
//		phones.addAll(contacts.keySet());
//	}

	public void addContact(String name, String phone) {
		names.add(name);
		phones.add(phone);
		notifyDataSetChanged();
	}

	private void initDataBase(){
		usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
		usersRef.addChildEventListener(new ChildEventListener() {
			@Override
			public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
				String phone = String.valueOf(dataSnapshot.getKey());
				String phone1 = phone.substring(2);
				Timber.d("phone: %s", phone1);
				if (phones.contains(phone)
						|| phones.contains(phone1)) { // registered user in contacts
					String name = String.valueOf(dataSnapshot.child(REF_PROFILE).child(REF_NAME).getValue());
					Timber.d("name: %s phone: %s", name, phone);
					if (!dataSnapshot.getKey().equals(user.getPhoneNumber())) {
						registers.add(name);
						notifyDataSetChanged();
					}
				}
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
			public void onCancelled(@NonNull DatabaseError databaseError) {

			}
		});
	}
	@NonNull
	@Override
	public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ContactsViewHolder(
				LayoutInflater.from(parent.getContext())
				.inflate(R.layout.contact_item, parent, false)
		);
	}

	@Override
	public void onBindViewHolder(@NonNull ContactsViewHolder holder, final int position) {
		Timber.d("onBind: %s", registers.get(position));
		holder.name.setText(registers.get(position));
		// TODO: 11/08/19 add phone later
		holder.layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				searchForPhone(registers.get(position));
			}
		});
	}

	private void searchForPhone(final String name) {
		usersRef.addChildEventListener(new ChildEventListener() {
			@Override
			public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
				String result = String.valueOf(dataSnapshot.child("profile").child("name").getValue());
				if (result != null && result.equals(name)) {
					String phone = dataSnapshot.getKey();
					sendRequest(phone);
					dataSnapshot.getRef().removeEventListener(this);
				}
				result = null;
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
			public void onCancelled(@NonNull DatabaseError databaseError) {

			}
		});
	}

	private void sendRequest(String phone) {

		usersRef.child(phone).child("requests").child(user.getPhoneNumber())
				.addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						if (dataSnapshot.exists()) {
							Toast.makeText(context, "Request already sent", Toast.LENGTH_SHORT).show();
						} else {
							dataSnapshot.getRef().setValue("pending");
							Toast.makeText(context, "Request Sent", Toast.LENGTH_SHORT).show();
						}
						dataSnapshot.getRef().removeEventListener(this);
					}

					@Override
					public void onCancelled(@NonNull DatabaseError databaseError) {
						Timber.d("onCancelled");
					}

				});


	}

	@Override
	public int getItemCount() {
		Timber.d("size: %s", registers.size());
		return registers.size();
	}

	class ContactsViewHolder extends RecyclerView.ViewHolder{

		TextView name, phone;
		View layout;
		public ContactsViewHolder(@NonNull View itemView) {
			super(itemView);
			name = itemView.findViewById(R.id.contact_name);
			phone = itemView.findViewById(R.id.contact_phone);
			layout = itemView.findViewById(R.id.layout);
		}
	}
}
