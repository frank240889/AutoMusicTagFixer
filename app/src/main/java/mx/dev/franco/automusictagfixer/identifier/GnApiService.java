package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Franco Castillo
 */
public class GnApiService {
    public interface OnEventApiListener {
        void onApiInitialized();
        void onApiError(String error);
    }


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

    private boolean mApiInitialized = false;
    private boolean mIsInitializing = false;
    private int mCounter = 0;
    private Map<String,String> mGnStatusToDisplay;
    private GnLanguage mLanguage;
    private GnException mInitializationError;
    private List<OnEventApiListener> mListeners;

    /**
     * We don't need instances of this class
     */
    private GnApiService(Context context){
        if(mContext == null) {
            mContext = context.getApplicationContext();
            mListeners = new ArrayList<>();
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

    public void addListener(OnEventApiListener listener) {
        mListeners.add(listener);
    }
    
    /**
     * Initializes the API making a max of {@link #MAX_RETRIES}.
     */
    private void initializeAPI(){
        if(!isApiInitialized() && !isApiInitializing()) {
            initApi();
            if(isApiInitialized()) {
                mCounter = 0;
            }
            else {
                mCounter++;
                if(mCounter <= MAX_RETRIES) {
                    initializeAPI();
                }
                else {

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
            setErrorInitialization(e);
        }
    }

    private void setErrorInitialization(GnException e) {
        mInitializationError = e;
    }

    public GnException getInitializationError() {
        return mInitializationError;
    }

    public boolean isApiInitialized() {
        return mApiInitialized;
    }

    public boolean isApiInitializing() {
        return mIsInitializing;
    }

    private void setApiInitializing(boolean initializing) {
        mIsInitializing = initializing;
    }

    private void setApiInitialized(boolean initialized) {
        mApiInitialized = initialized;
    }

    private void setGnManager(GnManager manager) {
        mGnManager = manager;
    }

    public GnManager getGnManager() {
        return mGnManager;
    }

    private void setGnUser(GnUser gnUser) {
        mGnUser = gnUser;
    }

    public GnUser getGnUser() {
        return mGnUser;
    }

    private void setGnLocale(GnLocale gnLocale) {
        mGnLocale = gnLocale;
    }

    public GnLocale getGnLocale() {
        return mGnLocale;
    }

    public GnLanguage getLanguage() {
        return mLanguage;
    }

    public Map<String, String> getStates() {
        return mGnStatusToDisplay;
    }
}
