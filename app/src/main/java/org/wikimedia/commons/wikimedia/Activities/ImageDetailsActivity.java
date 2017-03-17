package org.wikimedia.commons.wikimedia.Activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.wikimedia.commons.wikimedia.Fragments.MODFragment;
import org.wikimedia.commons.wikimedia.R;

public class ImageDetailsActivity extends AppCompatActivity {
    private ImageButton exitImageButton;
    private String imageURL;
    private String imageTitle;

    private ImageView imageView;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image_details);
        getSupportActionBar().hide();
        setupToolbar();


        if (savedInstanceState != null) {
            imageURL = savedInstanceState.getString("ImageURL");
            imageTitle = savedInstanceState.getString("ImageTitle", "");

        } else {
            imageURL = getIntent().getExtras().getString("ImageURL");
            imageTitle = getIntent().getExtras().getString("ImageTitle");
        }

        titleTextView = (TextView) findViewById(R.id.image_details_title);
        if (imageTitle != null && imageTitle.startsWith("File:"))
            imageTitle = imageTitle.substring("File:".length());
        if (imageTitle != null) {
            titleTextView.setText(imageTitle);
            titleTextView.setVisibility(View.VISIBLE);
        }
        imageView = (ImageView) findViewById(R.id.image_details_ImageView);
        if (imageURL.endsWith("gif"))
            Glide.with(ImageDetailsActivity.this)
                    .load(imageURL)
                    .asGif()
                    .into(imageView);
        else
            Glide.with(ImageDetailsActivity.this)
                    .load(imageURL)
                    .into(imageView);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("ImageURL", imageURL);
        if (imageTitle != null)
            outState.putString("ImageTitle", imageTitle);
    }


    private void setupToolbar() {
        exitImageButton = (ImageButton) findViewById(R.id.image_details_exit);
        exitImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
}
