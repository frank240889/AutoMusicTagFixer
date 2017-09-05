package mx.dev.franco.musicallibraryorganizer.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Toast;

import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnString;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;
import com.gracenote.gnsdk.IGnUserStore;

/**
 * Created by franco on 5/07/17.
 */

public class GnService implements IGnUserStore{
    //We can set a context in static field while we call getApplicationContext() to avoid memory leak, because
    //if we use the activity context, this activity can remain in memory due is still in use its context
    private static Context context;
    static GnManager gnManager;
    public static GnUser gnUser;
    private static GnLocale gnLocale;
    static final String gnsdkLicenseString = "-- BEGIN LICENSE v1.0 0B64261A --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 297011532\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE 0B64261A --\\r\\nlAADAgAeLXKgVttVCmTzGU8Lixv2VY0nKZECLARnmGWPPmpdAB8Bgp+dp5HRX8tQLJh1OvmV1ipXLqr6oy6Ds3ClSOE8\\r\\n-- END LICENSE 0B64261A --\\r\\n";//"-- BEGIN LICENSE v1.0 A75228BC --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 843162123\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE A75228BC --\\r\\nlAADAgAe/WEZPZ5IaetmxgKEpZm7EjG1SLm/yLvyhTwzlr8cAB4R2GcEuN/6PovFycqgCmnnmr3ioB/KXt3EDTz8yYk=\\r\\n-- END LICENSE A75228BC --\\r\\n";
    static final String gnsdkClientId = "297011532";//"843162123";
    static final String gnsdkClientTag = "6CB01DB21FA7F47FDBF1FD6DCDFA8E88";//"4E937B773F03BA431014169770593072";
    public static final String appString = "AutomaticMusicTagFixer";
    public static volatile boolean apiInitialized = false;

    /**
     * We don't need instances of this class
     */
    private GnService(){

    }


    /**
     * Set context for use in GNSDK methods
     * @param appContext
     */
    public static void setAppContext(Context appContext){
        if(context == null)
            context = appContext.getApplicationContext();
    }

    /**
     * This method initializes the API
     */
    public static void initializeAPI(){
        new Thread(new Runnable() {
            @Override
            public void run() {

                //We initialize the necessary objects for using the GNSDK API in a different thread for not blocking the UI
                try {
                    gnManager =  new GnManager(context,gnsdkLicenseString, GnLicenseInputMode.kLicenseInputModeString);
                    gnUser = new GnUser(new GnUserStore(context),gnsdkClientId,gnsdkClientTag,appString);
                    gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic, GnLanguage.kLanguageSpanish, GnRegion.kRegionGlobal, GnDescriptor.kDescriptorDetailed,gnUser);
                    gnLocale.setGroupDefault();
                    apiInitialized = true;

                    //This handler can update UI, getting the main looper first
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(context,"API de reconocimiento inicializada.",Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER,0,0);
                            toast.show();
                        }
                    });

                } catch (GnException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        apiInitialized = false;
        context = null;
        gnLocale.delete();
        gnLocale = null;
        gnManager.delete();
        gnManager = null;
        gnUser.delete();
        gnUser = null;

    }

    @Override
    public GnString loadSerializedUser(String s) {
        return null;
    }

    @Override
    public boolean storeSerializedUser(String s, String s1) {
        return false;
    }
}
