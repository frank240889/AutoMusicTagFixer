package mx.dev.franco.automusictagfixer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class ScrollingAboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set layout of activity
        setContentView(R.layout.activity_scrolling_about);
        //Set an action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScrollingAboutActivity.super.onBackPressed();
            }
        });

        //Set UI elements
        FloatingActionButton fab = findViewById(R.id.fab);
        TextView jaudiotagger = findViewById(R.id.audiotagger);
        TextView glide = findViewById(R.id.glide);
        TextView gnsdk = findViewById(R.id.gnsdk);
        TextView drawer = findViewById(R.id.drawer_image);
        TextView crashlytics = findViewById(R.id.crashlytics_lib);
        TextView iconics = findViewById(R.id.iconics_lib);

        //Set listener for UI elements
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openInExternalApp(Intent.ACTION_SENDTO, "mailto: dark.yellow.studios@gmail.com");
            }
        });

        jaudiotagger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInExternalApp(Intent.ACTION_VIEW, "http://www.jthink.net/jaudiotagger/");
            }
        });

        glide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInExternalApp(Intent.ACTION_VIEW, "https://github.com/bumptech/glide");
            }
        });

        gnsdk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInExternalApp(Intent.ACTION_VIEW, "https://developer.gracenote.com/gnsdk");
            }
        });

        drawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInExternalApp(Intent.ACTION_VIEW, "https://tgs266.deviantart.com/");
            }
        });

        crashlytics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInExternalApp(Intent.ACTION_VIEW, "https://fabric.io/kits/android/crashlytics");
            }
        });
        iconics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInExternalApp(Intent.ACTION_VIEW, "https://github.com/mikepenz/Android-Iconics");
            }
        });
        //Get action bar from toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.about));
    }

    /**
     * Receives an action and let the system show you
     * the apps that can handle this action
     * @param action
     * @param msg
     */
    private void openInExternalApp(String action, String msg){
        Uri uri = Uri.parse(msg);
        Intent intent = new Intent(action, uri);
        startActivity(intent);
    }
}
