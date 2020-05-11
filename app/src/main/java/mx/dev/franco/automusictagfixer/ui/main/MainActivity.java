package mx.dev.franco.automusictagfixer.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.interfaces.AutomaticTaskListener;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.ui.BaseFragment;
import mx.dev.franco.automusictagfixer.ui.BaseViewModelFragment;
import mx.dev.franco.automusictagfixer.ui.about.AboutFragment;
import mx.dev.franco.automusictagfixer.ui.faq.QuestionsFragment;
import mx.dev.franco.automusictagfixer.ui.settings.ConfigurationActivity;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static mx.dev.franco.automusictagfixer.AutoMusicTagFixer.DARK_MODE;
import static mx.dev.franco.automusictagfixer.utilities.Constants.ACTION_OPEN_MAIN_ACTIVITY;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_BROADCAST_MESSAGE;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_COMPLETE_TASK;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.ACTION_START_TASK;
import static mx.dev.franco.automusictagfixer.utilities.Constants.Actions.START_PROCESSING_FOR;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
    HasSupportFragmentInjector {
    public static String TAG = MainActivity.class.getName();

    @Inject
    DispatchingAndroidInjector<Fragment> mFragmentDispatchingAndroidInjector;
    public DrawerLayout mDrawerLayout;
    @Inject
    AbstractSharedPreferences mAbstractSharedPreferences;
    public MaterialToolbar mainToolbar;
    public AppBarLayout mainAppbar;
    public ActionBar actionBar;
    public EditText searchBox;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    public ExtendedFloatingActionButton startTaskFab;
    private MaterialButton mToggleNightModeButton;
    MainFragment mListFragment;
    AboutFragment mAboutFragment;
    QuestionsFragment mQuestionsFragment;
    // The receiver that handles the broadcasts from FixerTrackService
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        Window window = getWindow();
        window.requestFeature(SYSTEM_UI_FLAG_LAYOUT_STABLE|SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        super.onCreate(savedInstanceState);
        //windows is the top level in the view hierarchy,
        //it has a single Surface in which the contents of the window is rendered
        //A Surface is an object holding pixels that are being composited to the screen.
        setContentView(R.layout.activity_main);
        searchBox = findViewById(R.id.search_box);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mainToolbar = findViewById(R.id.main_toolbar);
        mainAppbar = findViewById(R.id.main_app_bar);


        startTaskFab = findViewById(R.id.fab_start_stop);
        setSupportActionBar(mainToolbar);
        actionBar = getSupportActionBar();
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mainToolbar,R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.setDrawerSlideAnimationEnabled(true);
        actionBarDrawerToggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View view = navigationView.getHeaderView(0);

        mToggleNightModeButton = view.findViewById(R.id.toggle_night_mode_button);

        mToggleNightModeButton.setOnClickListener(v -> {
            if(mAbstractSharedPreferences.getBoolean(DARK_MODE)) {
                mToggleNightModeButton.setIcon(getDrawable(R.drawable.ic_wb_sunny_24px));
                mAbstractSharedPreferences.putBoolean(DARK_MODE, false);
                mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}
                    @Override
                    public void onDrawerOpened(@NonNull View drawerView) {}
                    @Override
                    public void onDrawerStateChanged(int newState) {}
                    @Override
                    public void onDrawerClosed(@NonNull View drawerView) {
                        mDrawerLayout.removeDrawerListener(this);
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                });
                mDrawerLayout.closeDrawers();
            }
            else {
                mToggleNightModeButton.setIcon(getDrawable(R.drawable.ic_nights_stay_24px));
                mAbstractSharedPreferences.putBoolean(DARK_MODE, true);
                mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}
                    @Override
                    public void onDrawerOpened(@NonNull View drawerView) {}
                    @Override
                    public void onDrawerStateChanged(int newState) {}
                    @Override
                    public void onDrawerClosed(@NonNull View drawerView) {
                        mDrawerLayout.removeDrawerListener(this);
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    }
                });
                mDrawerLayout.closeDrawers();
            }
        });

        mListFragment = (MainFragment) getSupportFragmentManager().
                findFragmentByTag(MainFragment.class.getName());

        if(mListFragment == null)
            mListFragment = MainFragment.newInstance();

        getSupportFragmentManager().beginTransaction()
                .setPrimaryNavigationFragment(mListFragment)
                .replace(R.id.container_fragments, mListFragment, mListFragment.getTagName())
                .setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
                .commit();


        setupReceivers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        if(nightMode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY ||
                nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {

            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                mToggleNightModeButton.setIcon(getDrawable(R.drawable.ic_wb_sunny_24px));
                mToggleNightModeButton.setText(R.string.turn_lights_on);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            else {
                mToggleNightModeButton.setIcon(getDrawable(R.drawable.ic_dark_mode_24dp));
                mToggleNightModeButton.setText(R.string.turn_lights_off);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        }
        else {
            if(mAbstractSharedPreferences.getBoolean(DARK_MODE)) {
                mToggleNightModeButton.setIcon(getDrawable(R.drawable.ic_wb_sunny_24px));
                mToggleNightModeButton.setText(R.string.turn_lights_on);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            else {
                mToggleNightModeButton.setIcon(getDrawable(R.drawable.ic_dark_mode_24dp));
                mToggleNightModeButton.setText(R.string.turn_lights_off);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        mReceiver = null;
    }

    /**
     * Handle the onBackPressed callback for fragments.
     */
    //@Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
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
        IntentFilter broadcastOpenMainActivity = new IntentFilter(ACTION_OPEN_MAIN_ACTIVITY);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                processIntent(intent);
            }
        };

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        localBroadcastManager.registerReceiver(mReceiver, completedTaskFilter);
        localBroadcastManager.registerReceiver(mReceiver, startProcessingForFilter);
        localBroadcastManager.registerReceiver(mReceiver, startTaskFilter);
        localBroadcastManager.registerReceiver(mReceiver, broadcastMessageFilter);
        localBroadcastManager.registerReceiver(mReceiver, broadcastOpenMainActivity);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        mDrawerLayout.closeDrawer(GravityCompat.START);
        if(id == R.id.nav_audio_files_list) {
            mListFragment = (MainFragment) getSupportFragmentManager().
                    findFragmentByTag(MainFragment.class.getName());
            if(mListFragment == null)
                mListFragment = MainFragment.newInstance();
            mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

                }

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {

                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container_fragments,
                                    mListFragment, MainFragment.class.getName())
                            .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                            .commit();

                    mDrawerLayout.removeDrawerListener(this);
                }

                @Override
                public void onDrawerStateChanged(int newState) {

                }
            });
        }
        else if(id == R.id.nav_settings){
            mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

                }

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {

                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    mDrawerLayout.removeDrawerListener(this);
                    Intent intent = new Intent(MainActivity.this, ConfigurationActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onDrawerStateChanged(int newState) {

                }
            });
        }
        else if(id == R.id.nav_faq){
            mQuestionsFragment = (QuestionsFragment) getSupportFragmentManager().
                    findFragmentByTag(QuestionsFragment.class.getName());
            if(mQuestionsFragment == null)
                mQuestionsFragment = QuestionsFragment.newInstance();
            mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

                }

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {

                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                            .replace(R.id.container_fragments,
                                    mQuestionsFragment, QuestionsFragment.class.getName()).
                            commit();

                    mDrawerLayout.removeDrawerListener(this);
                }

                @Override
                public void onDrawerStateChanged(int newState) {

                }
            });
        }
        else if(id == R.id.nav_about){
            mAboutFragment = (AboutFragment) getSupportFragmentManager().
                    findFragmentByTag(AboutFragment.class.getName());
            if(mAboutFragment == null)
                mAboutFragment = AboutFragment.newInstance();

            mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

                }

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {

                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                            .replace(R.id.container_fragments,
                                    mAboutFragment, AboutFragment.class.getName()).
                            commit();
                    mDrawerLayout.removeDrawerListener(this);
                }

                @Override
                public void onDrawerStateChanged(int newState) {

                }
            });
        }


        return true;
    }

    /**
     * Handles responses from {@link FixerTrackService}
     * @param intent
     */
    private void processIntent(Intent intent) {
        //get action and handle it
        String action = intent.getAction();
        int id = intent.getIntExtra(Constants.MEDIA_STORE_ID, -1);
        BaseViewModelFragment baseViewModelFragment = (BaseViewModelFragment) getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
        if (baseViewModelFragment instanceof MainFragment) {
            switch (action) {
                case ACTION_START_TASK:
                    ((AutomaticTaskListener) baseViewModelFragment).onStartAutomaticTask();
                    break;
                case START_PROCESSING_FOR:
                    ((AutomaticTaskListener) baseViewModelFragment).onStartProcessingFor(id);
                    break;
                case ACTION_COMPLETE_TASK:
                    ((AutomaticTaskListener) baseViewModelFragment).onFinishedAutomaticTask();
                    break;
                case ACTION_BROADCAST_MESSAGE:
                    String message = intent.getStringExtra("message");
                    if(message != null){
                        ((AutomaticTaskListener.MessageListener) baseViewModelFragment).onIncomingMessageListener(message);
                    }
                    break;
                case ACTION_OPEN_MAIN_ACTIVITY:
                    Toast.makeText(this, action, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return mFragmentDispatchingAndroidInjector;
    }
}
