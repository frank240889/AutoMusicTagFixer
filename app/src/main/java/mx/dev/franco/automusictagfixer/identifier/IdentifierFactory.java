package mx.dev.franco.automusictagfixer.identifier;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class IdentifierFactory {
    public static final int FINGERPRINT_IDENTIFIER = 1;
    public static final int METADATA_IDENTIFIER = 2;

    private GnApiService gnApiService;
    private AbstractSharedPreferences sharedPreferences;

    @Inject
    public IdentifierFactory(GnApiService gnApiService, AbstractSharedPreferences sharedPreferences){
        this.gnApiService = gnApiService;
        this.sharedPreferences = sharedPreferences;
    }

    @Nullable
    public Identifier<Track, List<Identifier.IdentificationResults>> create(int identifierType) {
        if(identifierType == FINGERPRINT_IDENTIFIER)
            return new GnIdentifier(gnApiService, sharedPreferences);
        else
            return null;
    }
}
