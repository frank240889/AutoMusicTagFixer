package mx.dev.franco.automusictagfixer.ui

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

/**
 * Base fragment that abstract the common functionality for fragments
 * that inherits from it.
 *
 * @author Franco Castillo
 */
abstract class BaseViewModelFragment<VM: ViewModel> : BaseFragment(), HasAndroidInjector {

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var androidViewModelFactory: AndroidViewModelFactory

    protected lateinit var viewModel: VM

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = obtainViewModel()
    }

    abstract fun obtainViewModel(): VM

    open fun loading(isLoading: Boolean) {}

    override fun androidInjector(): AndroidInjector<Any> = fragmentDispatchingAndroidInjector
}