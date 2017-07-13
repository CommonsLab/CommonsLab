package com.commonslab.commonslab.Utils.CustomViews;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.commonslab.commonslab.Fragments.LoginFragment;

/**
 * Created by Valdio Veliu on 06/02/2017.
 */

public class CustomEditText extends android.support.v7.widget.AppCompatEditText {
    Context context;
    //Fragment that contains the EditText
    private static LoginFragment fragment;

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public static void setFragment(Fragment fragment) {
        CustomEditText.fragment = (LoginFragment) fragment;
    }


    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {

        if (fragment != null && keyCode == KeyEvent.KEYCODE_BACK) {
            // User has pressed Back key. So hide the keyboard
            InputMethodManager inputMethodManager = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
            // TODO: Hide your view as you do it in your activity

            if (fragment.getKeyboardStatus()) {
                //Keyboard active, close it
                fragment.resetView();//  reset view in the fragment onBackPressed with soft keyboard
                return true;// back action handled
            }
            return false;

        }

        return false;
    }
}