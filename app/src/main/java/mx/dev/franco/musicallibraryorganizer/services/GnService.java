package mx.dev.franco.musicallibraryorganizer.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnStorageSqlite;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;

import mx.dev.franco.musicallibraryorganizer.utilities.Constants;

/**
 * Created by franco on 5/07/17.
 */

public class GnService{
    //We can set a sContext in static field while we call getApplicationContext() to avoid memory leak, because
    //if we use the activity sContext, this activity can remain in memory due is still in use its sContext
    private static Context sContext;
    private static GnService sGnService;
    static GnManager gnManager;
    public static GnUser gnUser;
    private static GnLocale gnLocale;
    public static final String gnsdkLicenseString = "-- BEGIN LICENSE v1.0 0B64261A --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 297011532\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE 0B64261A --\\r\\nlAADAgAeLXKgVttVCmTzGU8Lixv2VY0nKZECLARnmGWPPmpdAB8Bgp+dp5HRX8tQLJh1OvmV1ipXLqr6oy6Ds3ClSOE8\\r\\n-- END LICENSE 0B64261A --\\r\\n";//"-- BEGIN LICENSE v1.0 A75228BC --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 843162123\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE A75228BC --\\r\\nlAADAgAe/WEZPZ5IaetmxgKEpZm7EjG1SLm/yLvyhTwzlr8cAB4R2GcEuN/6PovFycqgCmnnmr3ioB/KXt3EDTz8yYk=\\r\\n-- END LICENSE A75228BC --\\r\\n";
    public static final String gnsdkClientId = "297011532";//"843162123";
    public static final String gnsdkClientTag = "6CB01DB21FA7F47FDBF1FD6DCDFA8E88";//"4E937B773F03BA431014169770593072";
    public static final String appString = "AutomaticMusicTagFixer";
    public static volatile boolean apiInitialized = false;
    public static final int API_INITIALIZED_FROM_SPLASH = 100;
    public static final int API_INITIALIZED_AFTER_CONNECTED = 101;

    /**
     * We don't need instances of this class
     */
    private GnService(Context context){
        if(sContext == null)
            sContext = context.getApplicationContext();
    }


    /**
     * Set sContext for use in GNSDK methods
     * @param appContext
     */
    public static GnService withContext(Context appContext){
        if(sGnService == null){
            sGnService = new GnService(appContext);
        }
        return sGnService;
    }

    /**
     * This method initializes the API
     */
    public void initializeAPI(final int connectedFrom){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("GNSDK","starting GNSDK");
                //We initialize the necessary objects for using the GNSDK API in a different thread for not blocking the UI
                try {
                    gnManager = new GnManager(sContext, gnsdkLicenseString, GnLicenseInputMode.kLicenseInputModeString);
                    gnUser = new GnUser(new GnUserStore(sContext), gnsdkClientId, gnsdkClientTag, appString);
                    gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic, GnLanguage.kLanguageSpanish, GnRegion.kRegionGlobal, GnDescriptor.kDescriptorDetailed, gnUser);
                    gnLocale.setGroupDefault();
                    GnStorageSqlite.enable();
                    apiInitialized = true;


                    if (connectedFrom == API_INITIALIZED_AFTER_CONNECTED){
                        Intent intent = new Intent();
                        intent.setAction(Constants.GnServiceActions.ACTION_API_INITIALIZED);
                        LocalBroadcastManager.getInstance(sContext).sendBroadcast(intent);
                        
                    }

                } catch (GnException e) {
                    e.printStackTrace();
                    Job.scheduleJob(sContext);
                }
            }
        }).start();
    }
}
