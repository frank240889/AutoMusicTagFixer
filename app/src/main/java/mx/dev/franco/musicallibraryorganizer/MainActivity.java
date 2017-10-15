package mx.dev.franco.musicallibraryorganizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
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
import android.webkit.MimeTypeMap;
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
import mx.dev.franco.musicallibraryorganizer.services.GnService;
import mx.dev.franco.musicallibraryorganizer.services.Job;
import mx.dev.franco.musicallibraryorganizer.utilities.CustomMediaPlayer;
import mx.dev.franco.musicallibraryorganizer.utilities.RequiredPermissions;

import static mx.dev.franco.musicallibraryorganizer.services.GnService.apiInitialized;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MediaPlayer.OnCompletionListener,
    TrackAdapter.AudioItemHolder.ClickListener {
    private static final int ID_LOADER = 0;
    public static final int MODIFY_TRACK_DATA = 50;
    public static final String MAIN_ACTION = "main_action";
    public static final String IS_PROCESSING_TASK = "is_processing_task" ;
    public static String TAG = MainActivity.class.getName();

    //static field to indicate that task must not continue in case
    //user cancel the operation
    public static volatile boolean shouldContinue = true;
    //flag to deny make concurrent tasks between automatic mode
    //and manual mode, and avoid inconsistent behaviour or data
    public volatile static boolean isProcessingTask = false;


    //flag to indicate when the app is retrieving data from Gracenote service
    public static boolean isGettingData = false;

    //Reasons why cannot execute task
    public static final int NO_INTERNET_CONNECTION = 40;
    public static final int NO_INITIALIZED_API = 41;
    public static final int PROCESSING_TASK = 42;

    //media player instance, only one is allowed
    public static CustomMediaPlayer mediaPlayer;


    //Adapter with AudioItem objects for display in recyclerview
    private TrackAdapter audioItemArrayAdapter;
    private List<AudioItem> audioItemList;

    //actions to indicate to app from where to retrieve data.
    private static final int RE_SCAN = 20;
    private static final int CREATE_DATABASE = 21;
    private static final int READ_FROM_DATABASE = 22;

    //message to user when permission to read files is not granted, or
    //in case there have no music mfiles
    private TextView searchAgainMessage;
    //search object, for search more quickly
    //any track in recyclerview list
    private SearchView searchView;
    //fab button, this executes main task: correct a bunch of selected tracks;
    //this executes the automatic mode, without intervention of user,
    //this button either can cancel the task, in case the user decide it.
    private FloatingActionButton fab;
    //swipe refresh layout for give to user the
    //facility to re scan his/her library, this is hold
    //to material design patterns
    private SwipeRefreshLayout swipeRefreshLayout;
    //recycler view used for better performance in case the
    //user has a huge musical library
    private RecyclerView recyclerView;
    //this menu has some less useful (but important) options,
    private Menu menu;
    //instance to connection do datadabse
    private DataTrackDbHelper dbHelper;
    //local broadcast to manage response from FixerTrackService.
    private LocalBroadcastManager localBroadcastManager;
    //these filters help to separate the action to take,
    //depending on response from FixerTrackService
    private IntentFilter intentFilter;
    private IntentFilter intentFilter1;
    private IntentFilter intentFilter2;
    private IntentFilter intentFilter3;
    private IntentFilter intentFilter4;
    private IntentFilter intentFilter5;
    //the receiver of responses.
    private ResponseReceiver receiver;

    private GoogleApiClient client;

    //contextual toolbar
    private Toolbar toolbar;

    //snackbar to indicate to user what is happening
    private Snackbar snackbar;
    private ActionBar actionBar;
    private static final int NO_ID = -1;
    private static TrackAdapter.Sorter sorter;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);


        setContentView(R.layout.activity_main);

        //get unique instances from database connection and media player
        dbHelper = DataTrackDbHelper.getInstance(getApplicationContext());
        mediaPlayer = CustomMediaPlayer.getInstance(getApplicationContext());
        //register this activity to receiver onCompletion event from media player
        mediaPlayer.setOnCompletionListener(this);

        //create filters to listen for response from FixerTrackService
        intentFilter = new IntentFilter(FixerTrackService.ACTION_DONE);
        intentFilter1 = new IntentFilter(FixerTrackService.ACTION_CANCEL);
        intentFilter2 = new IntentFilter(FixerTrackService.ACTION_COMPLETE_TASK);
        intentFilter3 = new IntentFilter(FixerTrackService.ACTION_FAIL);
        intentFilter4 = new IntentFilter(GnService.API_INITIALIZED);
        intentFilter5 = new IntentFilter(FixerTrackService.ACTION_SET_AUDIOITEM_PROCESSING);
        //create receiver
        receiver = new ResponseReceiver();
        //get instance of local broadcast manager
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        //toolbar for adding some actions
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //floating action button fo automatic mode
        fab = (FloatingActionButton) findViewById(R.id.fab);
        searchAgainMessage = (TextView) findViewById(R.id.genericMessage);
        //Initialize recycler view and swipe refresh layout
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);

        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getApplicationContext(),R.color.primaryColor));
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_900));

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager( new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        //hide fab and show until list has been created
        fab.hide();
        //create data source for adapter
        audioItemList = new ArrayList<>();
        audioItemArrayAdapter = new TrackAdapter(getApplicationContext(), audioItemList,this);
        sorter = TrackAdapter.Sorter.getInstance();

        //pass a referecne to data source to media player
        mediaPlayer.setAdapter(audioItemArrayAdapter);
        //create snack bar for messages
        createSnackBar();

        //set adapter to our recyclerview
        recyclerView.setAdapter(audioItemArrayAdapter);
        //Lets implement functionality of refresh layout listener
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                searchAgainMessage.setVisibility(View.GONE);
                //If we already had the permission granted, lets go to read data from database and pass them to audioItemArrayAdapter, to show in the ListView,
                //otherwise we show the reason to access files
                if(!RequiredPermissions.ACCESS_GRANTED_FILES) {
                    showReason();
                    return;
                }
                else {

                    //if we have the permission, check Bundle object to verify if the activity comes from onPause or from onCreate
                    if(savedInstanceState == null){
                        //int taskType = DataTrackDbHelper.existDatabase(getApplicationContext()) && dbHelper.getCount(null) > 0 ? READ_FROM_DATABASE : CREATE_DATABASE;
                        AsyncReadFile asyncReadFile = new AsyncReadFile(RE_SCAN);
                        asyncReadFile.execute();
                    }

                }
            }

        });



        //If we already had the permission granted, lets go to read data from database and pass them to audioItemArrayAdapter, to show in Recyclerview,
        //otherwise we show the reason to access files

        if(!RequiredPermissions.ACCESS_GRANTED_FILES) {
            showReason();
        }
        else {
            //if we have the permission, check Bundle object to verify if the activity comes from onPause or from onCreate
            //if(savedInstanceState == null){
                recyclerView.setAdapter(audioItemArrayAdapter);
                int taskType = DataTrackDbHelper.existDatabase(this) && dbHelper.getCount(null) > 0 ? READ_FROM_DATABASE : CREATE_DATABASE;

                AsyncReadFile asyncReadFile = new AsyncReadFile(taskType);
                asyncReadFile.execute();
            //}
        }

    Log.d(IS_PROCESSING_TASK,isProcessingTask+"");
        //setup action to fab depending on isProcessingTask flag
        if(isProcessingTask){
           startTask();
        }
        else {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupActionFloatingActionButton();
                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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

        //cancel all notifications when activity is in foreground
        NotificationManager nManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        nManager.cancelAll();

        //register filters to listen for response from FixerTrackService
        localBroadcastManager.registerReceiver(receiver,intentFilter);
        localBroadcastManager.registerReceiver(receiver,intentFilter1);
        localBroadcastManager.registerReceiver(receiver,intentFilter2);
        localBroadcastManager.registerReceiver(receiver,intentFilter3);
        localBroadcastManager.registerReceiver(receiver,intentFilter4);
        localBroadcastManager.registerReceiver(receiver,intentFilter5);
        Log.d(TAG,"onStart");

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        //if state was previously saved, restore it
        isProcessingTask = savedInstanceState.getBoolean("processing_task",false);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTask();
            }
        });

        if(recyclerView.getAdapter() == null)
            recyclerView.setAdapter(audioItemArrayAdapter);
        Log.d(TAG,"onRestoreInstanceState");

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
        //Deregister filters to listen for response from FixerTrackService
        //and save resources, if service is not processing any task
        if(!isProcessingTask)
            localBroadcastManager.unregisterReceiver(receiver);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        Log.d(TAG,"onSaveInstanceState");
        //savedInstanceState.putBoolean("createItemList", false);
        //savedInstanceState.putParcelable("state_list", recyclerView.getLayoutManager().onSaveInstanceState());
        //save state
        savedInstanceState.putBoolean(IS_PROCESSING_TASK,isProcessingTask);
        super.onSaveInstanceState(savedInstanceState);

    }

    @Override
    public void onStop() {
        Log.d(TAG,"onStop");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
        //save state in case the user closes the app
        SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(IS_PROCESSING_TASK,isProcessingTask);
        editor.apply();
        //if currently is processing correction, make visible a notification
        //and make a service a foreground service
        if(isServiceRunning()) {
            Intent intent = new Intent(this, FixerTrackService.class);
            intent.setAction(FixerTrackService.ACTION_SHOW_NOTIFICATION);
            startService(intent);
        }

        super.onStop();
    }

    @Override
    public void onDestroy(){
        Log.d(TAG,"onDestroy");
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

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setVisibility(View.GONE);


        // Define the listener
        MenuItemCompat.OnActionExpandListener expandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {

                if(swipeRefreshLayout != null) {
                    swipeRefreshLayout.setEnabled(true);
                }

                if(audioItemArrayAdapter != null) {
                    audioItemArrayAdapter.getFilter().filter("");
                }
                if(audioItemList.size() != 0)
                    fab.show();
                searchView.setOnQueryTextListener(null);
                return true;  // Return true to collapse action swipeRefreshLayout
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {

                if(audioItemList.size() <= 0){
                    showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.no_items_found),NO_ID);
                    return false;
                }

                if (swipeRefreshLayout != null){
                    swipeRefreshLayout.setEnabled(false);
                }

                if(isGettingData){
                    showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.getting_data),NO_ID);
                    return false;
                }

                fab.hide();

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if(audioItemArrayAdapter != null) {
                            audioItemArrayAdapter.getFilter().filter(newText);
                        }
                        else {
                            showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.no_items_found),-1);
                        }
                        return true;
                    }
                });
                return true;  // Return true to expand action swipeRefreshLayout
            }
        };

        // Assign the listener to that action item_list
        MenuItemCompat.setOnActionExpandListener(searchItem, expandListener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item_list clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement

        switch (id){
            case R.id.select_all:
                //wait until processing finish
                if(isProcessingTask){
                    showSnackBar(Snackbar.LENGTH_LONG,getString(R.string.processing_task),-1);
                    return false;
                }

                selectAllItems();
                break;
            case R.id.go_to_current_playback:

                //no items found
                if(audioItemList.size() <= 0){
                    showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.no_items_found),NO_ID);
                    return false;
                }

                //jump to current item_list playing
                if(mediaPlayer != null && mediaPlayer.isPlaying() && audioItemArrayAdapter.getItemCount() > 0){
                    recyclerView.smoothScrollToPosition(mediaPlayer.getCurrentAudioItem().getPosition());
                }
                else {
                    showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.empty_list),-1);
                }
                break;

            case R.id.action_refresh:
                //request permission in case is necessary and update
                //our database
                if(RequiredPermissions.ACCESS_GRANTED_FILES) {
                    AsyncReadFile asyncReadFile = new AsyncReadFile(RE_SCAN);
                    asyncReadFile.execute();
                }
                else {
                    showReason();
                }
                break;
            case R.id.path_order:
                sorter.setSortParams(TrackContract.TrackData.DATA, 0);
                Collections.sort(audioItemList,sorter);
                audioItemArrayAdapter.notifyDataSetChanged();
                break;
            case R.id.title_order:
                sorter.setSortParams(TrackContract.TrackData.TITLE, 0);
                Collections.sort(audioItemList,sorter);
                audioItemArrayAdapter.notifyDataSetChanged();
                break;
            case R.id.artist_order:
                sorter.setSortParams(TrackContract.TrackData.ARTIST, 0);
                Collections.sort(audioItemList,sorter);
                audioItemArrayAdapter.notifyDataSetChanged();
                break;
            case R.id.album_order:
                sorter.setSortParams(TrackContract.TrackData.ALBUM, 0);
                Collections.sort(audioItemList,sorter);
                audioItemArrayAdapter.notifyDataSetChanged();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation swipeRefreshLayout item_list clicks here.

        int id = item.getItemId();

        if (id == R.id.rate) {
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.in_development),NO_ID);
        } else if (id == R.id.share) {
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.in_development),NO_ID);
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
            swipeRefreshLayout.setRefreshing(false);
            searchAgainMessage.setText(R.string.swipe_up_search);
            searchAgainMessage.setVisibility(View.VISIBLE);
            fab.hide();
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
                onClickCoverArt(view, position, false);
                break;
            case R.id.checkBoxTrack:
                selectItem((long)view.getTag(), view, position);
                break;
            case R.id.playButton:

                try {
                    AudioItem audioItem = audioItemArrayAdapter.getItemByIdOrPath((long) view.findViewById(R.id.playButton).getTag(), null);
                    if(!AudioItem.checkFileIntegrity(audioItem.getAbsolutePath())){
                        showConfirmationDialog(audioItem.getAbsolutePath(),position);
                        return;
                    }
                    mediaPlayer.playPreview(audioItem.getId());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
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

    /**
     * Handles onCompletion event fired from media player
     * when item_list that is playing ends.
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        AudioItem audioItem = audioItemArrayAdapter.getItemByIdOrPath(mediaPlayer.getCurrentId(),null);
        audioItem.setPlayingAudio(false);
        mediaPlayer.onCompletePlayback();
    }

    /**
     * Sets the action to floating action button (fab)
     */
    private void setupActionFloatingActionButton(){
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        else {

            //Automatic mode require some conditions to execute
            int canContinue = allowExecute(MainActivity.this);
            if(canContinue != 0) {
                showSnackBar(canContinue);
                return;
            }

            if(getCountSelectedItems() == 0){
                showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.no_songs_to_correct), NO_ID);
                return;
            }



            //start correction in automatic mode
            Intent intent = new Intent(MainActivity.this, FixerTrackService.class);
            intent.putExtra(FixerTrackService.SINGLE_TRACK, false);
            intent.putExtra(FixerTrackService.FROM_EDIT_MODE, false);
            startService(intent);
            startTask();

        }
    }

    /**
     * This method creates a general snackbar,
     * for recycle its use
     */
    private void createSnackBar() {

        snackbar = Snackbar.make(swipeRefreshLayout,"",Snackbar.LENGTH_SHORT);
        TextView tv = (TextView) this.snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);

            snackbar.getView().setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.primaryLightColor));
            tv.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));
            snackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800));

    }

    /**
     * @param duration
     * @param msg
     * @param id
     */
    private void showSnackBar(int duration, String msg, final long id){

        if(snackbar != null) {
            snackbar = null;
            createSnackBar();
        }

        //no action if no ID
        if(id == -1){
            snackbar.setText(msg);
            snackbar.setDuration(duration);
            snackbar.setAction("",null);
        }
        else {
            //setaction if id != -1
            snackbar.setText(msg);
            snackbar.setDuration(duration);
            snackbar.setAction(R.string.manual_mode, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent2 = new Intent(MainActivity.this, TrackDetailsActivity.class);
                    intent2.putExtra("itemId",id);
                    intent2.putExtra("manualMode",true);
                    startActivity(intent2);
                }
            });
        }

        snackbar.show();
    }

    /**
     * checks if FixerTrackService is running
     * @return
     */
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(FixerTrackService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return the number of elements marked
     *          as selected
     */
    private int getCountSelectedItems(){
        int numberOfSelectedItems = 0;
        if(audioItemArrayAdapter != null && audioItemArrayAdapter.getItemCount() >0) {
            for (int t = 0; t < audioItemArrayAdapter.getItemCount(); t++) {
                if (audioItemList.get(t).isChecked()) {
                    numberOfSelectedItems++;
                }
            }
        }

        return numberOfSelectedItems;
    }

    /**
     * Some actions require that some conditions are true
     * this method verifies these conditions
     * @param mContext
     * @return
     */
    public static int allowExecute(Context mContext){
        Context context = mContext.getApplicationContext();
        //No internet connection
        if(!DetectorInternetConnection.isConnected(context)){
            return NO_INTERNET_CONNECTION;
        }

        //API not initialized
        if(!apiInitialized){
            Job.scheduleJob(context);
            return NO_INITIALIZED_API;
        }

        //Task is already executing
        if(isProcessingTask){
            return PROCESSING_TASK;
        }

        return 0;
    }

    /**
     * This method mark as select all
     * items in recycler view
     */
    private void selectAllItems(){

        if(isProcessingTask){
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.processing_task),-1);
            return;
        }

        if(audioItemList.size() == 0 ){
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.no_items), NO_ID);
            return;
        }

        final boolean areAllSelected = audioItemArrayAdapter.areAllSelected();

        //database operation can block UI if we run on Main process
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(TrackContract.TrackData.IS_SELECTED,!areAllSelected);
                dbHelper.updateData(values);
            }
        }).start();

        for(int f = 0; f< audioItemArrayAdapter.getItemCount() ; f++){
            audioItemList.get(f).setChecked(!areAllSelected);
            audioItemArrayAdapter.notifyItemChanged(f);
        }

        audioItemArrayAdapter.setAllSelected(!areAllSelected);

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

        final AudioItem audioItem = audioItemArrayAdapter.getItemByIdOrPath(NO_ID, absolutePath);

        //wait until service finish correction to this track
        if(isProcessingTask){
            showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.processing_task), NO_ID);
            return;
        }



        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm_correction_title)).setMessage(getString(R.string.confirm_correction) + " " + AudioItem.getFilename(absolutePath) + "?")
                .setNegativeButton(getString(R.string.manual_mode), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickCoverArt(getView, position, true);
                    }
                })
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        int canContinue = allowExecute(MainActivity.this);
                        if(canContinue != 0) {
                            showSnackBar(canContinue);
                            return;
                        }


                        audioItem.setProcessing(true);
                        audioItemArrayAdapter.notifyItemChanged(position);
                        Intent intent = new Intent(MainActivity.this, FixerTrackService.class);
                        intent.putExtra(FixerTrackService.FROM_EDIT_MODE,false);
                        intent.putExtra(FixerTrackService.AUDIO_ITEM, audioItem);
                        startService(intent);
                        startTask();

                    }
                });
        final AlertDialog dialog =  builder.create();
        dialog.show();


    }

    public void cancelProcessing(){
    }


    /**
     * Shows a confirmation dialog when user is going to cancel current task
     * @param absolutePath
     * @param position
     */
    private void showConfirmationDialog(final String absolutePath, final int position){
        final AudioItem audioItem = audioItemArrayAdapter.getItemByIdOrPath(NO_ID, absolutePath);

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
                audioItemList.remove(audioItem);
                audioItemArrayAdapter.notifyItemRemoved(position);
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
     * @param manualMode
     */
    public void onClickCoverArt(View view, int position, boolean manualMode){
        String path = "";

        path = (String) ((View)view.getParent()).findViewById(R.id.absolute_path).getTag();

        if(!AudioItem.checkFileIntegrity(path)){
            showConfirmationDialog(path,position);
            return;
        }


        AudioItem audioItem = audioItemArrayAdapter.getItemByIdOrPath(NO_ID, path);

        if(audioItem.isProcessing()){
            showSnackBar(Snackbar.LENGTH_LONG, getString(R.string.current_file_processing),NO_ID);
            return;
        }

        Intent intent = new Intent(this, TrackDetailsActivity.class);
        intent.putExtra(FixerTrackService.MEDIASTORE_ID, audioItem.getId());
        intent.putExtra(FixerTrackService.MANUAL_MODE, manualMode);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                MainActivity.this,
                view,
                ViewCompat.getTransitionName(view));

        startActivity(intent, options.toBundle());
    }

    /**
     * This method marks as true selected column in database
     * and checks the audioitem checkbox
     * @param id
     * @param view
     * @param position
     */
    private void selectItem(final long id, final View view, final int position){
        AudioItem audioItem = audioItemArrayAdapter.getItemByIdOrPath(id, null);
        Log.d("path", audioItem.getAbsolutePath());
        if(!AudioItem.checkFileIntegrity(audioItem.getAbsolutePath())){
            showConfirmationDialog(audioItem.getAbsolutePath(),position);
            return;
        }

        final CheckBox checkBox = (CheckBox) view;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor data = dbHelper.getDataRow(id);
                ContentValues newValues = null;
                if(data != null){
                    data.moveToFirst();

                    boolean isChecked = data.getInt(data.getColumnIndexOrThrow(TrackContract.TrackData.IS_SELECTED)) != 0;

                    AudioItem audioItem = audioItemArrayAdapter.getItemByIdOrPath(id, "");

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
                    dbHelper.updateData(id, newValues);
                    isChecked = data.getInt(data.getColumnIndexOrThrow(TrackContract.TrackData.IS_SELECTED)) != 0;
                    newValues.clear();
                    data.close();
                    data = null;

                    System.gc();

                }

            }

        }).start();


    }

    private void executeScan(){
        if(recyclerView.getAdapter() == null)
            recyclerView.setAdapter(audioItemArrayAdapter);
        //if previously was granted the permission and our database has data
        //dont' clear that data, only read it instead.
        int taskType = DataTrackDbHelper.existDatabase(getApplicationContext()) && dbHelper.getCount(null) > 0 ? READ_FROM_DATABASE : CREATE_DATABASE;
        AsyncReadFile asyncReadFile = new AsyncReadFile(taskType);
        asyncReadFile.execute();

    }


    private void askForPermission(String permission) {

        switch (permission){
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                //No permission? then ask it
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{permission}, RequiredPermissions.READ_EXTERNAL_STORAGE_PERMISSION);
                }
                else {
                    executeScan();
                }
            break;
        }


    }

    private void startTask(){
        this.fab.setOnClickListener(null);
        this.fab.setImageDrawable(getDrawable(R.drawable.ic_stop_white_24dp));
        this.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStartTaskStateFab();
            }
        });
    }

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

                        Intent intent = new Intent(MainActivity.this, FixerTrackService.class);
                        intent.setAction(FixerTrackService.ACTION_CANCEL);
                        startService(intent);
                    }
                });
        final AlertDialog dialog =  builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void finishTaskByUser(){
        MainActivity.this.fab.setOnClickListener(null);
        MainActivity.this.fab.setImageDrawable(getDrawable(R.drawable.ic_magic_wand_auto_fix_button));
        MainActivity.this.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupActionFloatingActionButton();
            }
        });
        isProcessingTask = false;
        SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(IS_PROCESSING_TASK,isProcessingTask);
        editor.apply();
    }

    private void showSnackBar(int reason){
        String msg = "";
        switch (reason){
            case MainActivity.NO_INTERNET_CONNECTION:
                msg = getString(R.string.no_internet_connection_automatic_mode);
                break;
            case MainActivity.PROCESSING_TASK:
                msg = getString(R.string.processing_task);
                break;
            case MainActivity.NO_INITIALIZED_API:
                msg = getString(R.string.initializing_recognition_api);
                break;
        }
        showSnackBar(Snackbar.LENGTH_SHORT, msg,-1);
    }

    private void setFilePermissionGranted(){
        SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("accessFilesPermission", true);
        editor.apply();
        RequiredPermissions.ACCESS_GRANTED_FILES = true;
        editor = null;
        sharedPreferences = null;

    }

    private void showReason(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_permision).setMessage(R.string.explanation_permission_access_files);
        builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchAgainMessage.setVisibility(View.VISIBLE);
                searchAgainMessage.setText(R.string.swipe_up_search);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        final AlertDialog dialog =  builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setAudioItem(AudioItem audioItem){
        AudioItem currentAudioItem = audioItemArrayAdapter.getItemByIdOrPath(audioItem.getId(), null);
        if(currentAudioItem != null) {
            currentAudioItem.setId(audioItem.getId());
            currentAudioItem.setTitle(audioItem.getTitle());
            currentAudioItem.setArtist(audioItem.getArtist());
            currentAudioItem.setAlbum(audioItem.getAlbum());
            currentAudioItem.setAbsolutePath(audioItem.getAbsolutePath());
            currentAudioItem.setStatus(audioItem.getStatus());
            currentAudioItem.setChecked(false);
            currentAudioItem.setProcessing(false);
            audioItemArrayAdapter.notifyItemChanged(currentAudioItem.getPosition());
            currentAudioItem = null;
        }
    }

    private void setProcessingAudioItem(long id){
        AudioItem audioItem = audioItemArrayAdapter.getItemByIdOrPath(id,null);
        audioItem.setProcessing(true);
        recyclerView.smoothScrollToPosition(audioItem.getPosition());
        audioItemArrayAdapter.notifyItemChanged(audioItem.getPosition());
    }

    private void updateNumberItems(){
        if(audioItemList != null){
            actionBar.setTitle(audioItemList.size() + " " +getString(R.string.tracks));
        }
    }

    private class AsyncReadFile extends AsyncTask<Void, AudioItem, Void> {
        private int taskType;
        private Cursor data;
        private int added = 0;
        private int removed = 0;

        AsyncReadFile(int codeTaskType){
            this.taskType = codeTaskType;

            if(codeTaskType == CREATE_DATABASE){
                MainActivity.this.dbHelper.clearDb();
            }
        }

        private String setDefaultOrder(){
            String order = null;

            switch (SelectedOptions.DEFAULT_SORT){
                case 0:
                    order = MediaStore.Audio.AudioColumns.DATA;
                    break;
                case 1:
                    order = MediaStore.Audio.AudioColumns.TITLE;
                    break;
                case 2:
                    order = MediaStore.Audio.AudioColumns.ARTIST;
                    break;
                case 3:
                    order = MediaStore.Audio.AudioColumns.ALBUM;
                    break;
                default:
                    order = MediaStore.Audio.AudioColumns.DATA;
                    break;

            }

            return order;
        }

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
            isGettingData = true;
            if(searchView != null){
                searchView.setVisibility(View.GONE);
            }

            if(taskType == CREATE_DATABASE) {
                showSnackBar(Snackbar.LENGTH_SHORT,getString(R.string.getting_data),NO_ID);
                searchAgainMessage.setVisibility(View.GONE);
            }

        }

        @Override
        protected Void doInBackground(Void... voids) {

            switch (taskType){
                //If we are updating for new elements added
                case RE_SCAN:
                    removeUnusedItems();
                    rescanAndUpdateList();
                    break;
                //if database does not exist
                case CREATE_DATABASE:
                    createNewTable();
                    break;
                //if we are reading data from database
                case READ_FROM_DATABASE:
                    removeUnusedItems();
                    readFromDatabase();
                    break;
            }

            //close cursor
            if(this.data != null) {
                data.close();
                data = null;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(AudioItem... audioItems) {
            super.onProgressUpdate(audioItems);
            if(taskType == RE_SCAN){
                audioItemList.add(audioItems[0]);
                audioItemArrayAdapter.notifyItemInserted(0);
            }
            else {
                audioItemList.add(audioItems[0]);
                audioItemArrayAdapter.notifyItemInserted(audioItemList.size()-1);
            }

            updateNumberItems();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.setEnabled(true);

            isGettingData = false;

            //there are not songs?
            if(audioItemList.size() == 0){
                MainActivity.this.searchAgainMessage.setText(getString(R.string.no_items_found));
                MainActivity.this.searchAgainMessage.setVisibility(View.VISIBLE);
                return;
            }

            if(removed > 0){
                showSnackBar(Toast.LENGTH_SHORT,removed + " " + getString(R.string.removed_inexistent),NO_ID);
            }

            if(added > 0 ){
                showSnackBar(Toast.LENGTH_SHORT,added + " " + getString(R.string.new_files_found),NO_ID);
                recyclerView.smoothScrollToPosition(0);
            }

            if(searchView != null){
                searchView.setVisibility(View.VISIBLE);
            }
            updateNumberItems();
            MainActivity.this.fab.show();


            System.gc();
        }

        @Override
        public void onCancelled(){
            super.onCancelled();
            isGettingData = false;
            swipeRefreshLayout.setEnabled(true);
            swipeRefreshLayout.setRefreshing(false);
            updateNumberItems();
            if(searchView != null){
                searchView.setVisibility(View.VISIBLE);
            }
        }

        /**
         * This method gets data from media store,
         * only mp3 files data is retrieved
         * @return
         */
        private Cursor getDataFromDevice() {

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getString(R.string.mp3_type));
            //Only select mp3 music files from this content provider
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0" + " and "
                    + MediaStore.Audio.Media.MIME_TYPE + " = " + " \'" +mimeType + "\'";

            String[] projection = { //Columns to retrieve
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.AlbumColumns.ALBUM ,
                    MediaStore.Audio.Media.DATA // absolute path to audio file
            };
            //get data from content provider
            //the last parameter sorts the data alphanumerically by the "DATA" field
            return getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    setDefaultOrder());
        }

        /**
         * This method rescan mediastore DB, and
         * if there are new elements, add them to list
         */
        private void rescanAndUpdateList(){
            data = getDataFromDevice();
            while(data.moveToNext()){
                boolean existInTable = MainActivity.this.dbHelper.existInDatabase(data.getInt(0));

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

        private void removeUnusedItems(){
            for(int pos = 0 ; pos < audioItemList.size() ; pos++){
                AudioItem audioItem = audioItemList.get(pos);
                File file = new File(audioItem.getAbsolutePath());
                if (!file.exists()) {
                    MainActivity.this.dbHelper.removeItem(audioItem.getId(), TrackContract.TrackData.TABLE_NAME);
                    audioItemList.remove(pos);
                    audioItemArrayAdapter.notifyItemRemoved(pos);
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
         * This method read data from our
         * DB created after the first use of app,
         * then passes this data to adapter
         */
        private void readFromDatabase(){

            data = MainActivity.this.dbHelper.getDataFromDB(setDefaultOrder());
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
         * created at onCreated callback from
         * parent activity.
         */
        private void createAndAddAudioItem(){
            int mediaStoreId = data.getInt(0);//mediastore id
            String title = data.getString(1);
            String artist = data.getString(2);
            String album = data.getString(3);
            String fullPath = Uri.parse(data.getString(4)).toString(); //MediaStore.Audio.Media.DATA column is the file path

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
            long _id = MainActivity.this.dbHelper.insertItem(values, TrackContract.TrackData.TABLE_NAME);
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
                .setName("SelectFolder Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    public class ResponseReceiver extends BroadcastReceiver{
        private String TAG = ResponseReceiver.class.getName();
        @Override
        public void onReceive(Context context, final Intent intent) {
            goAsync();
            String action = intent.getAction();
            Log.d(TAG,action);
            if(action.equals(GnService.API_INITIALIZED)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSnackBar(Snackbar.LENGTH_SHORT, getString(R.string.api_initialized),NO_ID);
                    }
                });

            }
            else {

                switch (action) {
                    case FixerTrackService.ACTION_DONE:
                        AudioItem audioItem = intent.getParcelableExtra(FixerTrackService.AUDIO_ITEM);
                        setAudioItem(audioItem);
                        break;
                    case FixerTrackService.ACTION_SET_AUDIOITEM_PROCESSING:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setProcessingAudioItem(intent.getLongExtra(FixerTrackService.MEDIASTORE_ID,NO_ID));
                            }
                        });
                        break;
                    case FixerTrackService.ACTION_CANCEL:
                        audioItem = intent.getParcelableExtra(FixerTrackService.AUDIO_ITEM);
                        setAudioItem(audioItem);
                    case FixerTrackService.ACTION_COMPLETE_TASK:
                    case FixerTrackService.ACTION_FAIL:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finishTaskByUser();
                            }
                        });

                        break;
                }


            }
        }
    }

}
