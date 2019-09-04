package mx.dev.franco.automusictagfixer.identifier;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class IdentifierFactory {
    private GnService gnService;
    private AbstractSharedPreferences sharedPreferences;

    @Inject
    public IdentifierFactory(GnService gnService, AbstractSharedPreferences sharedPreferences){
        this.gnService = gnService;
        this.sharedPreferences = sharedPreferences;
    }

    public GnIdentifier create() {
        return new GnIdentifier(gnService, sharedPreferences);
    }
}
