package mx.dev.franco.automusictagfixer.ui.intro

import android.content.Intent
import android.os.Bundle
import io.github.dreierf.materialintroscreen.MaterialIntroActivity
import io.github.dreierf.materialintroscreen.SlideFragmentBuilder
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.ui.main.MainActivity
import mx.dev.franco.automusictagfixer.utilities.Constants

class IntroActivity : MaterialIntroActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addSlide(
            SlideFragmentBuilder()
                .backgroundColor(R.color.primaryColor)
                .buttonsColor(R.color.secondaryTextColor)
                .image(R.drawable.ic_sentiment_very_satisfied_white_24px)
                .title(getString(R.string.welcome))
                .description(getString(R.string.step_1))
                .build()
        )
        addSlide(
            SlideFragmentBuilder()
                .backgroundColor(R.color.primaryColor)
                .buttonsColor(R.color.secondaryTextColor)
                .image(R.drawable.ic_edit_white_24px)
                .description(getString(R.string.step_2))
                .build()
        )
        addSlide(
            SlideFragmentBuilder()
                .backgroundColor(R.color.primaryColor)
                .buttonsColor(R.color.secondaryTextColor)
                .image(R.drawable.ic_search_white_24px)
                .description(getString(R.string.step_3))
                .build()
        )
        addSlide(
            SlideFragmentBuilder()
                .backgroundColor(R.color.primaryColor)
                .buttonsColor(R.color.secondaryTextColor)
                .image(R.drawable.ic_touch_app_white_24px)
                .description(getString(R.string.step_4))
                .build()
        )
        addSlide(
            SlideFragmentBuilder()
                .backgroundColor(R.color.primaryColor)
                .buttonsColor(R.color.secondaryTextColor)
                .image(R.drawable.ic_check_box_white_24px)
                .description(getString(R.string.step_5))
                .build()
        )
    }

    override fun onFinish() {
        val sharedPreferences =
            getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("first", false)
        editor.apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAfterTransition()
    }
}