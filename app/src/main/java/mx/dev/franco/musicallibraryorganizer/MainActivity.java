package mx.dev.franco.musicallibraryorganizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mx.dev.franco.musicallibraryorganizer.database.DataTrackDbHelper;
import mx.dev.franco.musicallibraryorganizer.database.TrackContract;
import mx.dev.franco.musicallibraryorganizer.list.AudioItem;
import mx.dev.franco.musicallibraryorganizer.list.TrackAdapter;
import mx.dev.franco.musicallibraryorganizer.services.DetectorInternetConnection;
import mx.dev.franco.musicallibraryorganizer.services.FixerTrackService;
import mx.dev.franco.musicallibraryorganizer.services.Job;
import mx.dev.franco.musicallibraryorganizer.utilities.Constants;
import mx.dev.franco.musicallibraryorganizer.utilities.RequiredPermissions;
import mx.dev.franco.musicallibraryorganizer.utilities.SimpleMediaPlayer;

import static mx.dev.franco.musicallibraryorganizer.services.GnService.sApiInitialized;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
    TrackAdapter.AudioItemHolder.ClickListener {
    public static final String ACTION_OPEN_MAIN_ACTIVITY = "main_action";
    public static String TAG = MainActivity.class.getName();

    //flag to indicate when the app is retrieving data from Gracenote service
    public static boolean sIsGettingData = false;

    //Reasons why cannot execute task
    public static final int PROCESSING_TASK = 42;

    //media player instance, only one is allowed
    public static SimpleMediaPlayer sMediaPlayer;
    //indicates there's no action to take
    private static final int NO_ID = -1;
    //object for sorting the list
    private static TrackAdapter.Sorter sSorter;


    //Adapter of recyclerview
    private TrackAdapter mAudioItemArrayAdapter;
    //Data source to populate the adapter
    private List<AudioItem> mAudioItemList;

    //actions to indicate from where to retrieve data or what to do in first use app.
    private static final int RE_SCAN = 20;
    private static final int CREATE_DATABASE = 21;
    private static final int READ_FROM_DATABASE = 22;

    //message to user when permission to read files is not granted, or
    //in case there have no music files
    private TextView mSearchAgainMessageTextView;
    //search widget object, for search more quickly
    //any track by title in recyclerview list
    private SearchView mSearchViewWidget;
    //FloatingActionButton button, this executes main task: correct a bunch of selected tracks;
    //this executes the automatic mode, without intervention of user,
    //it can also cancel the task, in case the user decides it.
    private FloatingActionButton mFloatingActionButton;
    //swipe refresh layout for give to user the
    //ability to re scan his/her library making a swipe down gesture,
    //this is a material design pattern
    private SwipeRefreshLayout mSwipeRefreshLayout;
    //recycler view is a component that delivers
    //better performance with huge data sources
    private RecyclerView mRecyclerView;
    //instance to connection to database
    private DataTrackDbHelper mDataTrackDbHelper;
    //local broadcast for handling responses from FixerTrackService.
    private LocalBroadcastManager mLocalBroadcastManager;
    //these filters help to indicate which intents can be
    //received and handled from FixerTrackService.
    private IntentFilter mFilterActionDone;
    private IntentFilter mFilterActionCancel;
    private IntentFilter mFilterActionCompleteTask;
    private IntentFilter mFilterActionFail;
    private IntentFilter mFilterApiInitialized;
    private IntentFilter mFilterActionSetAudioProcessing;
    private IntentFilter mFilterActionNotFound;
    private IntentFilter mFilterActionConnectionLost;
    //the receiver that handles the intents from FixerTrackService
    private ResponseReceiver mReceiver;

    //global references to menu items
    private MenuItem mMenuItemPath, mMenuItemTitle, mMenuItemArtist, mMenuItemAlbum;
    private MenuItem lastCheckedItem;
    private Menu mMenu;

    private GoogleApiClient client;

    //contextual Toolbar
    private Toolbar mToolbar;

    //snackbar is a bottom UI component
    //to indicate to user what is happening
    private Snackbar mSnackbar;
    //a reference to action bar to making use
    //of something useful methods from this object
    private ActionBar mActionBar;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("UseSparseArrays")
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

        //get unique instances from database connection and media player
        mDataTrackDbHelper = DataTrackDbHelper.getInstance(getApplicationContext());
        sMediaPlayer = SimpleMediaPlayer.getInstance(getApplicationContext());

        //create filters to listen for response from FixerTrackService
        mFilterActionDone = new IntentFilter(Constants.Actions.ACTION_DONE);
        mFilterActionCancel = new IntentFilter(Constants.Actions.ACTION_CANCEL_TASK);
        mFilterActionNotFound = new IntentFilter(Constants.Actions.ACTION_NOT_FOUND);
        mFilterActionCompleteTask = new IntentFilter(Constants.Actions.ACTION_COMPLETE_TASK);
        mFilterActionFail = new IntentFilter(Constants.Actions.ACTION_FAIL);
        mFilterApiInitialized = new IntentFilter(Constants.GnServiceActions.ACTION_API_INITIALIZED);
        mFilterActionSetAudioProcessing = new IntentFilter(Constants.Actions.ACTION_SET_AUDIOITEM_PROCESSING);
        mFilterActionConnectionLost = new IntentFilter(Constants.Actions.ACTION_CONNECTION_LOST);

        //create receiver
        mReceiver = new ResponseReceiver();
        //get instance of local broadcast manager
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mSearchAgainMessageTextView = (TextView) findViewById(R.id.genericMessage);
        //Initialize recycler view and swipe refresh layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);

        mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getApplicationContext(),R.color.primaryColor));
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_900));

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        //layout manager gives the ability to show elements in different
        //ways, for example, LinearLayoutManager, shows as a list, but we can use
        //GridLayoutManager and our elements will be shown as mosaics.
        mRecyclerView.setLayoutManager( new LinearLayoutManager(this));
        //this options gives us more improvements to our list
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        //enable sound and vibration when touch an element
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);

        //get the actionbar to enable some extra functionality
        //to toolbar
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();

        //hide FloatingActionButton, then show if and only if list has been created
        mFloatingActionButton.hide();
        //create data source and adapter
        mAudioItemList = new ArrayList<>();
        mAudioItemArrayAdapter = new TrackAdapter(getApplicationContext(), mAudioItemList,this);
        //sorting object
        sSorter = TrackAdapter.Sorter.getInstance();

        //attach the adapter to media player
        sMediaPlayer.setAdapter(mAudioItemArrayAdapter);
        //we create a snack bar for messages
        createSnackBar();

        //attach adapter to our recyclerview
        mRecyclerView.setAdapter(mAudioItemArrayAdapter);
        //Lets implement functionality for refresh layout listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSearchAgainMessageTextView.setVisibility(View.GONE);
                //If not permission granted show the reason to access files until user grant permission
                if(!RequiredPermissions.ACCESS_GRANTED_FILES) {
                    showRequestPermissionReason();
                }
                else {
                    //after onStop, we can save a
                    //if we have the permission, check Bundle object to verify if the activity comes from onPause or from onCreate event
                    //if(savedInstanceState == null){
                        //int taskType = DataTrackDbHelper.existDatabase(getApplicationContext()) && mDataTrackDbHelper.getCount(null) > 0 ? READ_FROM_DATABASE : CREATE_DATABASE;
                        AsyncReadFile asyncReadFile = new AsyncReadFile(RE_SCAN);
                        asyncReadFile.execute();
                    //}

                }
            }

        });



        //If we already had the permission granted, lets go  read data from database and pass them to adapter, to show in Recyclerview,
        //otherwise we show the reason to access files until user grant permission

        if(!RequiredPermissions.ACCESS_GRANTED_FILES) {
            showRequestPermissionReason();
        }
        else {
            //if we have the permission, check Bundle object to verify if the activity comes from onPause or from onCreate
            //if(savedInstanceState == null){
                mRecyclerView.setAdapter(mAudioItemArrayAdapter);
                int taskType = DataTrackDbHelper.existDatabase(this) && mDataTrackDbHelper.getCount(null) > 0 ? READ_FROM_DATABASE : CREATE_DATABASE;

                AsyncReadFile asyncReadFile = new AsyncReadFile(taskType);
                asyncReadFile.execute();
            //}
        }
        //Actually if service is running, then register filters and receiver immediatly to update UI
        if(ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning()) {
            registerReceivers();
            startTask();
        }
        //if not, then go through normal flow of components initialization
        else {
            mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupActionFloatingActionButton();
                }
            });
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        Log.d(TAG,"onCreate");
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
        Log.d(TAG,"onStart");

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        //restore state of this components when restore event be fired
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTask();
            }
        });

        if(mRecyclerView.getAdapter() == null)
            mRecyclerView.setAdapter(mAudioItemArrayAdapter);
        Log.d(TAG,"onRestoreInstanceState");

    }


    @Override
    protected void onResume(){
        super.onResume();
        //register this filter only in case internet connection could not
        //be stablished at splash, and then receive a notification when a connection
        //could be stablished
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterApiInitialized);
        Log.d(TAG,"onResume");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG,"onPause");
        //Deregister filters if FixerTrackService is not processing any task,
        //useful for saving resources

        if(!ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning())
            mLocalBroadcastManager.unregisterReceiver(mReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        Log.d(TAG,"onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);

    }

    @Override
    public void onStop() {
        Log.d(TAG,"onStop");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
        super.onStop();
    }

    @Override
    public void onDestroy(){
        Log.d(TAG,"onDestroy");
        //Before app closes, check if "Correccion en segundo plano" is OFF, in this case cancel the current task and
        // release resources used by DB connection
        if(ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning()
                && !Settings.BACKGROUND_CORRECTION){
            Intent intentStopService = new Intent(this, FixerTrackService.class);
            stopService(intentStopService);
            if(mDataTrackDbHelper != null) {
                mDataTrackDbHelper.close();
                mDataTrackDbHelper = null;
            }
        }
        //if previous condition is ON, task will continue working after app closes.
        //DB connection is not close because service will continue updating data in background.
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
        if(sMediaPlayer.isPlaying()){
            sMediaPlayer.stop();
            sMediaPlayer.reset();
            sMediaPlayer.release();
            sMediaPlayer = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        //get global reference to menu, useful for manipulating where necessary
        mMenu = menu;

        MenuItem searchItem = menu.findItem(R.id.action_search);
        //get a global reference to search widget
        mSearchViewWidget = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchViewWidget.setVisibility(View.GONE);

        //get global references to sort types actions
        mMenuItemPath = menu.findItem(R.id.action_sort_by_path);
        mMenuItemTitle = menu.findItem(R.id.action_sort_by_title);
        mMenuItemArtist = menu.findItem(R.id.action_sort_by_artist);
        mMenuItemAlbum = menu.findItem(R.id.action_sort_by_album);
        setCheckedItem(null);

        // Define an expand listener for search widget
        MenuItemCompat.OnActionExpandListener expandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                //when no searching a track, activate refresh listener
                //of swipe layouy
                if(mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setEnabled(true);
                }
                //reset filter to show all tracks
                if(mAudioItemArrayAdapter != null) {
                    mAudioItemArrayAdapter.getFilter().filter("");
                }
                //and show floating action menu if user have songs
                if(mAudioItemList.size() != 0)
                    mFloatingActionButton.show();
                //finally remove listener
                mSearchViewWidget.setOnQueryTextListener(null);
                return true;  // Return true to collapse action mSwipeRefreshLayout
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                //if no songs, no case to expand the search widget
                if(mAudioItemList.size() <= 0){
                    showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.no_items_found),NO_ID);
                    return false;
                }
                //when searching a song, deactivate the swipe refresh layout
                //and don't let update with swipe gesture
                if (mSwipeRefreshLayout != null){
                    mSwipeRefreshLayout.setEnabled(false);
                }
                //when app is reading data from DB
                //do not let searching to avoid an error
                if(sIsGettingData){
                    showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.getting_data),NO_ID);
                    return false;
                }

                //then if user is searching a song, hide the fab button
                mFloatingActionButton.hide();

                //attach a listener that returns results while user is searching his/her song
                mSearchViewWidget.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if(mAudioItemArrayAdapter != null) {
                            mAudioItemArrayAdapter.getFilter().filter(newText);
                        }
                        else {
                            showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.no_items_found),-1);
                        }
                        return true;
                    }
                });
                return true;  // Return true to expand action mSwipeRefreshLayout
            }
        };

        // Assign the listener to searchItem
        MenuItemCompat.setOnActionExpandListener(searchItem, expandListener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item_list clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_select_all:
                actionSelectAll();
                break;

            case R.id.action_refresh:
                actionRefresh();
                break;

            case R.id.path_asc:
                actionSortBy(TrackContract.TrackData.DATA, TrackAdapter.ASC);
                setCheckedItem(item);
                break;
            case R.id.path_desc:
                actionSortBy(TrackContract.TrackData.DATA, TrackAdapter.DESC);
                setCheckedItem(item);
                break;

            case R.id.title_asc:
                actionSortBy(TrackContract.TrackData.TITLE, TrackAdapter.ASC);
                setCheckedItem(item);
                break;
            case R.id.title_desc:
                actionSortBy(TrackContract.TrackData.TITLE, TrackAdapter.DESC);
                setCheckedItem(item);
                break;

            case R.id.artist_asc:
                actionSortBy(TrackContract.TrackData.ARTIST, TrackAdapter.ASC);
                setCheckedItem(item);
                break;
            case R.id.artist_desc:
                actionSortBy(TrackContract.TrackData.ARTIST, TrackAdapter.DESC);
                setCheckedItem(item);
                break;

            case R.id.album_asc:
                actionSortBy(TrackContract.TrackData.ALBUM, TrackAdapter.ASC);
                setCheckedItem(item);
                break;
            case R.id.album_desc:
                actionSortBy(TrackContract.TrackData.ALBUM, TrackAdapter.DESC);
                setCheckedItem(item);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setCheckedItem(MenuItem item){
        if(mAudioItemList.size() == 0 ){
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.no_items), NO_ID);
            return;
        }

        //can't check an item while service is running and correcting songs, so wait until processing finish
        if(ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning()){
            showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.processing_task),-1);
            return;
        }
        int id;
        if(item != null) {
            id = item.getItemId();
            //reset indicator in previous item selected
            if(lastCheckedItem != null)
                lastCheckedItem.setIcon(null);

            lastCheckedItem = item;
            lastCheckedItem.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = preferences.edit();
            Log.d("id_sort_item",id+"");
            editor.putInt("key_default_sort",id);
            editor.apply();
            preferences = null;
            editor = null;
        }
        else {
            id = Settings.SETTING_SORT;
            Log.d("el id", id + "");
            lastCheckedItem = mMenu.findItem(id == 0  ? R.id.path_asc : id);
            lastCheckedItem.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
        }

        //mark sort type and reset other UI sort elements

        switch (id){
            case R.id.path_asc:
            case R.id.path_desc:
            case 0:
            case 1:
                mMenuItemTitle.setIcon(null);
                mMenuItemArtist.setIcon(null);
                mMenuItemAlbum.setIcon(null);
                mMenuItemPath.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
                break;

            case R.id.title_asc:
            case R.id.title_desc:
                mMenuItemArtist.setIcon(null);
                mMenuItemAlbum.setIcon(null);
                mMenuItemPath.setIcon(null);
                mMenuItemTitle.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
                break;

            case R.id.artist_asc:
            case R.id.artist_desc:
                mMenuItemTitle.setIcon(null);
                mMenuItemAlbum.setIcon(null);
                mMenuItemPath.setIcon(null);
                mMenuItemArtist.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
                break;

            case R.id.album_asc:
            case R.id.album_desc:
                mMenuItemTitle.setIcon(null);
                mMenuItemArtist.setIcon(null);
                mMenuItemPath.setIcon(null);
                mMenuItemAlbum.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white));
                break;
        }

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation mSwipeRefreshLayout item_list clicks here.

        int id = item.getItemId();

        if (id == R.id.rate) {
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.in_development),NO_ID);
        } else if (id == R.id.share) {
            Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                    .setType("text/plain")
                    .setText(getString(R.string.app_name) + " " + getString(R.string.share_message) ).getIntent();
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
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
        else {
            ;
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        //verify permission to access files and execute scan if were granted
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setFilePermissionGranted();
            executeScan();
        }
        //if not, show a message to indicate to user to swipe up to refresh
        else{
            mSwipeRefreshLayout.setRefreshing(false);
            mSearchAgainMessageTextView.setText(R.string.swipe_up_search);
            mSearchAgainMessageTextView.setVisibility(View.VISIBLE);
            mFloatingActionButton.hide();
        }

    }


    /**
     * this method handles click to every item_list in recycler view
     * @param position
     * @param view
     */
    @Override
    public void onItemClicked(int position, View view) {
        switch (view.getId()) {
            case R.id.coverArt:
                onClickCoverArt(view, position, Constants.CorrectionModes.VIEW_INFO);
                break;
            case R.id.checkBoxTrack:
                selectItem((long)view.getTag(), view, position);
                break;
            default:
                try {
                    correctSong(view, position);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void actionRefresh(){
        //request permission in case is necessary and update
        //our database
        if(RequiredPermissions.ACCESS_GRANTED_FILES) {
            AsyncReadFile asyncReadFile = new AsyncReadFile(RE_SCAN);
            asyncReadFile.execute();
        }
        else {
            showRequestPermissionReason();
        }
    }

    /**
     * Sets the action to floating action button (mFloatingActionButton)
     */
    private void setupActionFloatingActionButton(){
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        else {

            //Automatic mode require some conditions to execute
            //we put this in AsyncTask because the isConnected method
            //makes a network operation which blocks UI if we execute
            //in UI Thread
            new AsyncTask<Context,Void, Integer>(){
                @Override
                protected Integer doInBackground(Context contexts[]){
                    int canContinue = allowExecute(contexts[0].getApplicationContext());

                    return canContinue;
                }
                @Override
                protected void onPostExecute(Integer canContinue){
                    if(canContinue != 0) {
                        setSnackBarMessage(canContinue);
                        return;
                    }

                    if(mAudioItemArrayAdapter.getCountSelectedItems() == 0){
                        showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.no_songs_to_correct), NO_ID);
                        return;
                    }

                    //start correction in automatic mode
                    registerReceivers();
                    Intent intent = new Intent(MainActivity.this, FixerTrackService.class);
                    intent.putExtra(Constants.Activities.FROM_EDIT_MODE, false);

                    if(Settings.BACKGROUND_CORRECTION)
                        intent.putExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION, true);

                    startService(intent);
                    startTask();
                }
            }.execute(this);




        }
    }

    /**
     * This method creates a general mSnackbar,
     * for recycle its use
     */
    private void createSnackBar() {
        mSnackbar = Snackbar.make(mSwipeRefreshLayout,"",Snackbar.LENGTH_SHORT);
        TextView tv = (TextView) this.mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);

        mSnackbar.getView().setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.primaryLightColor));
        tv.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
        mSnackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));

    }

    /**
     * @param duration
     * @param msg
     * @param id
     */
    private void showSnackBar(int duration, String msg, final long id){

        if(mSnackbar != null) {
            mSnackbar = null;
            createSnackBar();
        }

        //no action if no ID
        if(id == -1){
            mSnackbar.setText(msg);
            mSnackbar.setDuration(duration);
            mSnackbar.setAction("",null);
        }
        else {
            //setaction if id != -1
            mSnackbar.setText(msg);
            mSnackbar.setDuration(duration);
            mSnackbar.setAction(R.string.manual_mode, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent2 = new Intent(MainActivity.this, TrackDetailsActivity.class);
                    intent2.putExtra("itemId",id);
                    intent2.putExtra("manualMode",true);
                    startActivity(intent2);
                }
            });
        }

        mSnackbar.show();
    }

    /**
     * Some actions require that some conditions are true
     * this method verifies these conditions
     * @param appContext
     * @return
     */
    public static int allowExecute(Context appContext){
        Context context = appContext.getApplicationContext();
        //No internet connection
        if(!DetectorInternetConnection.sIsConnected){
            DetectorInternetConnection.startCheckingConnection(appContext);
            return Constants.Conditions.NO_INTERNET_CONNECTION;
        }

        //API not initialized
        if(!sApiInitialized){
            Job.scheduleJob(context);
            return Constants.Conditions.NO_INITIALIZED_API;
        }

        //Task is already executing
        if(ServiceHelper.withContext(appContext).withService(FixerTrackService.CLASS_NAME).isServiceRunning()){
            return PROCESSING_TASK;
        }

        return 0;
    }

    /**
     * This method mark as select all
     * items in recycler view
     */
    private void actionSelectAll(){

        if(mAudioItemList.size() == 0 ){
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.no_items), NO_ID);
            return;
        }

        //wait until processing finish
        if(ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning()){
            showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.processing_task),-1);
            return;
        }

        boolean areAllSelected = mAudioItemArrayAdapter.areAllSelected();

        new AsyncTask<Boolean, Integer, Void>(){
            @Override
            protected Void doInBackground(Boolean... params) {
                ContentValues values = new ContentValues();
                values.put(TrackContract.TrackData.IS_SELECTED,!params[0]);
                mDataTrackDbHelper.updateData(values);
                for(int f = 0; f< mAudioItemArrayAdapter.getItemCount() ; f++){
                    mAudioItemList.get(f).setChecked(!params[0]);
                    publishProgress(f);
                }
                mAudioItemArrayAdapter.setAllSelected(!params[0]);
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... integers){
                super.onProgressUpdate(integers);
                mAudioItemArrayAdapter.notifyItemChanged(integers[0]);
            }

        }.execute(areAllSelected);


    }

    private void actionSortBy(String sortBy, int sortType){

        if(mAudioItemList.size() == 0 ){
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.no_items), NO_ID);
            return;
        }


        if(ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning()){
            showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.processing_task),-1);
            return;
        }

        sSorter.setSortParams(sortBy, sortType);
        Collections.sort(mAudioItemList, sSorter);
        mAudioItemArrayAdapter.notifyDataSetChanged();
    }



    /**
     * This method starts a correction for a single item_list
     * @param view
     * @param position
     * @throws IOException
     * @throws InterruptedException
     */
    private void correctSong(View view, final int position) throws IOException, InterruptedException {
        final String absolutePath = (String) view.findViewById(R.id.absolute_path).getTag();
        final View getView = view.findViewById(R.id.coverArt);

        //check if audio item_list can accessed
        boolean canBeRead = AudioItem.checkFileIntegrity(absolutePath);
        if(!canBeRead){
            showConfirmationDialog(absolutePath,position);
            return;
        }

        final AudioItem audioItem = mAudioItemArrayAdapter.getAudioItemByPosition(position); //mAudioItemArrayAdapter.getAudioItemByIdOrPath(NO_ID, absolutePath);

        //wait until service finish correction to this track
        if(ServiceHelper.withContext(getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning()){
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.processing_task), NO_ID);
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.correction_mode)).setMessage(getString(R.string.select_correction_mode) + " " + AudioItem.getFilename(absolutePath) + "?")
                .setNeutralButton(getString(R.string.manual), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickCoverArt(getView, position, Constants.CorrectionModes.MANUAL);
                    }
                })
                .setNegativeButton(getString(R.string.semiautomatic), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                        onClickCoverArt(getView, position, Constants.CorrectionModes.SEMI_AUTOMATIC);

                    }
                })
                .setPositiveButton(getString(R.string.automatic), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //we put this in AsyncTask because the isConnected method
                        //makes a network operation which blocks UI if we execute
                        //from UI Thread
                        new AsyncTask<Context,Void, Integer>(){
                            @Override
                            protected Integer doInBackground(Context contexts[]){
                                int canContinue = allowExecute(contexts[0].getApplicationContext());
                                return canContinue;
                            }
                            @Override
                            protected void onPostExecute(Integer canContinue){
                                if(canContinue != 0) {
                                    setSnackBarMessage(canContinue);
                                    return;
                                }

                                registerReceivers();
                                Intent intent = new Intent(MainActivity.this, FixerTrackService.class);
                                if(Settings.BACKGROUND_CORRECTION)
                                    intent.putExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION, true);
                                intent.putExtra(Constants.Activities.FROM_EDIT_MODE, false);
                                intent.putExtra(Constants.MEDIASTORE_ID, audioItem.getId());
                                startService(intent);
                                startTask();
                            }
                        }.execute(MainActivity.this);
                    }
                });
        final AlertDialog dialog =  builder.create();
        dialog.show();


    }

    /**
     * Shows a confirmation dialog when user is going to cancel current task
     * @param absolutePath
     * @param position
     */
    private void showConfirmationDialog(final String absolutePath, final int position){
        final AudioItem audioItem = mAudioItemArrayAdapter.getAudioItemByPosition(position); //mAudioItemArrayAdapter.getAudioItemByIdOrPath(NO_ID, absolutePath);

        String msg = "";
        String title = "";
        msg = getString(R.string.file_error);
        title = getString(R.string.title_dialog_file_error);


        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAudioItemList.remove(audioItem);
                mAudioItemArrayAdapter.notifyItemRemoved(position);
                updateNumberItems();
            }
        });


        builder.setTitle(title).setMessage(msg);

        final AlertDialog dialog =  builder.create();
        dialog.setCancelable(true);
        dialog.show();
    }


    /**
     * Opens new activity showing up the details from current audio item_list pressed
     * @param view
     * @param position
     * @param mode
     */
    public void onClickCoverArt(View view, int position, int mode){

        AudioItem audioItem = mAudioItemArrayAdapter.getAudioItemByPosition(position);
        String path = audioItem.getAbsolutePath();

        if(!AudioItem.checkFileIntegrity(path)){
            showConfirmationDialog(path,position);
            return;
        }

        if(audioItem.isProcessing()){
            showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.current_file_processing),NO_ID);
            return;
        }

        Intent intent = new Intent(this, TrackDetailsActivity.class);
        intent.putExtra(Constants.POSITION, position);
        intent.putExtra(Constants.MEDIASTORE_ID, audioItem.getId());
        intent.putExtra(Constants.CorrectionModes.MODE, mode);
        /*ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                MainActivity.this,
                view,
                ViewCompat.getTransitionName(view));*/

        startActivity(intent);
    }

    /**
     * This method marks as true selected column in database
     * and checks the audioitem checkbox
     * @param id
     * @param view
     * @param position
     */
    private void selectItem(final long id, final View view, final int position){
        AudioItem audioItem = mAudioItemArrayAdapter.getAudioItemByIdOrPath(id, null);
        Log.d("mMenuItemPath", audioItem.getAbsolutePath());
        if(!AudioItem.checkFileIntegrity(audioItem.getAbsolutePath())){
            showConfirmationDialog(audioItem.getAbsolutePath(),position);
            return;
        }

        final CheckBox checkBox = (CheckBox) view;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor data = mDataTrackDbHelper.getDataRow(id);
                ContentValues newValues = null;
                if(data != null){
                    data.moveToFirst();

                    boolean isChecked = data.getInt(data.getColumnIndexOrThrow(TrackContract.TrackData.IS_SELECTED)) != 0;

                    AudioItem audioItem = mAudioItemArrayAdapter.getAudioItemByIdOrPath(id, "");

                    newValues = new ContentValues();
                    if(isChecked) {
                        newValues.put(TrackContract.TrackData.IS_SELECTED, false);
                        audioItem.setChecked(false);

                        //only main thread can touch its views.
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                checkBox.setChecked(false);
                            }
                        });

                    }
                    else {
                        newValues.put(TrackContract.TrackData.IS_SELECTED, true);
                        audioItem.setChecked(true);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                checkBox.setChecked(true);
                            }
                        });
                    }
                    mDataTrackDbHelper.updateData(id, newValues);
                    newValues.clear();
                    data.close();
                    data = null;

                    System.gc();

                }

            }

        }).start();


    }

    /**
     * Executes the scan task
     * to seek supported audio files
     * inside smartphone memory
     */
    private void executeScan(){
        if(mRecyclerView.getAdapter() == null)
            mRecyclerView.setAdapter(mAudioItemArrayAdapter);
        //if previously was granted the permission and our database has data
        //dont' clear that data, only read it instead.
        int taskType = DataTrackDbHelper.existDatabase(getApplicationContext()) && mDataTrackDbHelper.getCount(null) > 0 ? READ_FROM_DATABASE : CREATE_DATABASE;
        AsyncReadFile asyncReadFile = new AsyncReadFile(taskType);
        asyncReadFile.execute();

    }


    /**
     * Request permission indicate in input parameter
     * @param permission
     */
    private void requestPermission(String permission) {

        if(permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)){
            //No permission? then request it
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, RequiredPermissions.READ_EXTERNAL_STORAGE_PERMISSION);
            }
            else {
                executeScan();
            }
        }
    }

    /**
     * Sets the running state to floating action button
     * and change drawable to indicate that indicate
     * to user that it can stop this task
     */
    private void startTask(){
        this.mFloatingActionButton.setOnClickListener(null);
        this.mFloatingActionButton.setImageDrawable(getDrawable(R.drawable.ic_stop_white_24dp));
        this.mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 setStartTaskStateFab();
            }
        });
    }

    /**
     * Shows a dialog to user and
     * it needs to decide whether
     * cancel task or not
     */
    private void setStartTaskStateFab(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.cancelling).setMessage(R.string.cancel_task)
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //stops service, and sets starting state to FAB
                        Intent requestStopService = new Intent(MainActivity.this, FixerTrackService.class);
                        stopService(requestStopService);
                        finishTaskByUser();
                    }
                });
        final AlertDialog dialog =  builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * Allows to register filters to handle
     * only certain actions sent by FixerTrackService
     */
    private void registerReceivers(){
        //register filters to listen for response from FixerTrackService
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionDone);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionCancel);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionCompleteTask);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionFail);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionSetAudioProcessing);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionNotFound);
        mLocalBroadcastManager.registerReceiver(mReceiver, mFilterActionConnectionLost);

    }

    /**
     * Stops service, and sets starting state to FAB
     */
    private void finishTaskByUser(){
        //if correction task is cancelled by user
        //set processing state to false of every item
        mAudioItemArrayAdapter.cancelProcessing();
        //once FixerTrackService has finished, we need to deregister
        //receiver to save battery
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
        MainActivity.this.mFloatingActionButton.setOnClickListener(null);
        MainActivity.this.mFloatingActionButton.setImageDrawable(getDrawable(R.drawable.ic_magic_wand_auto_fix_button));
        MainActivity.this.mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupActionFloatingActionButton();
            }
        });
    }

    /**
     * Sets a message to show in snackbar
     * @param reason
     */
    private void setSnackBarMessage(int reason){
        String msg = "";
        switch (reason){
            case Constants.Conditions.NO_INTERNET_CONNECTION:
                msg = getString(R.string.no_internet_connection_automatic_mode);
                break;
            case MainActivity.PROCESSING_TASK:
                msg = getString(R.string.processing_task);
                break;
            case Constants.Conditions.NO_INITIALIZED_API:
                msg = getString(R.string.initializing_recognition_api);
                break;
        }
        showSnackBar(Snackbar.LENGTH_SHORT, msg,-1);
    }

    /**
     * Save to shared preferences the access files permission
     */
    private void setFilePermissionGranted(){
        SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("accessFilesPermission", true);
        editor.apply();
        RequiredPermissions.ACCESS_GRANTED_FILES = true;
        editor = null;
        sharedPreferences = null;

    }

    /**
     * Indicates to user why is necessary this permission
     */
    private void showRequestPermissionReason(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_permision).setMessage(R.string.explanation_permission_access_files);
        builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user cancelled permission request,
                //show a message indicating that no songs were found
                mSearchAgainMessageTextView.setVisibility(View.VISIBLE);
                mSearchAgainMessageTextView.setText(R.string.swipe_up_search);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
        final AlertDialog dialog =  builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * Sets new tags (if were found) to current audio item
     * processed and update UI
     * @param newAudioItem
     * @param processedId
     */
    private void setNewItemValues(@Nullable AudioItem newAudioItem, long processedId) {
        //no results were found if newAudioItem is null
        boolean newAudioItemFound = (newAudioItem != null);
        long id = newAudioItemFound ? newAudioItem.getId() : processedId;
        //get current item by id received and set its processing and check state to false
        AudioItem actualAudioItem = mAudioItemArrayAdapter.getAudioItemByIdOrPath(id,null);

        //check if actual audio item is exist still in list
        if(actualAudioItem != null) {
            //if new tags were found, extract them values from audio item received from service
            //and sets to actual audio item
            if(newAudioItemFound){
                if(!newAudioItem.getTitle().isEmpty())
                    actualAudioItem.setTitle(newAudioItem.getTitle());
                if(!newAudioItem.getArtist().isEmpty())
                    actualAudioItem.setArtist(newAudioItem.getArtist());
                if(!actualAudioItem.getAlbum().isEmpty())
                    actualAudioItem.setAlbum(newAudioItem.getAlbum());

                actualAudioItem.setAbsolutePath(newAudioItem.getAbsolutePath());
                actualAudioItem.setStatus(newAudioItem.getStatus());
            }
            //if not only set status to show an icon to indicate
            //that no tags were found
            else {
                actualAudioItem.setStatus(AudioItem.STATUS_NO_TAGS_FOUND);
            }

            //update UI status of audio item
            actualAudioItem.setChecked(false);
            actualAudioItem.setProcessing(false);
            mAudioItemArrayAdapter.notifyItemChanged(actualAudioItem.getPosition());
            //reset position because can change when we reorder the list
            actualAudioItem.setPosition(-1);
            actualAudioItem = null;
        }
    }

    /**
     * Set its processing state to true and update UI
     * to indicate which audio item is currently processing
     * @param id
     */
    private void setProcessingAudioItem(long id){
        AudioItem audioItem = mAudioItemArrayAdapter.getAudioItemByIdOrPath(id,null);
        audioItem.setProcessing(true);
        mRecyclerView.scrollToPosition(audioItem.getPosition());
        mAudioItemArrayAdapter.notifyItemChanged(audioItem.getPosition());
        audioItem.setPosition(-1);
        audioItem = null;
    }

    /**
     * Updates number items shown in action bar
     * when are detected
     */
    private void updateNumberItems(){
        if(mAudioItemList != null){
            mActionBar.setTitle(mAudioItemList.size() + " " +getString(R.string.tracks));
        }
    }

    /**
     * This class request reads information of audio files from MediaStore,
     * then creates the initial DB when app
     * is used by first time; next openings of app it
     * reads the songs from this database instead of MediaStore
     */
    private class AsyncReadFile extends AsyncTask<Void, AudioItem, Void> {
        //Are we reading from our DB or MediaStore?
        private int taskType;
        //data retrieved from DB
        private Cursor data;
        //how many audio files were added or removed
        //from your smartphone
        private int added = 0;
        private int removed = 0;
        private boolean mMediaScanCompleted = false;

        AsyncReadFile(int codeTaskType){
            this.taskType = codeTaskType;
        }

        @Override
        protected void onPreExecute() {
            //check if a previous read of files
            //could not be completed, in this case
            //clear DB and re read data from Media Store

            SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            mMediaScanCompleted = sharedPreferences.getBoolean(Constants.COMPLETE_READ,false);
            if(!mMediaScanCompleted){
                taskType = CREATE_DATABASE;
            }

            //this method is invoked before
            //our background task begins, often
            //here is where we show for example
            //a progress bar.
            mSwipeRefreshLayout.setRefreshing(true);
            sIsGettingData = true;
            if(mSearchViewWidget != null){
                mSearchViewWidget.setVisibility(View.GONE);
            }

        }

        @Override
        protected Void doInBackground(Void... voids) {
            //make sure when is first use, clearing
            //database to avoid any problem, in case
            //there have had a unsuccessful previous
            //creation of DB
            if(this.taskType == CREATE_DATABASE){
                MainActivity.this.mDataTrackDbHelper.clearDb();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.getting_data),NO_ID);
                        mSearchAgainMessageTextView.setVisibility(View.GONE);
                    }
                });
            }


            switch (taskType){
                //If we are updating for new elements added
                case RE_SCAN:
                    removeUnusedItems();
                    rescanAndUpdateList();
                    break;
                //if database does not exist or is first use of app
                case CREATE_DATABASE:
                    createNewTable();
                    break;
                //if we are reading data from database (this means later app openings)
                case READ_FROM_DATABASE:
                    removeUnusedItems();
                    readFromDatabase();
                    break;
            }

            //close cursor to release this resource
            //there are a limit of cursors open at the same time
            //so make sure to release them.
            if(this.data != null) {
                data.close();
                data = null;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(AudioItem... audioItems) {
            //this method runs on UI thread,
            //here we are updating the UI.
            //is not necessary call its super method
            //because is an emtpy method

            //adds the item to top of list
            if(taskType == RE_SCAN){
                mAudioItemList.add(0,audioItems[0]);
                mAudioItemArrayAdapter.notifyItemInserted(0);
            }
            else {
                mAudioItemList.add(audioItems[0]);
                mAudioItemArrayAdapter.notifyItemInserted(mAudioItemList.size()-1);
            }

            //updates title of action bar
            updateNumberItems();
        }

        @Override
        protected void onPostExecute(Void result) {
            //when doInBackground finishes, this callback
            //is executed. Here is where we hide, for example,
            //a progress bar.

            //is not necessary call its super method
            //because is an emtpy method

            mSwipeRefreshLayout.setRefreshing(false);
            mSwipeRefreshLayout.setEnabled(true);
            sIsGettingData = false;
            SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor =  sharedPreferences.edit();
            //there are not songs?
            if(mAudioItemList.size() == 0){
                MainActivity.this.mSearchAgainMessageTextView.setText(getString(R.string.no_items_found));
                MainActivity.this.mSearchAgainMessageTextView.setVisibility(View.VISIBLE);
                editor.putBoolean(Constants.COMPLETE_READ, false);
                editor.apply();
                editor = null;
                sharedPreferences = null;
                return;
            }

            if(removed > 0){
                showSnackBar(Toast.LENGTH_SHORT,removed + " " + getString(R.string.removed_inexistent),NO_ID);
            }

            if(added > 0 ){
                showSnackBar(Toast.LENGTH_SHORT,added + " " + getString(R.string.new_files_found),NO_ID);
                mRecyclerView.smoothScrollToPosition(0);
            }

            if(mSearchViewWidget != null){
                mSearchViewWidget.setVisibility(View.VISIBLE);
            }
            updateNumberItems();

            //if audio files were found, then show
            //float action button that executes main task.
            //if no audio files were found, there is no case
            //to show this button because there will not have
            //anything to process.
            MainActivity.this.mFloatingActionButton.show();


            editor.putBoolean(Constants.COMPLETE_READ, true);
            editor.apply();
            editor = null;
            sharedPreferences = null;

            System.gc();
        }

        @Override
        public void onCancelled(){
            super.onCancelled();
            //this method will be invoked instead of
            //onPostExecute if AsyncTask is cancelled
            sIsGettingData = false;
            mSwipeRefreshLayout.setEnabled(true);
            mSwipeRefreshLayout.setRefreshing(false);
            updateNumberItems();
            if(mSearchViewWidget != null){
                mSearchViewWidget.setVisibility(View.VISIBLE);
            }
            //save state that could not be completed the data loading from songs
            SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor =  sharedPreferences.edit();
            MainActivity.this.mSearchAgainMessageTextView.setText(getString(R.string.no_items_found));
            MainActivity.this.mSearchAgainMessageTextView.setVisibility(View.VISIBLE);
            editor.putBoolean(Constants.COMPLETE_READ, false);
            editor.apply();
            editor = null;
            sharedPreferences = null;
        }

        /**
         * This method gets data from media store,
         * only mp3 files data is retrieved
         * @return
         */
        private Cursor getDataFromDevice() {

            //Select all music
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

            //Columns to retrieve
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.AlbumColumns.ALBUM ,
                    MediaStore.Audio.Media.DATA // absolute path to audio file
            };

            //get data from content provider
            //the last parameter sorts the data alphanumerically by the "DATA" column in ascendant mode
            return getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    Sort.setDefaultOrder());
        }

        /**
         * This method rescan mediastore DB, and
         * if there are new elements, add them to list
         */
        private void rescanAndUpdateList(){
            data = getDataFromDevice();
            while(data.moveToNext()){
                boolean existInTable = MainActivity.this.mDataTrackDbHelper.existInDatabase(data.getInt(0));

                if(!existInTable){
                    createAndAddAudioItem();
                    added++;
                }
            }

            if(this.data != null) {
                data.close();
                data = null;
            }
        }

        /**
         * Verifies if audio file exist in
         * smartphone storage
         */
        private void removeUnusedItems(){
            for(int pos = 0; pos < mAudioItemList.size() ; pos++){
                AudioItem audioItem = mAudioItemList.get(pos);
                File file = new File(audioItem.getAbsolutePath());
                if (!file.exists()) {
                    MainActivity.this.mDataTrackDbHelper.removeItem(audioItem.getId(), TrackContract.TrackData.TABLE_NAME);
                    mAudioItemList.remove(pos);
                    mAudioItemArrayAdapter.notifyItemRemoved(pos);
                    removed++;
                }
                file = null;
            }
        }


        /**
         * This method creates new table, in case is
         * the first use of app, then passes this data to adapter
         */
        private void createNewTable(){
            data = getDataFromDevice();
            if(data.moveToFirst()) {
                do {
                    createAndAddAudioItem();
                }
                while (data.moveToNext());
            }
        }

        /**
         * This method reads data from our
         * DB created after the first use of app,
         * then passes this data to adapter
         */
        private void readFromDatabase(){

            data = MainActivity.this.mDataTrackDbHelper.getDataFromDB(Sort.setDefaultOrder());
            //retrieved null data or 0 elements, so our database
            //does not contain anything, means that no audio files were found
            int dataLength = data != null ? data.getCount() : 0;
            if (dataLength > 0) {
                while (data.moveToNext()) {
                    int _id = data.getInt(data.getColumnIndexOrThrow(TrackContract.TrackData.MEDIASTORE_ID));
                    String title = data.getString(data.getColumnIndexOrThrow(TrackContract.TrackData.TITLE));
                    String artist = data.getString(data.getColumnIndexOrThrow(TrackContract.TrackData.ARTIST));
                    String album = data.getString(data.getColumnIndexOrThrow(TrackContract.TrackData.ALBUM));
                    String fullpath = data.getString(data.getColumnIndexOrThrow(TrackContract.TrackData.DATA));
                    boolean isChecked = data.getInt(data.getColumnIndexOrThrow(TrackContract.TrackData.IS_SELECTED)) != 0;
                    int status = data.getInt(data.getColumnIndexOrThrow(TrackContract.TrackData.STATUS));
                    boolean isProcessing = data.getInt(data.getColumnIndexOrThrow(TrackContract.TrackData.IS_PROCESSING)) != 0;

                    AudioItem audioItem = new AudioItem();
                    audioItem.setId(_id).
                            setTitle(title).
                            setArtist(artist).
                            setAlbum(album).
                            setAbsolutePath(fullpath).
                            setChecked(isChecked).
                            setStatus(status).
                            setProcessing(isProcessing);

                    publishProgress(audioItem);

                }//end while
            }//end if
        }


        /**
         * Here we add the audio item_list to adapter
         */
        private void createAndAddAudioItem(){
            int mediaStoreId = data.getInt(0);//mediastore id
            String title = data.getString(1);
            String artist = data.getString(2);
            String album = data.getString(3);
            String fullPath = Uri.parse(data.getString(4)).toString(); //MediaStore.Audio.Media.DATA column is the file mMenuItemPath

            ContentValues values = new ContentValues();
            values.put(TrackContract.TrackData.MEDIASTORE_ID,mediaStoreId);
            values.put(TrackContract.TrackData.TITLE, title);
            values.put(TrackContract.TrackData.ARTIST, artist);
            values.put(TrackContract.TrackData.ALBUM, album);
            values.put(TrackContract.TrackData.DATA, fullPath);

            AudioItem audioItem = new AudioItem();

            //we need to set id in audio item_list because all operations
            //we do, relay in this id,
            //so when we save row to DB
            //it returns its id as a result
            long _id = MainActivity.this.mDataTrackDbHelper.insertItem(values, TrackContract.TrackData.TABLE_NAME);
            audioItem.setId(_id).setTitle(title).setArtist(artist).setAlbum(album).setAbsolutePath(fullPath);

            publishProgress(audioItem);
            values.clear();
            values = null;
        }

    }


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("SelectFolder Page") // TODO: Define a mMenuItemTitle for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    /**
     * Instance of this class handles the response from FixerTrackService
     */
    public class ResponseReceiver extends BroadcastReceiver{
        private String TAG = ResponseReceiver.class.getName();
        @Override
        public void onReceive(Context context, final Intent intent) {
            /* This can be called by an application in {@link #onReceive} to allow
             * it to keep the broadcast active after returning from that function.
             * This does <em>not</em> change the expectation of being relatively
             * responsive to the broadcast (finishing it within 10s), but does allow
             * the implementation to move work related to it over to another thread
             * to avoid glitching the main UI thread due to disk IO.
            * */
            goAsync();

            //get action and handle it
            String action = intent.getAction();
            Log.d(TAG, action);
            switch (action) {
                case Constants.GnServiceActions.ACTION_API_INITIALIZED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.api_initialized),NO_ID);
                            }
                        });
                    break;
                case Constants.Actions.ACTION_NOT_FOUND:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setNewItemValues(null, intent.getLongExtra(Constants.MEDIASTORE_ID,-1));
                        }
                    });
                    break;
                case Constants.Actions.ACTION_FAIL:
                case Constants.Actions.ACTION_DONE:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setNewItemValues((AudioItem) intent.getParcelableExtra(Constants.AUDIO_ITEM), intent.getIntExtra(Constants.MEDIASTORE_ID,-1));
                        }
                    });

                    break;
                case Constants.Actions.ACTION_SET_AUDIOITEM_PROCESSING:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setProcessingAudioItem(intent.getLongExtra(Constants.MEDIASTORE_ID,NO_ID));
                            }
                        });
                    break;
                case Constants.Actions.ACTION_CONNECTION_LOST:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.connection_lost), NO_ID);
                        }
                    });
                case Constants.Actions.ACTION_CANCEL_TASK:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.task_cancelled), NO_ID);
                        }
                    });
                case Constants.Actions.ACTION_COMPLETE_TASK:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finishTaskByUser();
                        }
                    });
                    mLocalBroadcastManager.unregisterReceiver(mReceiver);
                    break;
            }
        }
    }



    public static class Sort{
        public static String setDefaultOrder(){

            switch (Settings.SETTING_SORT){
                case R.id.path_asc:
                case 0:

                    return MediaStore.Audio.AudioColumns.DATA + " COLLATE NOCASE ASC";
                case R.id.path_desc:
                case 1:

                    return MediaStore.Audio.AudioColumns.DATA + " COLLATE NOCASE DESC";
                case R.id.title_asc:

                    return MediaStore.Audio.AudioColumns.TITLE + " COLLATE NOCASE ASC";
                case R.id.title_desc:

                    return MediaStore.Audio.AudioColumns.TITLE + " COLLATE NOCASE DESC";
                case R.id.artist_asc:

                    return MediaStore.Audio.AudioColumns.ARTIST + " COLLATE NOCASE ASC";
                case R.id.artist_desc:

                    return MediaStore.Audio.AudioColumns.ARTIST + " COLLATE NOCASE DESC";
                case R.id.album_asc:

                    return MediaStore.Audio.AudioColumns.ALBUM + " COLLATE NOCASE ASC";
                case R.id.album_desc:

                    return MediaStore.Audio.AudioColumns.ALBUM + " COLLATE NOCASE DESC";
                default:

                    return MediaStore.Audio.AudioColumns.DATA + " COLLATE NOCASE ASC";

            }
        }
    }

}
