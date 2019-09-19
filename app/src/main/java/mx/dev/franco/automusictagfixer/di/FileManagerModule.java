package mx.dev.franco.automusictagfixer.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.FileTagger;
import mx.dev.franco.automusictagfixer.interfaces.FileManager;

@Module
public class FileManagerModule {
    @Singleton
    @Provides
    FileManager<FileTagger.InputParams, AudioTagger.TrackDataItem, AudioTagger.ResultCorrection> provideFileManager(AudioTagger tagger){
        return new FileTagger(tagger);
    }
}
