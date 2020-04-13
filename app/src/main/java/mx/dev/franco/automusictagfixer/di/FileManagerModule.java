package mx.dev.franco.automusictagfixer.di;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.filemanager.FileManager;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;

@Module
public class FileManagerModule {

    @Provides
    FileManager provideCoverSaverManager(GnApiService gnApiService) {
        return new FileManager(gnApiService);
    }
}
