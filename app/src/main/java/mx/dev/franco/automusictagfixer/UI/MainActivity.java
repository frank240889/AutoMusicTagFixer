package mx.dev.franco.automusictagfixer.UI;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.about.ScrollingAboutActivity;
import mx.dev.franco.automusictagfixer.UI.faq.QuestionsActivity;
import mx.dev.franco.automusictagfixer.UI.main.ListFragment;
import mx.dev.franco.automusictagfixer.UI.settings.SettingsActivity;
import mx.dev.franco.automusictagfixer.interfaces.LongRunningTaskListener;
import mx.dev.franco.automusictagfixer.interfaces.ProcessingListener;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.Constants;

import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_BROADCAST_MESSAGE;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_COMPLETE_TASK;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_START_TASK;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.START_PROCESSING_FOR;

public class MainActivity extends AppCompatActivity implements ResponseReceiver.OnResponse,
        NavigationView.OnNavigationItemSelectedListener,
        BaseFragment.OnConfirmBackPressedListener {
    public static String TAG = MainActivity.class.getName();

    // The receiver that handles the broadcasts from FixerTrackService
    private ResponseReceiver mReceiver;
    public DrawerLayout mDrawer;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //windows is the top level in the view hierarchy,
        //it has a single Surface in which the contents of the window is rendered
        //A Surface is an object holding pixels that are being composited to the screen.
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.activity_main);

        setupReceivers();

        mDrawer = findViewById(R.id.drawer_layout);


        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ListFragment listFragment = ListFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.container_fragments,
                listFragment, ListFragment.class.getName())
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        mReceiver.clearReceiver();
        mReceiver = null;
    }

    /**
     * Handle the onBackPressed callback for fragments.
     */
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0 ){
            int topFragmentIndex = getSupportFragmentManager().getBackStackEntryCount() - 1;
            FragmentManager.BackStackEntry backStackEntry = getSupportFragmentManager().
                    getBackStackEntryAt(topFragmentIndex);
            String backStackEntryName = backStackEntry.getName();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(backStackEntryName);
            if(fragment instanceof BaseFragment){
                ((BaseFragment) fragment).onBackPressed();
            }
            else {
                super.onBackPressed();
            }
        }

        else {
            callSuperOnBackPressed();
        }
    }

    /**
     * Handle the onBackPressed for this activity.
     */
    @Override
    public void callSuperOnBackPressed() {
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }


    /**
     * Allows to register filters to handle
     * only certain actions sent by FixerTrackService
     */
    private void setupReceivers() {
        IntentFilter startTaskFilter = new IntentFilter(ACTION_START_TASK);
        IntentFilter completedTaskFilter = new IntentFilter(ACTION_COMPLETE_TASK);
        IntentFilter startProcessingForFilter = new IntentFilter(START_PROCESSING_FOR);
        IntentFilter broadcastMessageFilter = new IntentFilter(ACTION_BROADCAST_MESSAGE);

        mReceiver = new ResponseReceiver(this, new Handler());

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        localBroadcastManager.registerReceiver(mReceiver, completedTaskFilter);
        localBroadcastManager.registerReceiver(mReceiver, startProcessingForFilter);
        localBroadcastManager.registerReceiver(mReceiver, startTaskFilter);
        localBroadcastManager.registerReceiver(mReceiver, broadcastMessageFilter);

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation mSwipeRefreshLayout item_list clicks here.

        int id = item.getItemId();

        if (id == R.id.rate) {
            rateApp();
        } else if (id == R.id.share) {
            String shareSubText = getString(R.string.app_name) + " " + getString(R.string.share_message);
            String shareBodyText = getPlayStoreLink();

            Intent shareIntent = ShareCompat.IntentBuilder.from(this).
                    setType("text/plain").
                    setText(shareSubText +"\n"+ shareBodyText).getIntent();
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(shareIntent);
        }
        else if(id == R.id.settings){
            //configure app settings
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.faq){
            Intent intent = new Intent(this,QuestionsActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.about){
            Intent intent = new Intent(this,ScrollingAboutActivity.class);
            startActivity(intent);
        }

        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private String getPlayStoreLink(){
        return Constants.PLAY_STORE_URL + getApplicationContext().getPackageName();
    }

    private void rateApp(){
        String packageName = getApplicationContext().getPackageName();
        Uri uri = Uri.parse("market://details?id=" + packageName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, after pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(Constants.PLAY_STORE_URL + packageName)));
        }
    }

    /**
     * Handles responses from {@link FixerTrackService}
     * @param intent
     */
    @Override
    public void onResponse(Intent intent) {

        //get action and handle it
        String action = intent.getAction();
        int id = intent.getIntExtra(Constants.MEDIA_STORE_ID, -1);
        BaseFragment baseFragment = (BaseFragment) getSupportFragmentManager().findFragmentByTag(ListFragment.class.getName());
        switch (action) {
            case ACTION_START_TASK:
                ((LongRunningTaskListener) baseFragment).onLongRunningTaskStarted();
                break;
            case START_PROCESSING_FOR:
                ((ProcessingListener) baseFragment).onStartProcessingFor(id);
                break;
            case ACTION_COMPLETE_TASK:
                ((LongRunningTaskListener)baseFragment).onLongRunningTaskFinish();
                    /*getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME,
                        Context.MODE_PRIVATE).edit().putBoolean(Constants.ALL_ITEMS_CHECKED, false).
                            apply();*/
                break;
            case ACTION_BROADCAST_MESSAGE:
                String message = intent.getStringExtra("message");
                if(message != null){
                    ((LongRunningTaskListener) baseFragment).onLongRunningTaskMessage(message);
                }
                break;
        }

    }
}
