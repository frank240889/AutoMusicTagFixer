package mx.dev.franco.automusictagfixer.UI.main;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.List;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseFragment;
import mx.dev.franco.automusictagfixer.UI.about.ScrollingAboutActivity;
import mx.dev.franco.automusictagfixer.UI.faq.QuestionsActivity;
import mx.dev.franco.automusictagfixer.UI.search.ResultSearchListFragment;
import mx.dev.franco.automusictagfixer.UI.settings.SettingsActivity;
import mx.dev.franco.automusictagfixer.identifier.GnService;
import mx.dev.franco.automusictagfixer.interfaces.OnTestingNetwork;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class MainActivity extends AppCompatActivity
        implements ResponseReceiver.OnResponse, NavigationView.OnNavigationItemSelectedListener,
        BaseFragment.OnConfirmBackPressedListener {
    public static String TAG = MainActivity.class.getName();

    //the receiver that handles the broadcasts from FixerTrackService
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ResultSearchListFragment.TAG);
        if(fragment instanceof ResultSearchListFragment) {
            ((ResultSearchListFragment) fragment).onNewIntent(intent);
        }
        else {
            //to hide it, call the method again
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            try {
                assert imm != null;
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            ResultSearchListFragment resultSearchListFragment = ResultSearchListFragment.newInstance(intent);
            getSupportFragmentManager().beginTransaction().
                    setCustomAnimations(R.anim.slide_in_right,
                            R.anim.slide_out_left, R.anim.slide_in_left,
                            R.anim.slide_out_right).
                    addToBackStack(ResultSearchListFragment.TAG).
                    add(R.id.container_fragments, resultSearchListFragment, ResultSearchListFragment.TAG).
                    commit();

        }

    }



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

    private void rescan(){
        boolean hasPermission = ContextCompat.
                checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        ListFragment listFragment = (ListFragment) getSupportFragmentManager().findFragmentByTag(ListFragment.class.getName());
        if(!hasPermission) {
            if(listFragment != null)
                listFragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
        else if(ServiceUtils.getInstance(getApplicationContext()).
                checkIfServiceIsRunning(FixerTrackService.class.getName())){
            Snackbar snackbar = AndroidUtils.getSnackbar(mDrawer, getApplicationContext());
            snackbar.setText(R.string.no_available);
            snackbar.show();
        }
        else {
            if(listFragment != null)
                listFragment.updateList();
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
    private void setupReceivers(){
        IntentFilter apiInitializedFilter = new IntentFilter(Constants.GnServiceActions.ACTION_API_INITIALIZED);
        IntentFilter connectionLostFilter = new IntentFilter(Constants.Actions.ACTION_CONNECTION_LOST);
        IntentFilter startTaskFilter = new IntentFilter(Constants.Actions.ACTION_START_TASK);
        IntentFilter showProgressFilter = new IntentFilter(Constants.Actions.START_PROCESSING_FOR);
        IntentFilter finishProcessingFilter = new IntentFilter(Constants.Actions.FINISH_TRACK_PROCESSING);
        IntentFilter completedTaskFilter = new IntentFilter(Constants.Actions.ACTION_COMPLETE_TASK);
        IntentFilter errorTask = new IntentFilter(Constants.Actions.ACTION_SD_CARD_ERROR);
        IntentFilter rescanFilter = new IntentFilter(Constants.Actions.ACTION_RESCAN);

        mReceiver = new ResponseReceiver(this, new Handler());

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        localBroadcastManager.registerReceiver(mReceiver, completedTaskFilter);
        localBroadcastManager.registerReceiver(mReceiver, showProgressFilter);
        localBroadcastManager.registerReceiver(mReceiver, connectionLostFilter);
        localBroadcastManager.registerReceiver(mReceiver, finishProcessingFilter);
        localBroadcastManager.registerReceiver(mReceiver, apiInitializedFilter);
        localBroadcastManager.registerReceiver(mReceiver, startTaskFilter);
        localBroadcastManager.registerReceiver(mReceiver, errorTask);
        localBroadcastManager.registerReceiver(mReceiver, rescanFilter);

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
        List<Fragment> fragmentList;
        switch (action) {
            case Constants.GnServiceActions.ACTION_API_INITIALIZED:
                fragmentList = getSupportFragmentManager().getFragments();
                for(Fragment fragment:fragmentList){
                    if(fragment instanceof GnService.OnApiListener){
                        ((GnService.OnApiListener) fragment).onApiInitialized();
                    }
                }

                break;
            case Constants.Actions.ACTION_CONNECTION_LOST:
                    fragmentList = getSupportFragmentManager().getFragments();
                    for(Fragment fragment:fragmentList){
                        if(fragment instanceof OnTestingNetwork.OnTestingResult){
                            ((OnTestingNetwork.OnTestingResult<Void>) fragment).
                                    onNetworkDisconnected(null);
                        }
                    }
                break;

            case Constants.Actions.ACTION_START_TASK:
                    ListFragment f1 = (ListFragment) getSupportFragmentManager().findFragmentByTag(ListFragment.class.getName());
                    if(f1 != null)
                        f1.onTaskStarted();
                break;
            case Constants.Actions.ACTION_SD_CARD_ERROR:
                    Toast toast = AndroidUtils.getToast(getApplicationContext());
                    toast.setText(intent.getStringExtra("error"));
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.show();
                break;
            case Constants.Actions.START_PROCESSING_FOR:
                ListFragment f2 = (ListFragment) getSupportFragmentManager().findFragmentByTag(ListFragment.class.getName());
                if(f2 != null)
                    f2.onStartProcessingFor(id);
                break;
            case Constants.Actions.FINISH_TRACK_PROCESSING:
                    String error = intent.getStringExtra("error");
                    if(error != null){
                        ListFragment f3 = (ListFragment) getSupportFragmentManager().findFragmentByTag(ListFragment.class.getName());
                        if(f3 != null)
                            f3.onFinishProcessing(error);
                    }
                break;
            case Constants.Actions.ACTION_COMPLETE_TASK:
                ListFragment f4 = (ListFragment) getSupportFragmentManager().findFragmentByTag(ListFragment.class.getName());
                if(f4 != null)
                    f4.onFinishTask();
                    String message = intent.getStringExtra("message");
                    toast = AndroidUtils.getToast(getApplicationContext());
                    toast.setText(message);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.show();
                    getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME,
                        Context.MODE_PRIVATE).edit().putBoolean(Constants.ALL_ITEMS_CHECKED, false).
                            apply();
                break;
            case Constants.Actions.ACTION_RESCAN:
                    rescan();
                break;
        }

    }
}
