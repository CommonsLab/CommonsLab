package org.wikimedia.commons.wikimedia.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.wikimedia.commons.wikimedia.Activities.LoginActivity;
import org.wikimedia.commons.wikimedia.Activities.MainActivity;
import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.CustomViews.CustomEditText;
import org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils.StorageUtil;

import apiwrapper.commons.wikimedia.org.Commons;
import apiwrapper.commons.wikimedia.org.Enums.CookieStatus;
import apiwrapper.commons.wikimedia.org.Interfaces.LoginCallback;


public class LoginFragment extends Fragment {

    private ImageView login_drawable;
    private CustomEditText login_username;
    private CustomEditText login_password;
    public Button login_button;
    private LinearLayout linear_form_container;
    //    private TextView login_forgot_password;
    private Button to_register_screen;

    private Commons commons;

    private boolean keyboardActive = false;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        commons = new Commons(getActivity().getApplicationContext(), CookieStatus.ENABLED);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewSetup();
    }


    private void viewSetup() {
        linear_form_container = (LinearLayout) getActivity().findViewById(R.id.linear_form_container);
        login_drawable = (ImageView) getActivity().findViewById(R.id.login_drawable);
        login_username = (CustomEditText) getActivity().findViewById(R.id.login_username);
        login_password = (CustomEditText) getActivity().findViewById(R.id.password_login);
        login_username.setFragment(LoginFragment.this);// setup custom EditTexts
        login_password.setFragment(LoginFragment.this);// setup custom EditTexts

        login_button = (Button) getActivity().findViewById(R.id.login_button);
//        login_forgot_password = (TextView) getActivity().findViewById(R.id.login_forgot_password);
        to_register_screen = (Button) getActivity().findViewById(R.id.to_register_screen);


        login_username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                keyboardActive = true;
                if (keyboardActive(getActivity())) {
                    login_drawable.setVisibility(View.GONE);
                } else
                    login_drawable.setVisibility(View.VISIBLE);

            }
        });
        login_username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keyboardActive = true;

                if (keyboardActive(getActivity())) {
                    login_drawable.setVisibility(View.GONE);
                } else
                    login_drawable.setVisibility(View.VISIBLE);

            }
        });
        login_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keyboardActive = true;

                if (keyboardActive(getActivity())) {
                    login_drawable.setVisibility(View.GONE);
                } else
                    login_drawable.setVisibility(View.VISIBLE);

            }
        });

        login_password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                keyboardActive = true;
                if (keyboardActive(getActivity())) {
                    login_drawable.setVisibility(View.GONE);
                } else
                    login_drawable.setVisibility(View.VISIBLE);

            }
        });

        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keyboardActive = true;
                closeKeyboard(getActivity(), login_button.getWindowToken());
                attemptLogin();
            }
        });

        to_register_screen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((LoginActivity) getActivity()).loadRegisterScreen();
            }
        });


        setFonts();
    }


    public void resetView() {
        keyboardActive = false;
        login_drawable.setVisibility(View.VISIBLE);
    }

    public boolean getKeyboardStatus() {
        return keyboardActive;
    }

    private void setFonts() {
        Typeface type = Typeface.createFromAsset(getActivity().getAssets(), "fonts/open_sans/OpenSans-Regular.ttf");
        Typeface typeOpenSansLight = Typeface.createFromAsset(getActivity().getAssets(), "fonts/open_sans/OpenSans-Light.ttf");
        login_username.setTypeface(type);
        login_password.setTypeface(type);
        login_button.setTypeface(typeOpenSansLight);
//        login_forgot_password.setTypeface(type);
        to_register_screen.setTypeface(type);
    }

    private void attemptLogin() {
        final String username = login_username.getText().toString();
        String password = login_password.getText().toString();

        if (username == null || username.equals("")) {
            login_username.requestFocus();
            login_username.setError(getString(R.string.field_required));
        } else if (password == null || password.equals("")) {
            login_password.requestFocus();
            login_password.setError(getString(R.string.field_required));
        } else {
            ((LoginActivity) getActivity()).toggleLoadingScreen();
            commons.userLogin(username, password, new LoginCallback() {
                @Override
                public void onLoginSuccessful() {
                    storeUserCredentials(username);
                    showToastMessage("Welcome Back :)");
                    getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
                }

                @Override
                public void onFailure() {
                    ((LoginActivity) getActivity()).toggleLoadingScreen();
                    showToastMessage("Incorrect username or password");
                }
            });
        }
    }

    private void showToastMessage(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private void storeUserCredentials(String username) {
        StorageUtil storage = new StorageUtil(getActivity());
        storage.storeUserCredentials(username);
    }


    private void closeKeyboard(Context context, IBinder windowToken) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0);

    }

    private boolean keyboardActive(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        return inputMethodManager.isAcceptingText();
    }
}

