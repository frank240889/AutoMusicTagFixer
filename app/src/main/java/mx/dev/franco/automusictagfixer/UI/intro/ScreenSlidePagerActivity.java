package mx.dev.franco.automusictagfixer.UI.intro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.utilities.Constants;


/**
 * Created by franco on 10/11/16.
 */

public class ScreenSlidePagerActivity extends FragmentActivity {

    /**
     * The number of pages (wizard steps) to show.
     */
    private static final int NUM_PAGES = 5;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mViewPager;

    /**
     * The pager adapter, which provides the pages to the swipeRefreshLayout pager widget.
     */
    private PagerAdapter mPagerAdapter;

    private Button mButtonNext, mButtonOmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        // Instantiate a ViewPager and a PagerAdapter.
        mViewPager = findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mViewPager.setOffscreenPageLimit(5);
        mViewPager.setAdapter(mPagerAdapter);

        mButtonNext = this.findViewById(R.id.bNext);
        mButtonOmit = this.findViewById(R.id.bOmit);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if(position == mViewPager.getChildCount() - 1){
                    mButtonNext.setText(R.string.ok_button_last_slide);
                    mButtonOmit.setVisibility(View.GONE);
                    mButtonNext.setOnClickListener(null);
                    mButtonNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(ScreenSlidePagerActivity.this, MainActivity.class);
                            onClickLastPager();
                            startActivity(intent);
                            finish();
                        }
                    });
                }
                else {
                    mButtonNext.setText(R.string.nextSlide);
                    mButtonOmit.setVisibility(View.VISIBLE);
                    mButtonNext.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v) {
                            if (mViewPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                                mButtonNext.setText(R.string.ok_button_last_slide);
                                mButtonOmit.setVisibility(View.GONE);
                                mButtonNext.setOnClickListener(null);
                                mButtonNext.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent intent = new Intent(ScreenSlidePagerActivity.this, MainActivity.class);
                                        onClickLastPager();
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            }
                            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
                        }
                    });
                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mButtonOmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ScreenSlidePagerActivity.this, MainActivity.class);
                onClickLastPager();
                startActivity(intent);
                finish();
            }
        });

        mButtonNext.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mViewPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                    mButtonNext.setText(R.string.ok_button_last_slide);
                    mButtonOmit.setVisibility(View.GONE);
                    mButtonNext.setOnClickListener(null);
                    mButtonNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(ScreenSlidePagerActivity.this, MainActivity.class);
                            onClickLastPager();
                            startActivity(intent);
                            finish();
                        }
                    });
                }
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
            }
        });


        TabLayout tabLayout = findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(mViewPager, true);
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            mButtonNext.setText(R.string.nextSlide);
            mButtonOmit.setVisibility(View.VISIBLE);
            mButtonNext.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if (mViewPager.getCurrentItem() + 1 == NUM_PAGES - 1) {
                        mButtonNext.setText(R.string.ok_button_last_slide);
                        mButtonOmit.setVisibility(View.GONE);
                        mButtonNext.setOnClickListener(null);
                        mButtonNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(ScreenSlidePagerActivity.this, MainActivity.class);
                                onClickLastPager();
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
                }
            });
            // Otherwise, select the previous step.
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
        }



    }

    @Override
    public void onStart(){
        super.onStart();
    }

    private void onClickLastPager(){
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
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
            else {
                return new ScreenSlidePageFragment5();
            }

        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
