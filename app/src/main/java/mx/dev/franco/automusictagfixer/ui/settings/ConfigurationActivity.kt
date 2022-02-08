package mx.dev.franco.automusictagfixer.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import mx.dev.franco.automusictagfixer.R
import javax.inject.Inject

class ConfigurationActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    lateinit var mDispatchingAndroidInjector: DispatchingAndroidInjector<Any>
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun androidInjector(): AndroidInjector<Any> = mDispatchingAndroidInjector
}