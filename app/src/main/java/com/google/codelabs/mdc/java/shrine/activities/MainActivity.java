package com.google.codelabs.mdc.java.shrine.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.codelabs.mdc.java.shrine.fragments.LoginFragment;
import com.google.codelabs.mdc.java.shrine.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shr_main_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new LoginFragment())
                    .commit();
        }
    }

    public void gotoGoogleMap(){
        Intent myIntent = new Intent(MainActivity.this, MapsActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void gotoDriverActivity(){
        Intent myIntent = new Intent(MainActivity.this, DriverActivity.class);
        MainActivity.this.startActivity(myIntent);
    }
}
