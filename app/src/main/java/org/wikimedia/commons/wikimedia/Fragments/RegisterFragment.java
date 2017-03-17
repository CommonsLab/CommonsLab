package org.wikimedia.commons.wikimedia.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.wikimedia.commons.wikimedia.Activities.LoginActivity;
import org.wikimedia.commons.wikimedia.Activities.MainActivity;
import org.wikimedia.commons.wikimedia.R;
import org.wikimedia.commons.wikimedia.Utils.SharedPreferencesUtils.StorageUtil;

import apiwrapper.commons.wikimedia.org.Commons;
import apiwrapper.commons.wikimedia.org.Enums.CookieStatus;
import apiwrapper.commons.wikimedia.org.Interfaces.CaptchaCallback;
import apiwrapper.commons.wikimedia.org.Interfaces.CreateAccountCallback;
import apiwrapper.commons.wikimedia.org.Models.Captcha;


public class RegisterFragment extends Fragment {
    private ScrollView scrollView;
    private ImageView toolbar;

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText retypePasswordEditText;
    private EditText emailEditText;
    private ImageView register_captchaImageView;
    private EditText captchaWordEditText;

    private Button register_button;
    private Commons commons;
    private Captcha registerCaptcha;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        commons = new Commons(getActivity().getApplicationContext(), CookieStatus.ENABLED);

        initToolbar(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupView();
        loadCaptcha();
    }

    private void loadCaptcha() {
        commons.getCaptcha(new CaptchaCallback() {
            @Override
            public void onCaptchaReceived(Captcha captcha) {
                registerCaptcha = captcha;
                Glide.with(getActivity())
                        .load(registerCaptcha.getCaptchaURL())
                        .into(register_captchaImageView);
            }

            @Override
            public void onFailure() {
                loadCaptcha();
            }
        });
    }

    private void initToolbar(View view) {
        toolbar = (ImageView) view.findViewById(R.id.register_toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((LoginActivity) getActivity()).loadLoginScreen();
            }
        });
//        toolbar.setNavigationIcon(R.drawable.ic_keyboard_back);
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ((LoginActivity) getActivity()).loadLoginScreen();
//            }
//        });
    }

    private void setupView() {
        usernameEditText = (EditText) getActivity().findViewById(R.id.register_username);
        passwordEditText = (EditText) getActivity().findViewById(R.id.password_register);
        retypePasswordEditText = (EditText) getActivity().findViewById(R.id.password_register_retype);
        emailEditText = (EditText) getActivity().findViewById(R.id.register_email);
        register_captchaImageView = (ImageView) getActivity().findViewById(R.id.register_captchaImageView);
        scrollView = (ScrollView) getActivity().findViewById(R.id.register_scrollView);
        captchaWordEditText = (EditText) getActivity().findViewById(R.id.register_captchaWord);

        register_button = (Button) getActivity().findViewById(R.id.register_button);
        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeKeyboard(getActivity(), register_button.getWindowToken());
                createAccount();
            }
        });

        setFonts();
    }

    private void setFonts() {
        Typeface type = Typeface.createFromAsset(getActivity().getAssets(), "fonts/open_sans/OpenSans-Regular.ttf");
        usernameEditText.setTypeface(type);
        passwordEditText.setTypeface(type);
        retypePasswordEditText.setTypeface(type);
        emailEditText.setTypeface(type);
        captchaWordEditText.setTypeface(type);
        register_button.setTypeface(type);
    }


    private void createAccount() {
        final String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String retypePassword = retypePasswordEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String captchaWord = captchaWordEditText.getText().toString();


        if (username == null || username.equals("")) {
            usernameEditText.requestFocus();
            usernameEditText.setError(getString(R.string.field_required));
        } else if (password == null || password.equals("")) {
            passwordEditText.requestFocus();
            passwordEditText.setError(getString(R.string.field_required));
        } else if (retypePassword == null || retypePassword.equals("")) {
            retypePasswordEditText.requestFocus();
            retypePasswordEditText.setError(getString(R.string.field_required));
        } else if (email == null || email.equals("")) {
            emailEditText.requestFocus();
            emailEditText.setError(getString(R.string.field_required));
        } else if (!email.contains("@")) {
            emailEditText.requestFocus();
            emailEditText.setError(getString(R.string.email_error));
        } else if (captchaWord == null || captchaWord.equals("")) {
            captchaWordEditText.requestFocus();
            captchaWordEditText.setError(getString(R.string.field_required));
        } else if (!password.equals(retypePassword)) {
            showToastMessage("Passwords do not match");
        } else if (password.length() < 6) {
            showToastMessage("Password to short \nMinimal 6 characters needed");
        } else {
            ((LoginActivity) getActivity()).toggleLoadingScreen();
            commons.createAccount(username, password, retypePassword, email, captchaWord, registerCaptcha.getCaptchaId(),
                    new CreateAccountCallback() {
                        @Override
                        public void onAccountCreatedSuccessful() {
                            storeUserCredentials(username);
                            showToastMessage("Account created successfully \nPlease confirm your account");
                            getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
                        }

                        @Override
                        public void onFailure(String s) {
                            ((LoginActivity) getActivity()).toggleLoadingScreen();
                            captchaWordEditText.setText("");
                            showToastMessage(s);
                            loadCaptcha();// load new captcha
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
}
