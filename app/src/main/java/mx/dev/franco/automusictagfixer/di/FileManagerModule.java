package mx.dev.franco.automusictagfixer.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.filemanager.FileManager;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.interfaces.AudioMetadataManager;
import mx.dev.franco.automusictagfixer.persistence.cache.CoverResultsCache;

@Module
public class FileManagerModule {
    @Singleton
    @Provides
    AudioMetadataManager<AudioMetadataTagger.InputParams, AudioTagger.AudioFields, AudioTagger.ResultCorrection> provideFileManager(AudioTagger tagger){
        return new AudioMetadataTagger(tagger);
    }

    @Provides
    FileManager provideCoverSaverManager(CoverResultsCache coverCache) {
        return new FileManager(coverCache);
    }
}
