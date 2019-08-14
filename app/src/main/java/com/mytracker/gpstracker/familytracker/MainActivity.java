package com.mytracker.gpstracker.familytracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mytracker.gpstracker.familytracker.view.VerificationActivity;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();

        user = auth.getCurrentUser();

        if(user != null){
            if (user.getDisplayName() == null) {
                startActivity(new Intent(this, RegisterNameActivity.class));
                finish();
            } else {
                Intent myIntent = new Intent(MainActivity.this,MyNavigationTutorial.class);
                startActivity(myIntent);
                finish();
            }
        }
    }

    public void getStarted_click(View v)
    {

        Intent myintent = new Intent(MainActivity.this, VerificationActivity.class);
        startActivity(myintent);
        finish();

    }
}
