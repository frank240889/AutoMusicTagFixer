package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import java.util.List;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseFragment;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.utilities.Constants;

/**
 * Created by franco on 22/07/17.
 */

public class TrackDetailsActivity extends AppCompatActivity implements
        ResponseReceiver.OnResponse, BaseFragment.OnConfirmBackPressedListener {

    private static final String TAG = TrackDetailsActivity.class.getName();
    //Receiver to handle responses
    private ResponseReceiver mReceiver;
    private TrackDetailFragment mTrackDetailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AutoMusicTagFixer.getContextComponent().inject(this);
        //windows is the top level in the view hierarchy,
        //it has a single Surface in which the contents of the window is rendered
        //A Surface is an object holding pixels that are being composited to the screen.
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);
        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        //set the layout to this activity
        setContentView(R.layout.activity_track_details);
        //We get the current instance of SimpleMediaPlayer object
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        mTrackDetailFragment = TrackDetailFragment.newInstance(getIntent().
                getIntExtra(Constants.MEDIA_STORE_ID,-1),
                getIntent().getIntExtra(Constants.CorrectionModes.MODE,Constants.CorrectionModes.VIEW_INFO));
        registerReceivers();

        getSupportFragmentManager().beginTransaction().
                replace(R.id.container_fragment_detail, mTrackDetailFragment).
                commitAllowingStateLoss();

    }

    /**
     * This callback handles
     * the back button pressed from
     * Android system
     */
    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for(Fragment fragment : fragments){
            if(fragment instanceof BaseFragment){
                ((BaseFragment) fragment).onBackPressed();
                break;
            }
            else {
                super.onBackPressed();
            }
        }
    }

    /**
     * Release resources in this last callback
     * received in activity before is destroyed
     *
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        mReceiver = null;
    }

    /**
     * Register filters to handle intents from FixerTrackService
     */
    private void registerReceivers(){
        IntentFilter apiInitialized = new IntentFilter(Constants.GnServiceActions.ACTION_API_INITIALIZED);
        IntentFilter connectionLost = new IntentFilter(Constants.Actions.ACTION_CONNECTION_LOST);
        mReceiver = new ResponseReceiver(this, new Handler());
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver,apiInitialized);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver,connectionLost);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mTrackDetailFragment.onActivityResult(requestCode, resultCode, data);
    }



    @Override
    public void onResponse(Intent intent) {
        String action = intent.getAction();
        switch (action) {
            //API is no initialized
            case Constants.GnServiceActions.ACTION_API_INITIALIZED:
                mTrackDetailFragment.onApiInitialized();
                break;
            case Constants.Actions.ACTION_CONNECTION_LOST:
                mTrackDetailFragment.cancelIdentification();
                break;
            default:
                break;
        }
    }

    @Override
    public void callSuperOnBackPressed() {
        super.onBackPressed();
    }
}
