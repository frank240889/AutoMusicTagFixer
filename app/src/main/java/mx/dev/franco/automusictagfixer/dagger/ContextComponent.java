package mx.dev.franco.automusictagfixer.dagger;

import javax.inject.Singleton;

import mx.dev.franco.automusictagfixer.UI.SplashActivity;
import mx.dev.franco.automusictagfixer.UI.main.ListFragment;
import mx.dev.franco.automusictagfixer.UI.main.TrackAdapter;
import mx.dev.franco.automusictagfixer.UI.search.SearchActivity;
import mx.dev.franco.automusictagfixer.UI.search.SearchTrackAdapter;
import mx.dev.franco.automusictagfixer.UI.track_detail.TrackDetailsActivity;
import mx.dev.franco.automusictagfixer.fixer.Fixer;
import mx.dev.franco.automusictagfixer.modelsUI.main.ListViewModel;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.AsyncFileSaver;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.TrackDataLoader;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.TrackDetailInteractor;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.TrackDetailPresenter;
import mx.dev.franco.automusictagfixer.network.AsyncConnectivityDetector;
import mx.dev.franco.automusictagfixer.persistence.mediastore.AsyncFileReader;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;

/**
 * Created by Franco Castillo on 27/03/2018.
 */

@dagger.Component (modules = ContextModule.class)
@Singleton
public interface ContextComponent {
    void inject(TrackRepository trackRepository);
    void inject(ListFragment listFragment);
    void inject(ListViewModel listViewModel);
    void inject(AsyncFileReader asyncFileReader);
    void inject(FixerTrackService fixerTrackService);
    void inject(TrackAdapter trackAdapter);
    void inject(TrackDataLoader trackDataLoader);
    void inject(TrackDetailInteractor trackDetailInteractor);
    void inject(AsyncFileSaver asyncFileSaver);
    void inject(TrackDetailsActivity trackDetailsActivity);
    void inject(AsyncConnectivityDetector asyncConnectivityDetector);
    void inject(Fixer fixer);
    void inject(SplashActivity splashActivity);
    void inject(TrackDetailPresenter trackDetailPresenter);
    void inject(SearchTrackAdapter searchTrackAdapter);
    void inject(SearchActivity searchActivity);
}
