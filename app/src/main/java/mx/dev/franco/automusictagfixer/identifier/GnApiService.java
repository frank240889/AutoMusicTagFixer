package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
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

import java.util.UUID;

import mx.dev.franco.automusictagfixer.BuildConfig;

/**
 * @author Franco Castillo
 */
public class GnApiService {
    private static GnApiService sInstance;
    private volatile boolean mApiInitialized = false;
    private volatile boolean mIsInitializing = false;
    private Context mContext;
    private GnManager mGnManager;
    private GnUser mGnUser;
    private GnLocale mGnLocale;
    private volatile GnLanguage mLanguage;
    private volatile GnException mInitializationError;

    /**
     * We don't need instances of this class
     */
    private GnApiService(Context context){
        if(mContext == null) {
            mContext = context.getApplicationContext();
        }
    }

    public static synchronized GnApiService getInstance(Context context) {
        if(sInstance == null){
            sInstance = new GnApiService(context);
        }
        return sInstance;
    }
    
    /**
     * Initializes the API making a max of {@link #MAX_RETRIES}.
     */
    private void internalInitializeAPI(@Nullable GnLanguage language){
        if(!isApiInitialized() && !isApiInitializing()) {
            initApi(language);
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
        internalInitializeAPI(language);
    }

    /**
     * Internal API initialization a setup of values required by API calls.
     */
    private void initApi(GnLanguage gnLanguage) {
        setApiInitializing(true);
        try {
            GnManager gnManager = new GnManager(mContext.getApplicationContext(),
                    BuildConfig.GNSDK_API_KEY,
                    GnLicenseInputMode.kLicenseInputModeString);
            setGnManager(gnManager);
            GnUserStore gnUserStore = new GnUserStore(
                    mContext.getApplicationContext());

            GnUser gnUser;
            GnString gnStringId = gnUserStore.loadSerializedUser(BuildConfig.GNSDK_CLIENT_ID);

            if (!gnStringId.isEmpty() && !"null".equals(gnStringId.toString())) {
                gnUser = new GnUser(gnStringId.toString());
            }
            else {
                String randomId = UUID.randomUUID().toString();
                gnUserStore.storeSerializedUser(BuildConfig.GNSDK_CLIENT_ID, randomId);
                gnUser = new GnUser(gnUserStore,
                        BuildConfig.GNSDK_CLIENT_ID,
                        BuildConfig.GNSDK_CLIENT_TAG,
                        BuildConfig.APP_STRING);
            }

            setGnUser(gnUser);
            GnLocale gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic,
                    gnLanguage,
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
}
