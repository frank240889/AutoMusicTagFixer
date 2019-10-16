package mx.dev.franco.automusictagfixer.ui;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author Franco Castillo
 * A viewmodels factory adapted to be used by dagger.
 */
@Singleton
public class AndroidViewModelFactory implements ViewModelProvider.Factory {
    private final Map<Class<? extends ViewModel>, Provider<ViewModel>> mViewModelsMap;
    @Inject
    public AndroidViewModelFactory(Map<Class<? extends ViewModel>, Provider<ViewModel>> viewModelsMap) {
        this.mViewModelsMap = viewModelsMap;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        Provider<ViewModel> viewModelProvider = mViewModelsMap.get(modelClass);
        if(viewModelProvider == null)
            throw new IllegalArgumentException("model class " + modelClass + " not found");

        return (T) mViewModelsMap.
                get(modelClass).
                get();
    }
}
