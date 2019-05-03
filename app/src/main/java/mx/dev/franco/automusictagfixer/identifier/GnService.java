package mx.dev.franco.automusictagfixer.identifier;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.crashlytics.android.Crashlytics;
import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Settings;

/**
 * Created by franco on 5/07/17.
 */

public class GnService {

    public interface OnApiListener {
        void onApiInitialized();
        void onApiNotInitialized();
    }

    private static final java.lang.String TAG = GnService.class.getName();
    //We can set a Context in static field while we call getApplicationContext() to avoid memory leaks, because
    //if we use the activity Context, this activity can remain in memory due is still in use its Context
    private static Context sContext;
    private static GnService sGnService;
    private static GnManager sGnManager;
    private static GnUser sGnUser;
    private static GnLocale sGnLocale;

    /*Production account*/
    private static final String sGnsdkLicenseString =
    "-- BEGIN LICENSE v1.0 0B64261A --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 297011532\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE 0B64261A --\\r\\nlAADAgAeLXKgVttVCmTzGU8Lixv2VY0nKZECLARnmGWPPmpdAB8Bgp+dp5HRX8tQLJh1OvmV1ipXLqr6oy6Ds3ClSOE8\\r\\n-- END LICENSE 0B64261A --\\r\\n";   //"-- BEGIN LICENSE v1.0 A75228BC --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 843162123\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE A75228BC --\\r\\nlAADAgAe/WEZPZ5IaetmxgKEpZm7EjG1SLm/yLvyhTwzlr8cAB4R2GcEuN/6PovFycqgCmnnmr3ioB/KXt3EDTz8yYk=\\r\\n-- END LICENSE A75228BC --\\r\\n";
    private static final String sGnsdkClientId = "297011532";//"843162123";//
    private static final String sGnsdkClientTag = "6CB01DB21FA7F47FDBF1FD6DCDFA8E88";
    private static final String sAppString = "AutomaticMusicTagFixer";
    /***************************/
    private static volatile boolean sApiInitialized = false;
    private static volatile boolean sIsInitializing = false;
    private static volatile int sCounter = 0;

    /**
     * We don't need instances of this class
     */
    private GnService(Context context){
        if(sContext == null) {
            sContext = context.getApplicationContext();
        }
    }

    public static GnService getInstance() {
        return sGnService;
    }

    public static void init(Context application) {
        if(!(application instanceof Application))
            throw new IllegalArgumentException("Required app context.");

        if(sGnService == null){
            sGnService = new GnService(application);
        }
    }

    /**
     * This method initializes the API
     */
    public void initializeAPI(){
        if(!isApiInitialized() && isApiInitializing()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    initApi();
                    if(isApiInitialized()) {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(() -> handleResult(true));
                        sCounter = 0;
                    }
                    else {
                        sCounter++;
                        if(sCounter == 5) {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> handleResult(false));
                        }
                        else {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> initializeAPI());
                        }
                    }

                }
            });
            thread.start();
        }
    }

    private void handleResult(boolean b) {
        Intent intent = new Intent();
        intent.setAction(Constants.GnServiceActions.ACTION_API_INITIALIZATION_RESULT);
        intent.putExtra(Constants.GnServiceActions.INITIALIZATION_RESULT, b);
        LocalBroadcastManager.getInstance(sContext).sendBroadcast(intent);
    }

    private void initApi() {
        setApiInitializing(true);
        try {
            GnManager gnManager = new GnManager(sContext.getApplicationContext(),
                    sGnsdkLicenseString,
                    GnLicenseInputMode.kLicenseInputModeString);
            GnService.getInstance().setGnManager(gnManager);
            GnUser gnUser = new GnUser(new GnUserStore(
                    sContext.getApplicationContext()),
                    sGnsdkClientId,
                    sGnsdkClientTag,
                    GnService.sAppString);
            GnService.getInstance().setGnUser(gnUser);
            GnLocale gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic,
                    Settings.SETTING_LANGUAGE,
                    GnRegion.kRegionGlobal,
                    GnDescriptor.kDescriptorDetailed,
                    gnUser);

            gnLocale.setGroupDefault();
            GnService.getInstance().setGnLocale(gnLocale);
            GnService.getInstance().setApiInitializing(false);
            GnService.getInstance().setApiInitialized(true);
        }
        catch (GnException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            Crashlytics.log(sContext.getApplicationContext().getString(R.string.could_not_init_api));
            GnService.getInstance().setApiInitializing(false);
            GnService.getInstance().setApiInitialized(false);
        }
    }

    public synchronized boolean isApiInitialized() {
        return sApiInitialized;
    }

    public synchronized boolean isApiInitializing() {
        return sIsInitializing;
    }

    public synchronized void setApiInitializing(boolean initializing) {
        sIsInitializing = initializing;
    }

    public synchronized void setApiInitialized(boolean initialized) {
        sApiInitialized = initialized;
    }

    public synchronized void setGnManager(GnManager manager) {
        sGnManager = manager;
    }

    public synchronized GnManager getGnManager() {
        return sGnManager;
    }

    public synchronized void setGnUser(GnUser gnUser) {
        sGnUser = gnUser;
    }

    public synchronized GnUser getGnUser() {
        return sGnUser;
    }

    public synchronized void setGnLocale(GnLocale gnLocale) {
        sGnLocale = gnLocale;
    }

    public synchronized GnLocale getGnLocale() {
        return sGnLocale;
    }
}
