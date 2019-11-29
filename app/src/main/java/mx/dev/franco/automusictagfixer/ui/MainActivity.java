package mx.dev.franco.automusictagfixer.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.interfaces.LongRunningTaskListener;
import mx.dev.franco.automusictagfixer.interfaces.ProcessingListener;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.ui.about.AboutFragment;
import mx.dev.franco.automusictagfixer.ui.faq.QuestionsFragment;
import mx.dev.franco.automusictagfixer.ui.main.MainFragment;
import mx.dev.franco.automusictagfixer.ui.settings.SettingsActivity;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_BROADCAST_MESSAGE;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_COMPLETE_TASK;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_START_TASK;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.START_PROCESSING_FOR;

public class MainActivity extends AppCompatActivity implements ResponseReceiver.OnResponse,
        NavigationView.OnNavigationItemSelectedListener,
    HasSupportFragmentInjector {
    public static String TAG = MainActivity.class.getName();

    @Inject
    DispatchingAndroidInjector<Fragment> fragmentDispatchingAndroidInjector;
    // The receiver that handles the broadcasts from FixerTrackService
    private ResponseReceiver mReceiver;
    public DrawerLayout mDrawer;
    @Inject
    AbstractSharedPreferences mAbstractSharedPreferences;
    public MaterialToolbar mMainToolbar;
    public AppBarLayout mMainAppbar;
    public ActionBar mActionBar;
    public EditText mSearchBox;
    public ActionBarDrawerToggle toggle;
    MainFragment listFragment;
    AboutFragment aboutFragment;
    QuestionsFragment questionsFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        Window window = getWindow();
        window.requestFeature(SYSTEM_UI_FLAG_LAYOUT_STABLE|SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        //window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        super.onCreate(savedInstanceState);
        //windows is the top level in the view hierarchy,
        //it has a single Surface in which the contents of the window is rendered
        //A Surface is an object holding pixels that are being composited to the screen.
        setContentView(R.layout.activity_main);
        mSearchBox = findViewById(R.id.search_box);
        mDrawer = findViewById(R.id.drawer_layout);
        mMainToolbar = findViewById(R.id.main_toolbar);
        mMainAppbar = findViewById(R.id.main_app_bar);
        setupToolbar();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View view = navigationView.getHeaderView(0);

        MaterialButton toggleNightModeButton = view.findViewById(R.id.toggle_night_mode_button);

        if(mAbstractSharedPreferences.getBoolean("dark_mode")) {
            toggleNightModeButton.setIcon(getDrawable(R.drawable.ic_wb_sunny_24px));
            toggleNightModeButton.setText(R.string.turn_lights_on);
        }
        else {
            toggleNightModeButton.setIcon(getDrawable(R.drawable.ic_nights_stay_24px));
            toggleNightModeButton.setText(R.string.turn_lights_off);
        }

        toggleNightModeButton.setOnClickListener(v -> {
            if(mAbstractSharedPreferences.getBoolean("dark_mode")) {
                toggleNightModeButton.setIcon(getDrawable(R.drawable.ic_wb_sunny_24px));
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                mAbstractSharedPreferences.putBoolean("dark_mode", false);
            }
            else {
                toggleNightModeButton.setIcon(getDrawable(R.drawable.ic_nights_stay_24px));
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                mAbstractSharedPreferences.putBoolean("dark_mode", true);
            }
        });

        listFragment = (MainFragment) getSupportFragmentManager().
                findFragmentByTag(MainFragment.class.getName());

        if(listFragment == null)
            listFragment = MainFragment.newInstance();

        addFragment(listFragment);


        setupReceivers();
    }

    public void setupToolbar() {
        setSupportActionBar(mMainToolbar);
        mActionBar = getSupportActionBar();
        toggle = new ActionBarDrawerToggle(this, mDrawer,
                mMainToolbar,R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        //toggle.setDrawerIndicatorEnabled(true);
        //toggle.setDrawerSlideAnimationEnabled(true);
        mDrawer.post(() -> toggle.syncState());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }


    private void addFragment(BaseFragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setPrimaryNavigationFragment(fragment)
                .replace(R.id.container_fragments, fragment, fragment.getTagName())
                .setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
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
    //@Override
    public void onBackPressed() {
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
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        BaseFragment baseFragment = (BaseFragment) getSupportFragmentManager().getFragments().get(0);
        outState.putString("current_fragment", baseFragment.getTagName());
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
        int id = item.getItemId();
      mDrawer.closeDrawer(GravityCompat.START);
        Fragment topFragment = null;
        /*if (id == R.id.rate) {
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
        else*/
        if(id == R.id.audio_files_list) {
            listFragment = (MainFragment) getSupportFragmentManager().
                    findFragmentByTag(MainFragment.class.getName());
            if(listFragment == null)
                listFragment = MainFragment.newInstance();

            getSupportFragmentManager().beginTransaction().
                replace(R.id.container_fragments,
                    listFragment, MainFragment.class.getName())
                .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                .commit();
        }
        else if(id == R.id.settings){
            //configure app settings
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.faq){
            questionsFragment = (QuestionsFragment) getSupportFragmentManager().
                    findFragmentByTag(QuestionsFragment.class.getName());
            if(questionsFragment == null)
                questionsFragment = QuestionsFragment.newInstance();
            //if(getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName()) != null) {
                getSupportFragmentManager().beginTransaction().
                    setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                    .replace(R.id.container_fragments,
                        questionsFragment, QuestionsFragment.class.getName()).
                    commit();
            //}
            /*else {
                getSupportFragmentManager().beginTransaction().
                    setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                    .add(R.id.container_fragments,
                        questionsFragment, QuestionsFragment.class.getName()).
                    commit();
            }*/
        }
        else if(id == R.id.about){
            aboutFragment = (AboutFragment) getSupportFragmentManager().
                    findFragmentByTag(AboutFragment.class.getName());
            if(aboutFragment == null)
                aboutFragment = AboutFragment.newInstance();
            //if(getSupportFragmentManager().getFragments().size() > 1) {
                getSupportFragmentManager().beginTransaction().
                    setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                    .replace(R.id.container_fragments,
                        aboutFragment, AboutFragment.class.getName()).
                    commit();
            //}
            /*else {
                getSupportFragmentManager().beginTransaction().
                    setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                    .add(R.id.container_fragments,
                        aboutFragment, AboutFragment.class.getName()).
                    commit();
            }*/

        }


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
        BaseViewModelFragment baseViewModelFragment = (BaseViewModelFragment) getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
        switch (action) {
            case ACTION_START_TASK:
                ((LongRunningTaskListener) baseViewModelFragment).onLongRunningTaskStarted();
                break;
            case START_PROCESSING_FOR:
                ((ProcessingListener) baseViewModelFragment).onStartProcessingFor(id);
                break;
            case ACTION_COMPLETE_TASK:
                ((LongRunningTaskListener) baseViewModelFragment).onLongRunningTaskFinish();
                    /*getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME,
                        Context.MODE_PRIVATE).edit().putBoolean(Constants.ALL_ITEMS_CHECKED, false).
                            apply();*/
                break;
            case ACTION_BROADCAST_MESSAGE:
                String message = intent.getStringExtra("message");
                if(message != null){
                    ((LongRunningTaskListener) baseViewModelFragment).onLongRunningTaskMessage(message);
                }
                break;
        }

    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentDispatchingAndroidInjector;
    }
}
