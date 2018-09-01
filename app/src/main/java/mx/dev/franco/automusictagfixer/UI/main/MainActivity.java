package mx.dev.franco.automusictagfixer.UI.main;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.transition.Fade;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.Toast;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.about.ScrollingAboutActivity;
import mx.dev.franco.automusictagfixer.UI.faq.QuestionsActivity;
import mx.dev.franco.automusictagfixer.UI.settings.SettingsActivity;
import mx.dev.franco.automusictagfixer.datasource.TrackAdapter;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.room.database.TrackContract;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.services.ServiceHelper;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;

public class MainActivity extends AppCompatActivity
        implements ResponseReceiver.OnResponse, ListFragment.OnInteractionFragment, NavigationView.OnNavigationItemSelectedListener{
    public static String TAG = MainActivity.class.getName();

    //the receiver that handles the intents from FixerTrackService
    private ResponseReceiver mReceiver;

    private ListFragment mListFragment;

    private Toolbar mToolbar;
    private ActionBar mActionBar;
    public FloatingActionButton mStartTaskFab;
    public FloatingActionButton mStopTaskFab;
    private Menu mMenu;
    private DrawerLayout mDrawer;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //windows is the top level in the view hierarchy,
        //it has a single Surface in which the contents of the window is rendered
        //A Surface is an object holding pixels that are being composited to the screen.
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.activity_main);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mStartTaskFab = findViewById(R.id.fab_start);
        mStopTaskFab = findViewById(R.id.fab_stop);
        mStartTaskFab.hide();
        mStopTaskFab.hide();

        setupReceivers();

        mListFragment = ListFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mListFragment)
                .commit();

        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Log.d(TAG,"onCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);

    }


    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG,"onPause");
    }


    @Override
    public void onStop() {
        Log.d(TAG,"onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy");
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        mReceiver.clearReceiver();
        mReceiver = null;
        if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("key_background_service", true)){
            Intent intent = new Intent(this,FixerTrackService.class);
            stopService(intent);
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int x = v.getRight();
                int y = v.getBottom();
                int endRadius = mToolbar.getWidth()*2;

                Animator animator = AndroidUtils.createRevealWithDelay(searchView, x, y, 0, endRadius);
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animator.start();

            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                int x = mToolbar.getRight();
                int y = mToolbar.getBottom();
                int startRadius = mToolbar.getWidth()*2;

                Animator animator = AndroidUtils.createRevealWithDelay(searchView, x, y, startRadius, 0);
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        searchView.setIconified(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        searchView.setIconified(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animator.start();

                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mMenu = menu;
        checkItem(-1);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        boolean sorted = false;
        mListFragment.stopScroll();
        switch (id){
            case R.id.action_select_all:
                if(mListFragment.getDatasource() == null || mListFragment.getDatasource().size() == 0){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                mListFragment.checkAll();
                break;
            case R.id.action_refresh:
                boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if(!hasPermission) {
                    mListFragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
                }
                else if(ServiceHelper.getInstance(getApplicationContext()).checkIfServiceIsRunning(FixerTrackService.class.getName())){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                }
                else {
                    mListFragment.updateList();
                }
                break;

            case R.id.path_asc:
                sorted = mListFragment.sort(TrackContract.TrackData.DATA, TrackAdapter.ASC);
                if(!sorted){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                checkItem(id);
                break;
            case R.id.path_desc:
                sorted = mListFragment.sort(TrackContract.TrackData.DATA, TrackAdapter.DESC);
                if(!sorted){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                checkItem(id);
                break;
            case R.id.title_asc:
                sorted = mListFragment.sort(TrackContract.TrackData.TITLE, TrackAdapter.ASC);
                if(!sorted){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                checkItem(id);
                break;
            case R.id.title_desc:
                sorted = mListFragment.sort(TrackContract.TrackData.TITLE, TrackAdapter.DESC);
                if(!sorted){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                checkItem(id);
                break;
            case R.id.artist_asc:
                sorted = mListFragment.sort(TrackContract.TrackData.ARTIST, TrackAdapter.ASC);
                if(!sorted){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                checkItem(id);
                break;
            case R.id.artist_desc:
                sorted = mListFragment.sort(TrackContract.TrackData.ARTIST, TrackAdapter.DESC);
                if(!sorted){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                checkItem(id);
                break;
            case R.id.album_asc:
                sorted = mListFragment.sort(TrackContract.TrackData.ALBUM, TrackAdapter.ASC);
                if(!sorted){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                checkItem(id);
                break;
            case R.id.album_desc:
                sorted = mListFragment.sort(TrackContract.TrackData.ALBUM, TrackAdapter.DESC);
                if(!sorted){
                    Snackbar snackbar = AndroidUtils.getSnackbar(mToolbar, getApplicationContext());
                    snackbar.setText(R.string.no_available);
                    snackbar.show();
                    return false;
                }
                checkItem(id);
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void checkItem(int selectedItem) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
        if(selectedItem == -1){
            int currentSelectedItem = sharedPreferences.getInt(Constants.SELECTED_ITEM, -1);
            if(currentSelectedItem == -1){
                MenuItem defaultMenuItemSelected = mMenu.findItem(R.id.title_asc);
                defaultMenuItemSelected.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
                sharedPreferences.edit().putInt(Constants.SELECTED_ITEM, defaultMenuItemSelected.getItemId()).apply();
                sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM, defaultMenuItemSelected.getItemId()).apply();
            }
            else {
                MenuItem menuItemSelected = mMenu.findItem(currentSelectedItem);
                if(menuItemSelected != null) {
                    menuItemSelected.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
                    sharedPreferences.edit().putInt(Constants.SELECTED_ITEM, menuItemSelected.getItemId()).apply();
                    sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM, menuItemSelected.getItemId()).apply();
                }
            }
        }
        else {
            int lastItemSelected = sharedPreferences.getInt(Constants.LAST_SELECTED_ITEM, -1);
            if(selectedItem == lastItemSelected)
                return;

            MenuItem menuItemSelected = mMenu.findItem(selectedItem);
            MenuItem lastMenuItemSelected = mMenu.findItem(lastItemSelected);
            lastMenuItemSelected.setIcon(null);
            menuItemSelected.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
            sharedPreferences.edit().putInt(Constants.SELECTED_ITEM, menuItemSelected.getItemId()).apply();
            sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM, menuItemSelected.getItemId()).apply();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }


    /**
     * Allows to register filters to handle
     * only certain actions sent by FixerTrackService
     */
    private void setupReceivers(){
        IntentFilter apiInitializedFilter = new IntentFilter(Constants.GnServiceActions.ACTION_API_INITIALIZED);
        IntentFilter connectionLostFilter = new IntentFilter(Constants.Actions.ACTION_CONNECTION_LOST);
        IntentFilter startTaskFilter = new IntentFilter(Constants.Actions.ACTION_START_TASK);
        IntentFilter showProgressFilter = new IntentFilter(Constants.Actions.START_PROCESSING_FOR);
        IntentFilter finishProcessingFilter = new IntentFilter(Constants.Actions.FINISH_TRACK_PROCESSING);
        IntentFilter completedTaskFilter = new IntentFilter(Constants.Actions.ACTION_COMPLETE_TASK);
        IntentFilter errorTask = new IntentFilter(Constants.Actions.ACTION_SD_CARD_ERROR);

        mReceiver = new ResponseReceiver(this, new Handler());

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        localBroadcastManager.registerReceiver(mReceiver, completedTaskFilter);
        localBroadcastManager.registerReceiver(mReceiver, showProgressFilter);
        localBroadcastManager.registerReceiver(mReceiver, connectionLostFilter);
        localBroadcastManager.registerReceiver(mReceiver, finishProcessingFilter);
        localBroadcastManager.registerReceiver(mReceiver, apiInitializedFilter);
        localBroadcastManager.registerReceiver(mReceiver, startTaskFilter);
        localBroadcastManager.registerReceiver(mReceiver, errorTask);

    }

    @Override
    public void onClickCover() {
        /*TrackDetailFragment trackDetailFragment = TrackDetailFragment.newInstance("","");
        getSupportFragmentManager().beginTransaction().
                replace(R.id.fragment_container, trackDetailFragment).
                addToBackStack(TrackDetailFragment.TAG).
                commit();*/
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

            Intent shareIntent = ShareCompat.IntentBuilder.from(this).setType("text/plain").setText(shareSubText +"\n"+ shareBodyText).getIntent();
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
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


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
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
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,  Uri.parse(Constants.PLAY_STORE_URL + packageName)));
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
        Snackbar snackbar;
        Toast toast;
        Log.d(TAG, action);
        switch (action) {
            case Constants.GnServiceActions.ACTION_API_INITIALIZED:
                    snackbar = AndroidUtils.getSnackbar(this.mToolbar, this);
                    if(mListFragment.getDatasource().size() > 0) {
                        snackbar.setText(R.string.api_initialized);
                    }
                    else {
                        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                            snackbar.setText(R.string.title_dialog_permision);
                        }
                        else {
                            snackbar.setText(R.string.add_some_tracks);
                        }
                    }
                    snackbar.show();

                break;
            case Constants.Actions.ACTION_CONNECTION_LOST:
                    toast = AndroidUtils.getToast(this);
                    toast.setText(R.string.connection_lost);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.show();
                break;

            case Constants.Actions.ACTION_START_TASK:
                    mListFragment.correctionStarted();
                break;
            case Constants.Actions.ACTION_SD_CARD_ERROR:
                    toast = AndroidUtils.getToast(getApplicationContext());
                    toast.setText(intent.getStringExtra("error"));
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.show();
                break;
            case Constants.Actions.START_PROCESSING_FOR:
                    mListFragment.scrollTo(id);
                break;
            case Constants.Actions.FINISH_TRACK_PROCESSING:
                    String error = intent.getStringExtra("error");
                    if(error != null){
                        toast = AndroidUtils.getToast(getApplicationContext());
                        toast.setText(error);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.show();
                    }
                break;
            case Constants.Actions.ACTION_COMPLETE_TASK:
                    mListFragment.correctionCompleted();
                    String message = intent.getStringExtra("message");
                    toast = AndroidUtils.getToast(getApplicationContext());
                    toast.setText(message);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.show();
                break;
        }

    }
}
