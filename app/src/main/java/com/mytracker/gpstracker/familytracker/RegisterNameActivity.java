package com.mytracker.gpstracker.familytracker;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.Date;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

import static com.mytracker.gpstracker.familytracker.model.Constants.REF_NAME;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_PHOTO;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_PROFILE;
import static com.mytracker.gpstracker.familytracker.model.Constants.REF_USERS;

public class RegisterNameActivity extends AppCompatActivity {

    Toolbar toolbar;
    EditText nameEt;
    CircleImageView circleImageView;
    Button nxtBtn;
    StorageReference storageReference;
    FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_name);
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("Your Profile");
        setSupportActionBar(toolbar);
        nameEt = (EditText)findViewById(R.id.name_et);
        nxtBtn = (Button)findViewById(R.id.nxt_button);

        user = FirebaseAuth.getInstance().getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference().child(user.getPhoneNumber());
        if (user == null) {
            finish();
        }
        nxtBtn.setEnabled(false);
        nxtBtn.setBackgroundColor(Color.parseColor("#faebd7"));
        circleImageView = (CircleImageView)findViewById(R.id.profile_image);

        final DatabaseReference userName =
                FirebaseDatabase.getInstance().getReference()
                        .child(REF_USERS)
                        .child(user.getPhoneNumber())
                        .child(REF_PROFILE)
                        .child(REF_NAME);

        nameEt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                user.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameEt.getText().toString())
                        .build());
                        userName.setValue(nameEt.getText().toString());

                startActivity(new Intent(RegisterNameActivity.this, MyNavigationTutorial.class));
            }
        });
        nameEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                    if(s.length()>0)
                    {
                        nxtBtn.setEnabled(true);
                        nxtBtn.setBackgroundColor(Color.parseColor("#9C27B0"));
                    }
                    else
                    {
                        nxtBtn.setEnabled(false);
                        nxtBtn.setBackgroundColor(Color.parseColor("#faebd7"));
                    }
            }
        });


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Timber.d("OnSave");
    }

    public void openGallery(View v)
    {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, 12);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 12 && resultCode==RESULT_OK && data!=null)
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


                user.updateProfile(new UserProfileChangeRequest.Builder().setPhotoUri(resultUri).build());

                UploadTask uploadTask = storageReference.putFile(resultUri);

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
                            Toast.makeText(RegisterNameActivity.this, "Photo updated successfully", Toast.LENGTH_SHORT).show();
                            circleImageView.setImageURI(null);
                            circleImageView.setImageURI(resultUri);
                            FirebaseDatabase.getInstance().getReference()
                                    .child(REF_USERS)
                                    .child(user.getPhoneNumber())
                                    .child(REF_PROFILE)
                                    .child(REF_PHOTO)
                                    .setValue(downloadUri.toString());
                        } else {
                            // Handle failures
                            // ...
                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Timber.d(error);
            }
        }
    }
}
