package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;


/**
 * Created by franco on 10/11/16.
 */

public class ScreenSlidePagerActivity extends FragmentActivity {

    /**
     * The number of pages (wizard steps) to show.
     */
    private static final int NUM_PAGES = 4;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager viewPager;

    /**
     * The pager adapter, which provides the pages to the swipeRefreshLayout pager widget.
     */
    private PagerAdapter pagerAdapter;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector gestureDetector;
    private View.OnTouchListener gestureListener;

    private Button buttonNext, buttonOmit ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        // Instantiate a ViewPager and a PagerAdapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        buttonNext = (Button) this.findViewById(R.id.bNext);
        buttonOmit = (Button) this.findViewById(R.id.bOmit);
        viewPager.setOnTouchListener(gestureListener);
        buttonOmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ScreenSlidePagerActivity.this, MainActivity.class);
                onClickLastPager();
                startActivity(intent);
                finish();
            }
        });
        buttonNext.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                    buttonNext.setText(R.string.ok_button_last_slide);
                    buttonOmit.setVisibility(View.GONE);
                    buttonNext.setOnClickListener(null);
                    buttonNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(ScreenSlidePagerActivity.this, MainActivity.class);
                            onClickLastPager();
                            startActivity(intent);
                            finish();
                        }
                    });
                }
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(viewPager, true);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            buttonNext.setText(R.string.nextSlide);
            buttonOmit.setVisibility(View.VISIBLE);
            buttonNext.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if (viewPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                        buttonNext.setText(R.string.ok_button_last_slide);
                        buttonOmit.setVisibility(View.GONE);
                        buttonNext.setOnClickListener(null);
                        buttonNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(ScreenSlidePagerActivity.this, MainActivity.class);
                                onClickLastPager();
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                }
            });
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }



    }

    @Override
    public void onStart(){
        super.onStart();
        //View decorView = getWindow().getDecorView();
// Hide both the navigation bar and the status bar.
// SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
// a general rule, you should design your app to hide the status bar whenever you
// hide the navigation bar.
        //int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        //decorView.setSystemUiVisibility(uiOptions);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, Win);


    }

    private void onClickLastPager(){
        SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("first",false);
        editor.apply();
        editor = null;
        sharedPreferences = null;
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            if(position == 0){
                return new ScreenSlidePageFragment();
            }
            else if(position == 1) {
                return new ScreenSlidePageFragment2();
            }
            else if(position == 2){
                return new ScreenSlidePageFragment3();
            }
            else if(position == 3) {
                return new ScreenSlidePageFragment4();
            }
            else{
                return new ScreenSlidePageFragment();
            }

        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {

                    if (viewPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                        buttonNext.setText(R.string.ok_button_last_slide);
                        buttonNext.setOnClickListener(null);
                        buttonNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(ScreenSlidePagerActivity.this,MainActivity.class);
                                startActivity(intent);
                                onClickLastPager();
                                finish();
                            }
                        });
                        buttonOmit.setVisibility(View.GONE);
                    }
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    buttonNext.setText(R.string.nextSlide);
                    buttonOmit.setVisibility(View.VISIBLE);

                }
            } catch (Exception e) {

            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }
}
