package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

public class authview extends Activity {

    String ids;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffauth);

        SharedPreferences prefs;

        prefs = getSharedPreferences("finger486", MODE_PRIVATE);

        ids = prefs.getString("name","");

        webloading();
    }

    private void webloading() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String real = "https://fidochallenge486.tk:8080/applogin/" + ids;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(real));
                startActivity(intent);
                finish();
            }
        }, 2000);
    }

}