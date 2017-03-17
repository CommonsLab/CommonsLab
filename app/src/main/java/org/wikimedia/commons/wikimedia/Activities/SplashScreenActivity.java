package org.wikimedia.commons.wikimedia.Activities;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils.StorageUtil;

public class SplashScreenActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION = 500;

    private Handler handler;
    private Runnable runnable;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBar();
        setContentView(R.layout.activity_splash_screen);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                dismissSplash();
            }
        };
        // click to dismiss the splash screen prematurely
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissSplash();
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(runnable, SPLASH_DURATION);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    private void dismissSplash() {
        if (new StorageUtil(getApplicationContext()).validUserSession()) {
            startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class));
        }
        finish();
    }

    private void hideStatusBar() {
        // If the Android version is lower than Jellybean, use this call to hide
        // the status bar.
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        //Hide the Status Bar on Android 4.1 and Higher
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

}

