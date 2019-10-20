package mx.dev.franco.automusictagfixer.ui.about;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class ScrollingAboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set layout of activity
        setContentView(R.layout.activity_scrolling_about);
        //Set an action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> ScrollingAboutActivity.super.onBackPressed());

        //Set UI elements
        FloatingActionButton fab = findViewById(R.id.fab);
        TextView jaudiotagger = findViewById(R.id.audiotagger);
        TextView drawer = findViewById(R.id.drawer_image);

        //Set listener for UI elements
        fab.setOnClickListener(view -> AndroidUtils.openInExternalApp(Intent.ACTION_SENDTO, "mailto: dark.yellow.studios@gmail.com", ScrollingAboutActivity.this));

        jaudiotagger.setOnClickListener(v -> AndroidUtils.openInExternalApp(Intent.ACTION_VIEW, "http://www.jthink.net/jaudiotagger/", ScrollingAboutActivity.this));

        drawer.setOnClickListener(v -> AndroidUtils.openInExternalApp(Intent.ACTION_VIEW, "https://tgs266.deviantart.com/", ScrollingAboutActivity.this));

        //Get action bar from toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.about));
    }
}
