package mx.dev.franco.automusictagfixer.ui.about;

import android.content.Intent;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.ui.BaseFragment;
import mx.dev.franco.automusictagfixer.ui.MainActivity;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class AboutFragment extends BaseFragment {


    private MaterialToolbar mToolbar;
    private ActionBar mActionBar;
    private AppBarLayout mAppBarLayout;


    public AboutFragment(){}

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.activity_scrolling_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //Set an action bar
        mToolbar = view.findViewById(R.id.toolbar);
        //Set UI elements
        MaterialButton shareButton = view.findViewById(R.id.share_button);
        MaterialButton rateButton = view.findViewById(R.id.rate_button);
        MaterialButton drawerButton = view.findViewById(R.id.drawer_button);
        MaterialButton jaudiotaggerButton = view.findViewById(R.id.jaudio_tagger_button);
        MaterialButton contactButton = view.findViewById(R.id.bug_report_button);

        mAppBarLayout = view.findViewById(R.id.app_bar);
        mAppBarLayout.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                Drawable background = view.getBackground();
                if (background != null) {
                    background.getOutline(outline);
                } else {
                    outline.setRect(0, 0, view.getWidth(), view.getHeight());
                    outline.setAlpha(0.0f);
                }
            }
        });
        //Set listener for UI elements
        contactButton.setOnClickListener(v -> AndroidUtils.openInExternalApp(Intent.ACTION_SENDTO, "mailto: dark.yellow.studios@gmail.com", getActivity()));

        jaudiotaggerButton.setOnClickListener(v -> AndroidUtils.openInExternalApp(Intent.ACTION_VIEW, "http://www.jthink.net/jaudiotagger/", getActivity()));

        drawerButton.setOnClickListener(v -> AndroidUtils.openInExternalApp(Intent.ACTION_VIEW, "https://tgs266.deviantart.com/", getActivity()));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity)getActivity()).setSupportActionBar(mToolbar);
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();
        //mActionBar.setDisplayHomeAsUpEnabled(true);
        //mActionBar = ((MainActivity)getActivity()).getSupportActionBar();
        //mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        /*ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(getActivity(),
                ((MainActivity)getActivity()).mDrawer,
                mToolbar,R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        ((MainActivity)getActivity()).mDrawer.addDrawerListener(toggle);
        toggle.syncState();*/
        mActionBar.setTitle(getString(R.string.about));
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        //callSuperOnBackPressed();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = super.onCreateAnimation(transit, enter, nextAnim);

        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        }

        if (animation != null && getView() != null)
            getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

        return animation;
    }
}
