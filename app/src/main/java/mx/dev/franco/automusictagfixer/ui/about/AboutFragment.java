package mx.dev.franco.automusictagfixer.ui.about;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.ui.BaseFragment;
import mx.dev.franco.automusictagfixer.ui.MainActivity;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class AboutFragment extends BaseFragment {

    public AboutFragment(){}

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //Set an action bar
        //mToolbar = view.findViewById(R.id.toolbar);
        //Set UI elements
        MaterialButton shareButton = view.findViewById(R.id.share_button);
        MaterialButton rateButton = view.findViewById(R.id.rate_button);
        MaterialButton drawerButton = view.findViewById(R.id.drawer_button);
        MaterialButton jaudiotaggerButton = view.findViewById(R.id.jaudio_tagger_button);
        MaterialButton contactButton = view.findViewById(R.id.bug_report_button);
        //Set listener for UI elements
        contactButton.setOnClickListener(v ->
                AndroidUtils.openInExternalApp(Intent.ACTION_SENDTO,
                        "mailto: dark.yellow.studios@gmail.com", getActivity()));

        jaudiotaggerButton.setOnClickListener(v ->
                AndroidUtils.openInExternalApp(Intent.ACTION_VIEW,
                        "http://www.jthink.net/jaudiotagger/", getActivity()));

        drawerButton.setOnClickListener(v ->
                AndroidUtils.openInExternalApp(Intent.ACTION_VIEW,
                        "https://tgs266.deviantart.com/", getActivity()));
        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //((MainActivity)getActivity()).toggle.syncState();
        ((MainActivity)getActivity()).mActionBar.setTitle(getString(R.string.about));
        ((MainActivity)getActivity()).mStartTaskFab.hide();
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
