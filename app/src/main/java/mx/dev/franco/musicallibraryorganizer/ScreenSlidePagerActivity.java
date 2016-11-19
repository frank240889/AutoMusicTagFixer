package mx.dev.franco.musicallibraryorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;


/**
 * Created by franco on 10/11/16.
 */

public class ScreenSlidePagerActivity extends FragmentActivity {

    ScreenSlidePagerActivity _this;
    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 4;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;

    protected Button buttonNext, buttonOmit ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);
        _this = this;
        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        buttonNext = (Button) this.findViewById(R.id.bNext);
        buttonOmit = (Button) this.findViewById(R.id.bOmit);
        mPager.setOnTouchListener(gestureListener);
        buttonOmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(_this, SelectFolderActivity.class);
                startActivity(intent);
                finish();
            }
        });
        buttonNext.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                    buttonNext.setText(R.string.ok_button_last_slide);
                    buttonOmit.setVisibility(View.GONE);
                    buttonNext.setOnClickListener(null);
                    buttonNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(_this, SelectFolderActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                }
                mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            buttonNext.setText(R.string.nextSlide);
            buttonOmit.setVisibility(View.VISIBLE);
            buttonNext.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if (mPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                        buttonNext.setText(R.string.ok_button_last_slide);
                        buttonOmit.setVisibility(View.GONE);
                        buttonNext.setOnClickListener(null);
                        buttonNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(_this, SelectFolderActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                    mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
                }
            });
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }



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

                    if (mPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                        buttonNext.setText(R.string.ok_button_last_slide);
                        buttonNext.setOnClickListener(null);
                        buttonNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(_this,SelectFolderActivity.class);
                                startActivity(intent);
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
