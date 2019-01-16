package mx.dev.franco.automusictagfixer.UI.intro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.Constants;

public class IntroActivity extends MaterialIntroActivity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        addSlide(new SlideFragmentBuilder()
                        .backgroundColor(R.color.grey_900)
                        .buttonsColor(R.color.primaryColor)
                        .image(R.drawable.ic_sentiment_very_satisfied_white_24px)
                        .title(getString(R.string.welcome))
                        .description(getString(R.string.step_1))
                        .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.grey_800)
                .buttonsColor(R.color.primaryColor)
                .image(R.drawable.ic_edit_white_24px)
                .description(getString(R.string.step_2))
                .build());
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.grey_700)
                .buttonsColor(R.color.primaryColor)
                .image(R.drawable.ic_search_white_24px)
                .description(getString(R.string.step_3))
                .build());
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.grey_900)
                .buttonsColor(R.color.primaryColor)
                .image(R.drawable.ic_touch_app_white_24px)
                .description(getString(R.string.step_4))
                .build());
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.grey_800)
                .buttonsColor(R.color.primaryColor)
                .image(R.drawable.ic_check_box_white_24px)
                .description(getString(R.string.step_5))
                .build());
    }

    @Override
    public void onFinish(){
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("first",false);
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finishAfterTransition();
    }
}
