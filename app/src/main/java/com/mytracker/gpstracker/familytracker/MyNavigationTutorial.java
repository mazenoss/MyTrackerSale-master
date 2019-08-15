package com.mytracker.gpstracker.familytracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;


import android.widget.TextView;
import android.widget.Toast;


//import com.google.android.gms.ads.AdListener;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.InterstitialAd;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mytracker.gpstracker.familytracker.model.repository.Contacts;
import com.mytracker.gpstracker.familytracker.view.ContactsActivity;
import com.mytracker.gpstracker.familytracker.view.Dialogs;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.zip.Inflater;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

import static com.mytracker.gpstracker.familytracker.model.Constants.REF_LOCATION;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_PHOTO;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_PROFILE;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_TRACKING;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_USERS;

public class MyNavigationTutorial extends AppCompatActivity
        implements OnMapReadyCallback {

    private final int LOCATION_RQ = 1000, CALL_RQ = 2000, GALLERY_RQ = 3000, CONTACTS_RQ = 4000;
    private String phone;
    GoogleMap mMap;
    GoogleApiClient client;
    FirebaseAuth auth;
    FirebaseUser user;
    DatabaseReference usersRef, profileRef, trackingRef, requestRef;
    TextView textName, textPhone;
    CircleImageView circleImageView;
    HashMap<String, Marker> markers;
    boolean first;
    HashMap<String, String> contacts;

//    InterstitialAd interstitialAd;

    Toolbar toolbar;

    StorageReference storageReference;
    DatabaseReference photoRef;
    Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_navigation_tutorial);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Family GPS Tracker");
        setSupportActionBar(toolbar);

        contacts = new HashMap<>();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_CONTACTS}, CONTACTS_RQ);
        } else {
            contacts = new Contacts(this).getContactList();
        }

        if (BuildConfig.DEBUG) Timber.plant(new Timber.DebugTree());

        Dialogs dialogs = new Dialogs();
        Timber.d("actioon: %s", getIntent().getAction());
        if ("request_action".equals(getIntent().getAction())) {
            dialogs.requestDialog(this, getIntent().getStringExtra("phone"));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        textName = (TextView) header.findViewById(R.id.nameTxt);
        textPhone = (TextView) header.findViewById(R.id.emailTxt);
        circleImageView = (CircleImageView) header.findViewById(R.id.imageView2);


        circleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
        auth = FirebaseAuth.getInstance();

        markers = new HashMap<>();
        first = true;
        user = auth.getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference()
                .child(user.getPhoneNumber());

        textPhone.setText(user.getPhoneNumber());
        textName.setText(user.getDisplayName());

        // Firebase nodes
        usersRef = FirebaseDatabase.getInstance().getReference().child(REF_USERS);
        profileRef = usersRef.child(user.getPhoneNumber()).child(REF_PROFILE);
        trackingRef = usersRef.child(user.getPhoneNumber()).child(REF_TRACKING);
        photoRef = usersRef.child(user.getPhoneNumber()).child(REF_PROFILE).child(REF_PHOTO);
        photoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String url = String.valueOf(dataSnapshot.getValue());
                Glide.with(getApplicationContext())
                        .asBitmap().load(url).into(circleImageView);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        //check for permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_RQ);
            } else {
                getLocation();
            }

        } else {
            getLocation();
        }


        //Start service for requests
        startService(new Intent(this, RequestService.class));

        trackingRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                String phone = dataSnapshot.getKey();
                getTrackingUsers(phone);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Marker marker = markers.get(dataSnapshot.getKey());
                if (marker != null) {
                    marker.remove();
                    markers.remove(dataSnapshot.getKey());
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void openGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, GALLERY_RQ);
    }

    private void setUserIcon(String phone, final Marker marker) {
        usersRef.child(phone).child(REF_PROFILE).child(REF_PHOTO)
            .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String url = String.valueOf(dataSnapshot.getValue());
                Glide.with(getApplicationContext()).asBitmap()
                        .load(url)
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {

                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                if(resource!=null){
                                    //  Bitmap circularBitmap = getRoundedCornerBitmap(bitmap, 150);
                                    Bitmap bitmap = Bitmap.createScaledBitmap(resource, 100, 100, false);
                                    final BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (marker != null) {
                                                marker.setIcon(icon);
                                            }
                                        }
                                    });
                                }
                                return false;
                            }
                        }).submit();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Get users in tracking list
    // add markers on map
    private void getTrackingUsers(final String phone) {
        usersRef.child(phone).child(REF_PROFILE).child(REF_LOCATION)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Timber.d(String.valueOf(dataSnapshot.getValue()));
                        String[] location = dataSnapshot.getValue(String.class).split(", ");
                        if (mMap != null) {
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.valueOf(location[0]), Double.valueOf(location[1]))));
                            marker.setTag(phone);
                            marker.setTitle(phone);

                            setUserIcon(phone, marker);
                            if (markers.containsKey(phone)) {
                                markers.get(phone).remove();
                            }
                            markers.put(phone, marker);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    // get the location of the device
    @SuppressLint("MissingPermission")
    private void getLocation() {
        Timber.d("getLocation");
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            // if gps not enabled, ask to open it
            if (!checkForGPS(locationManager)) askForGPS();

            // listen for location changes and update the node in firebase
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000L, 0f, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Timber.d(String.valueOf(location.getLatitude()));
                    if (mMap != null) {
                        setMapOptions(location);

                        profileRef.child(REF_LOCATION).setValue(location.getLatitude() + ", " + location.getLongitude());
                    }
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                    Timber.d("OnStatus: %s", s);
                }

                @Override
                public void onProviderEnabled(String s) {
                    Timber.d("OnProviderEnabled: %s", s);
                }

                @Override
                public void onProviderDisabled(String s) {
                    Timber.d("OnProviderDisabled: %s", s);
                }
            });
        }

    }

    @SuppressLint("MissingPermission")
    private void setMapOptions(Location location) {

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (first) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            first = false;
        }
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        if (marker != null) {
            marker.remove();
        }
        marker = mMap.addMarker(new MarkerOptions()
        .position(new LatLng(location.getLatitude(), location.getLongitude())));
        marker.setTag(user.getPhoneNumber());

        setUserIcon(user.getPhoneNumber(), marker);
    }

    private void askForGPS() {
        Timber.d("ask");
        new AlertDialog.Builder(this)
                .setMessage(R.string.gps_network_not_enabled)
                .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        }
                )
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean checkForGPS(LocationManager locationManager) {
        Timber.d("check");
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (!mMap.isMyLocationEnabled())
            mMap.setMyLocationEnabled(true);

        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
//                        fusedClient.requestLocationUpdates(request, new LocationCallback(){
//                            @Override
//                            public void onLocationAvailability(LocationAvailability locationAvailability) {
//                                if (!locationAvailability.isLocationAvailable()) {
//                                    // location not available
//                                    // gps may be unavailable
//                                }
//                            }
//
//                            @Override
//                            public void onLocationResult(LocationResult locationResult) {
//                                UserLocation location = locationResult.getLastLocation();
//                                Timber.d("location: %s", location.getLatitude());
//                                // : 09/08/19 add location to current user
//                            }
//                        }, getMainLooper());
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Timber.d("suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Timber.d("failed");
                    }
                })
                .build();
        client.connect();

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                showMarkerDialog(String.valueOf(marker.getTag()));
//                Toast.makeText(MyNavigationTutorial.this, (CharSequence) marker.getTag(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    // show options for call and delete
    private void showMarkerDialog(final String phone) {
        new AlertDialog.Builder(this)
                .setTitle(phone)
                .setMessage("What do you want?")
                .setPositiveButton("Call", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        callPhone(phone);
                    }
                })
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deletePhone(phone);
                    }
                })
                .create().show();
    }

    private void deletePhone(String phone) {
        trackingRef.child(phone).removeValue();
        usersRef.child(phone).child("tracking").child(user.getPhoneNumber()).removeValue();
    }

    private void callPhone(String phone) {
        this.phone = phone;
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phone));
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_RQ);
                return;
            }
        }
        startActivity(callIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Timber.d("code: %s", requestCode);
        if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            if(requestCode==LOCATION_RQ)
            {
                Toast.makeText(getApplicationContext(),"UserLocation permission granted. Thank you.",Toast.LENGTH_SHORT).show();
                getLocation();

            } else if (requestCode == CALL_RQ) {
                callPhone(phone);
            } else if (requestCode == CONTACTS_RQ) {
                contacts = new Contacts(this).getContactList();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_RQ && resultCode==RESULT_OK && data!=null)
        {
            Uri uri = data.getData();
            CropImage.activity(uri)
                    .start(this);
        }

        Timber.d("request2: %s", requestCode);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Timber.d("resultCode: %s", resultCode);
            Timber.d("result: %s", result.getUri());
            if (resultCode == RESULT_OK) {
                final Uri resultUri = result.getUri();

                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    bitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (bitmap != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                }
                byte[] bytes = baos.toByteArray();
                UploadTask uploadTask = storageReference.putBytes(bytes);

                circleImageView.setImageURI(null);
                circleImageView.setImageBitmap(bitmap);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return storageReference.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            Timber.d("urii: %s", downloadUri);
                            Toast.makeText(MyNavigationTutorial.this, "Photo updated successfully", Toast.LENGTH_SHORT).show();
                            FirebaseDatabase.getInstance().getReference()
                                    .child(REF_USERS)
                                    .child(user.getPhoneNumber())
                                    .child(REF_PROFILE)
                                    .child(REF_PHOTO)
                                    .setValue(downloadUri.toString());
                        }
                        else {
                            // Handle failures
                            // ...
                            Toast.makeText(MyNavigationTutorial.this, (CharSequence) task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Timber.d(error);
            }
        }
    }

    public void inviteMembers(View v)
    {
    	startActivity(new Intent(this, ContactsActivity.class)
        .putExtra("contacts", contacts));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_my_navigation_tutorial_drawer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.signout) {
            if (user != null) {
                auth.signOut();
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
