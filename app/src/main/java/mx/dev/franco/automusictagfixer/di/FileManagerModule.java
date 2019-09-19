package mx.dev.franco.automusictagfixer.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.interfaces.AudioMetadataManager;

@Module
public class FileManagerModule {
    @Singleton
    @Provides
    AudioMetadataManager<AudioMetadataTagger.InputParams, AudioTagger.AudioFields, AudioTagger.ResultCorrection> provideFileManager(AudioTagger tagger){
        return new AudioMetadataTagger(tagger);
    }
}
