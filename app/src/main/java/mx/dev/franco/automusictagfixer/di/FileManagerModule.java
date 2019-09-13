package mx.dev.franco.automusictagfixer.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.FileTagger;
import mx.dev.franco.automusictagfixer.interfaces.FileManager;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.TrackDataLoader;

@Module
public class FileManagerModule {
    @Singleton
    @Provides
    FileManager<FileTagger.InputParams, TrackDataLoader.TrackDataItem, AudioTagger.ResultCorrection> provideFileManager(AudioTagger tagger){
        return new FileTagger(tagger);
    }
}
