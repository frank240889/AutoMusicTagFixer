package mx.dev.franco.automusictagfixer.identifier;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

public class IdentifierFactory {
    public static final int FINGERPRINT_IDENTIFIER = 1;
    public static final int METADATA_IDENTIFIER = 2;

    private GnApiService gnApiService;
    private ResourceManager resourceManager;

    @Inject
    public IdentifierFactory(GnApiService gnApiService, ResourceManager resourceManager){
        this.gnApiService = gnApiService;
        this.resourceManager = resourceManager;
    }

    @Nullable
    public Identifier<Map<String, String>, List<? extends Identifier.IdentificationResults>> create(int identifierType) {
        if(identifierType == FINGERPRINT_IDENTIFIER)
            return new AudioFingerprintIdentifier(gnApiService, resourceManager);
        else
            return null;
    }
}
