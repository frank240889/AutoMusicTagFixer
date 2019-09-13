package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;

import java.util.Map;

/**
 * @author Franco Castillo
 */
public class GnApiService {
    public static final String BEGIN_PROCESSING = "kMusicIdFileCallbackStatusProcessingBegin";
    public static final String QUERYING_INFO = "kMusicIdFileCallbackStatusFileInfoQuery";
    public static final String COMPLETE_IDENTIFICATION = "kMusicIdFileCallbackStatusProcessingComplete";
    public static final String STATUS_ERROR = "kMusicIdFileCallbackStatusError";
    public static final String STATUS_PROCESSING_ERROR = "kMusicIdFileCallbackStatusProcessingError";
    public static final String BEGIN_PROCESSING_MSG = "Iniciando identificación...";
    public static final String QUERYING_INFO_MSG = "Identificando, espere por favor...";
    public static final String COMPLETE_IDENTIFICATION_MSG = "Identificación completa";
    public static final String STATUS_ERROR_MSG = "Error";
    public static final String STATUS_PROCESSING_ERROR_MSG = "Error al procesar ";
    private static final int MAX_RETRIES = 5;

    private Context mContext;
    private GnManager mGnManager;
    private GnUser mGnUser;
    private GnLocale mGnLocale;
    private static GnApiService sInstance;

    /******************Data required by API*****************************/
    private static final String sGnsdkLicenseString =
    "-- BEGIN LICENSE v1.0 0B64261A --\\r\\nname: \\r\\nnotes: " +
            "Gracenote Open Developer Program\\r\\nstart_date: " +
            "0000-00-00\\r\\nclient_id: 297011532\\r\\nmusicid_file: " +
            "enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: " +
            "enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: " +
            "enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: " +
            "enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: " +
            "enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: " +
            "enabled\\r\\n-- SIGNATURE 0B64261A --\\r\\nlAADAgAeLXKgVttVCmTzGU8Lixv" +
            "2VY0nKZECLARnmGWPPmpdAB8Bgp+dp5HRX8tQLJh1OvmV1ipXLqr6oy6Ds3ClSOE8\\r\\n-- " +
            "END LICENSE 0B64261A --\\r\\n";

    private static final String sGnsdkClientId = "297011532";
    private static final String sGnsdkClientTag = "6CB01DB21FA7F47FDBF1FD6DCDFA8E88";
    private static final String sAppString = "AutomaticMusicTagFixer";
    /******************************************************************/

    private volatile boolean sApiInitialized = false;
    private volatile boolean sIsInitializing = false;
    private volatile int sCounter = 0;
    private Map<String,String> mGnStatusToDisplay;
    private GnLanguage mLanguage;

    /**
     * We don't need instances of this class
     */
    private GnApiService(Context context){
        if(mContext == null) {
            mContext = context.getApplicationContext();
            initStates();
        }
    }

    private void initStates() {
        mGnStatusToDisplay = new ArrayMap<>();
        mGnStatusToDisplay.put(BEGIN_PROCESSING,BEGIN_PROCESSING_MSG);
        mGnStatusToDisplay.put(QUERYING_INFO,QUERYING_INFO_MSG);
        mGnStatusToDisplay.put(COMPLETE_IDENTIFICATION,COMPLETE_IDENTIFICATION_MSG);
        mGnStatusToDisplay.put(STATUS_ERROR,STATUS_ERROR_MSG);
        mGnStatusToDisplay.put(STATUS_PROCESSING_ERROR,STATUS_PROCESSING_ERROR_MSG);
    }

    public static synchronized GnApiService getInstance(Context context) {
        if(sInstance == null){
            sInstance = new GnApiService(context);
        }
        return sInstance;
    }

    @Deprecated
    public static void init(Context application) {

    }

    /**
     * Initializes the API making a max of {@link #MAX_RETRIES}.
     */
    public synchronized void initializeAPI(){
        if(!isApiInitialized() && !isApiInitializing()) {
            initApi();
            if(isApiInitialized()) {
                sCounter = 0;
            }
            else {
                sCounter++;
                if(sCounter <= MAX_RETRIES) {
                    initializeAPI();
                }
            }
        }
    }

    /**
     * Initialization of API with the desired delivery language results.
     * @param language
     */
    public synchronized void initializeAPI(@Nullable GnLanguage language){
        if(language == null)
            mLanguage = GnLanguage.kLanguageSpanish;
        else
            mLanguage = language;
        initializeAPI();
    }

    /**
     * Internal API initialization a setup of values required by API calls.
     */
    private void initApi() {
        setApiInitializing(true);
        try {
            GnManager gnManager = new GnManager(mContext.getApplicationContext(),
                    sGnsdkLicenseString,
                    GnLicenseInputMode.kLicenseInputModeString);
            setGnManager(gnManager);
            GnUser gnUser = new GnUser(new GnUserStore(
                    mContext.getApplicationContext()),
                    sGnsdkClientId,
                    sGnsdkClientTag,
                    GnApiService.sAppString);
            setGnUser(gnUser);
            GnLocale gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic,
                    mLanguage,
                    GnRegion.kRegionGlobal,
                    GnDescriptor.kDescriptorDetailed,
                    gnUser);

            gnLocale.setGroupDefault();
            setGnLocale(gnLocale);
            setApiInitializing(false);
            setApiInitialized(true);
        }
        catch (GnException e) {
            Crashlytics.logException(e);
            Crashlytics.log("Could not initialize API: " + e.toString());
            Log.w(getClass().getName(), "Could not initialize API: " + e.toString());
            setApiInitializing(false);
            setApiInitialized(false);
        }
    }

    public synchronized boolean isApiInitialized() {
        return sApiInitialized;
    }

    public synchronized boolean isApiInitializing() {
        return sIsInitializing;
    }

    private synchronized void setApiInitializing(boolean initializing) {
        sIsInitializing = initializing;
    }

    private synchronized void setApiInitialized(boolean initialized) {
        sApiInitialized = initialized;
    }

    private synchronized void setGnManager(GnManager manager) {
        mGnManager = manager;
    }

    public synchronized GnManager getGnManager() {
        return mGnManager;
    }

    private synchronized void setGnUser(GnUser gnUser) {
        mGnUser = gnUser;
    }

    public synchronized GnUser getGnUser() {
        return mGnUser;
    }

    private synchronized void setGnLocale(GnLocale gnLocale) {
        mGnLocale = gnLocale;
    }

    public synchronized GnLocale getGnLocale() {
        return mGnLocale;
    }

    public GnLanguage getLanguage() {
        return mLanguage;
    }

    public Map<String, String> getStates() {
        return mGnStatusToDisplay;
    }
}
