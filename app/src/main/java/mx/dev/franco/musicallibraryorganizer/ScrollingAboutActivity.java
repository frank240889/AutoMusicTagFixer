package mx.dev.franco.musicallibraryorganizer;

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
        setContentView(R.layout.activity_scrolling_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openInExternalApp(Intent.ACTION_SENDTO, "mailto:support@dark-yellow-studios.com");
            }
        });

        TextView jaudiotagger = (TextView) findViewById(R.id.audiotagger);
        TextView glide = (TextView) findViewById(R.id.glide);
        TextView gnsdk = (TextView) findViewById(R.id.gnsdk);
        TextView drawer= (TextView) findViewById(R.id.drawer_image);
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.about) + " " + getString(R.string.app_name));
    }

    private void openInExternalApp(String action, String msg){
        Uri uri = Uri.parse(msg);
        Intent intent = new Intent(action, uri);
        startActivity(intent);
    }
}
