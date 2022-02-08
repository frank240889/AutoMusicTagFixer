package mx.dev.franco.automusictagfixer.ui

import android.content.Context
import android.os.Bundle
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

abstract class BaseViewModelRoundedBottomSheetDialogFragment<ViewModel: Any> :
    BaseRoundedBottomSheetDialogFragment(), HasAndroidInjector{
    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var androidViewModelFactory: AndroidViewModelFactory
    protected lateinit var vm: ViewModel

    /**
     * Called when a fragment is first attached to its context.
     * [.onCreate] will be called after this.
     */
    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = getViewModel()
    }

    abstract fun getViewModel(): ViewModel
    protected fun loading(isLoading: Boolean) {}

    override fun androidInjector(): AndroidInjector<Any> = fragmentDispatchingAndroidInjector
}