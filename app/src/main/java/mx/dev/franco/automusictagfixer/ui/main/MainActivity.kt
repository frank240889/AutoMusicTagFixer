package mx.dev.franco.automusictagfixer.ui.main

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.databinding.ActivityMainBinding
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var sharedPreferences: AbstractSharedPreferences

    private var _viewBinding: ActivityMainBinding? = null

    private val viewBinding: ActivityMainBinding get() = _viewBinding!!

    companion object {
        val TAG = MainActivity::class.java.name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        ActivityMainBinding.inflate(layoutInflater).apply {
            _viewBinding = this
            setContentView(this.root)
        }

        setupBottomBar()

        /*mToggleNightModeButton?.setOnClickListener {
            if (sharedPreferences!!.getBoolean(AutoMusicTagFixer.DARK_MODE)) {
                mToggleNightModeButton?.icon = ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_wb_sunny_24px
                )
                sharedPreferences?.putBoolean(AutoMusicTagFixer.DARK_MODE, false)
                mDrawerLayout?.addDrawerListener(object : DrawerLayout.DrawerListener {
                    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
                    override fun onDrawerOpened(drawerView: View) {}
                    override fun onDrawerStateChanged(newState: Int) {}
                    override fun onDrawerClosed(drawerView: View) {
                        mDrawerLayout?.removeDrawerListener(this)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                })
                mDrawerLayout?.closeDrawers()
            } else {
                mToggleNightModeButton?.icon = getDrawable(R.drawable.ic_nights_stay_24px)
                sharedPreferences?.putBoolean(AutoMusicTagFixer.DARK_MODE, true)
                mDrawerLayout?.addDrawerListener(object : DrawerLayout.DrawerListener {
                    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
                    override fun onDrawerOpened(drawerView: View) {}
                    override fun onDrawerStateChanged(newState: Int) {}
                    override fun onDrawerClosed(drawerView: View) {
                        mDrawerLayout?.removeDrawerListener(this)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                })
                mDrawerLayout?.closeDrawers()
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        //setNightMode()
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    /*
    private fun setNightMode() {
        val nightMode: Int = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY ||
            nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ) {
            val currentNightMode: Int =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                mToggleNightModeButton?.icon = getDrawable(R.drawable.ic_wb_sunny_24px)
                mToggleNightModeButton?.setText(R.string.turn_lights_on)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                mToggleNightModeButton?.icon = getDrawable(R.drawable.ic_dark_mode_24dp)
                mToggleNightModeButton?.setText(R.string.turn_lights_off)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        } else {
            if (sharedPreferences!!.getBoolean(AutoMusicTagFixer.DARK_MODE)) {
                mToggleNightModeButton?.icon = getDrawable(R.drawable.ic_wb_sunny_24px)
                mToggleNightModeButton?.setText(R.string.turn_lights_on)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                mToggleNightModeButton?.icon = getDrawable(R.drawable.ic_dark_mode_24dp)
                mToggleNightModeButton?.setText(R.string.turn_lights_off)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
    */

    private fun setupBottomBar() {
        viewBinding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            handleClick(item)
            false
        }
    }

    private fun handleClick(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.nav_audio_files_list -> {

            }
            R.id.nav_settings -> {

            }
            R.id.nav_faq -> {

            }
            R.id.nav_about -> {

            }
        }
    }
}