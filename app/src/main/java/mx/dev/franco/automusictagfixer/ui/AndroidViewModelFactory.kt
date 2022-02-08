package mx.dev.franco.automusictagfixer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

/**
 * @author Franco Castillo
 * A viewmodels factory adapted to be used by dagger.
 */
@Suppress("UNCHECKED_CAST")
class AndroidViewModelFactory
@Inject
constructor(private val mViewModelsMap: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val viewModelProvider = mViewModelsMap[modelClass]
            ?: error("model class $modelClass not found")
        return viewModelProvider.get() as T
    }
}