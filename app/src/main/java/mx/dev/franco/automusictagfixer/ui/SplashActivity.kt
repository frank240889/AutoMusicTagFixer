package mx.dev.franco.automusictagfixer.ui

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import mx.dev.franco.automusictagfixer.ui.intro.IntroActivity
import mx.dev.franco.automusictagfixer.ui.main.MainActivity
import mx.dev.franco.automusictagfixer.utilities.Constants

/**
 * Created by franco on 6/11/16.
 */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var preferences = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )

        //Is first use of app?
        preferences = getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, MODE_PRIVATE)
        val firstTime = preferences.getBoolean("first", true)
        val intent: Intent = if (firstTime) {
            //Is first app use
            Intent(this, IntroActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        finishAfterTransition()
        startActivity(intent)
    }
}