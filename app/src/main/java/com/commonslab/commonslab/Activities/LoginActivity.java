package com.commonslab.commonslab.Activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.commonslab.commonslab.Fragments.LoginFragment;
import com.commonslab.commonslab.Fragments.RegisterFragment;
import com.commonslab.commonslab.R;

public class LoginActivity extends AppCompatActivity {

    private boolean inLoginFragment = true; //true-> login screen ,,,, false -> Register screen
    private FrameLayout loadingScreen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loadingScreen = (FrameLayout) findViewById(R.id.progressBarHolder);

        setWindowFlags();
        loadLoginView();
    }

    private void setWindowFlags() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        this.getWindow().addFlags
                (WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setSoftInputMode
                (WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }


    private void loadLoginView() {
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        LoginFragment fragment = new LoginFragment();
        fragmentTransaction.add(R.id.login_container, fragment, "LoginFragment");
        fragmentTransaction.commit();
    }

    public void loadRegisterScreen() {
        Fragment fragment = new RegisterFragment();
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        fragmentTransaction.replace(R.id.login_container, fragment);
        fragmentTransaction.commit();

        inLoginFragment = false;
    }

    public void loadLoginScreen() {
        Fragment fragment = new LoginFragment();
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
        fragmentTransaction.replace(R.id.login_container, fragment, "LoginFragment");
        fragmentTransaction.commit();

        inLoginFragment = true;
    }

    public void toggleLoadingScreen() {
        if (loadingScreen.getVisibility() == View.GONE) {
            loadingScreen.setVisibility(View.VISIBLE);
            loadingScreen.bringToFront();
        } else
            loadingScreen.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (!inLoginFragment)
            loadLoginScreen();
        else
            super.onBackPressed();
    }

}
