package mx.dev.franco.automusictagfixer.ui.search;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.BaseViewModelFragment;
import mx.dev.franco.automusictagfixer.ui.InformativeFragmentDialog;
import mx.dev.franco.automusictagfixer.ui.MainActivity;
import mx.dev.franco.automusictagfixer.ui.main.ViewWrapper;
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailFragment;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class ResultSearchFragment extends BaseViewModelFragment<SearchListViewModel> implements
        FoundItemHolder.ClickListener{
    public static final String TAG = ResultSearchFragment.class.getName();

    //A simple texview to show a message when no songs were identificationFound
    private TextView mMessage;
    //recycler view is a component that delivers
    //better performance with huge data sources
    private RecyclerView mRecyclerView;
    private SearchTrackAdapter mAdapter;
    private Toolbar mToolbar;
    private ActionBar mActionBar;
    private String mQuery = null;
    private SearchListViewModel mSearchListViewModel;
    private AppBarLayout mAppBarLayout;

    @Inject
    ServiceUtils serviceUtils;
    private EditText mSearchBox;

    public static ResultSearchFragment newInstance() {
        return new ResultSearchFragment();
    }

    public ResultSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAdapter = new SearchTrackAdapter(this);
    }

    @Override
    protected SearchListViewModel getViewModel() {
        return ViewModelProviders.
            of(this, androidViewModelFactory).get(SearchListViewModel.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSearchListViewModel = ViewModelProviders.of(this).get(SearchListViewModel.class);
        mSearchListViewModel.getSearchResults().observe(this, this::onSearchResults);
        mSearchListViewModel.getSearchResults().observe(this, mAdapter);
        mSearchListViewModel.isTrackProcessing().observe(this, this::showMessageError);
        mSearchListViewModel.actionTrackEvaluatedSuccessfully().observe(this, this::openDetailTrack);
        mSearchListViewModel.actionIsTrackInaccessible().observe(this, this::showInaccessibleTrack);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result_search_list, container, false);
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            assert imm != null;
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).mSearchBox.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED){
                mQuery = ((MainActivity)getActivity()).mSearchBox.getText().toString();
                mSearchListViewModel.search(mQuery);
                hideKeyboard();
            }

            return false;
        });

        //attach adapter to our recyclerview
        mRecyclerView = view.findViewById(R.id.found_tracks_recycler_view);
        mMessage = view.findViewById(R.id.found_message);

        LinearLayoutManager line = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(line);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity)getActivity()).mDrawer.removeDrawerListener(((MainActivity)getActivity()).toggle);
        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        //((MainActivity)getActivity()).mActionBar.setDisplayHomeAsUpEnabled(true);
        //((MainActivity)getActivity()).mActionBar.setDefaultDisplayHomeAsUpEnabled(true);
        //((MainActivity)getActivity()).mActionBar.setHomeButtonEnabled(true);
        //((MainActivity)getActivity()).toggle.setDrawerIndicatorEnabled(true);

        ((MainActivity)getActivity()).mSearchBox.setVisibility(View.VISIBLE);
        ((MainActivity)getActivity()).mSearchBox.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm != null)
            imm.showSoftInput(((MainActivity)getActivity()).mSearchBox, InputMethodManager.SHOW_IMPLICIT);
    }

    private void showInaccessibleTrack(ViewWrapper viewWrapper) {
        String content = String.format(getString(R.string.file_error), viewWrapper.track.getPath());
        InformativeFragmentDialog informativeFragmentDialog = InformativeFragmentDialog.
                newInstance(getString(R.string.attention), content, getString(R.string.remove_from_list), null);
        informativeFragmentDialog.setOnClickBasicFragmentDialogListener(() -> mSearchListViewModel.removeTrack(viewWrapper.track));
        informativeFragmentDialog.show(getChildFragmentManager(), informativeFragmentDialog.getTag());
    }

    private void openDetailTrack(ViewWrapper viewWrapper) {
        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        //to hide it, call the method again
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            assert imm != null;
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        TrackDetailFragment trackDetailFragment;

        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        trackDetailFragment = (TrackDetailFragment) getParentFragment().
                getChildFragmentManager().findFragmentByTag(TrackDetailFragment.class.getName());
        if(trackDetailFragment != null){
            trackDetailFragment.loadTrackData(AndroidUtils.getBundle(viewWrapper.track.getMediaStoreId(),
                    viewWrapper.mode));
        }
        else {
            trackDetailFragment = TrackDetailFragment.newInstance(
                    viewWrapper.track.getMediaStoreId(),
                    viewWrapper.mode);
            getParentFragment().
                    getChildFragmentManager().beginTransaction().
                    setCustomAnimations(R.anim.slide_in_bottom,
                            R.anim.slide_out_left, R.anim.slide_in_left,
                            R.anim.slide_out_top).
                    addToBackStack(TrackDetailFragment.class.getName()).
                    add(R.id.child_fragment_container,
                            trackDetailFragment, TrackDetailFragment.class.getName()).
                    commit();
        }
    }

    private void showMessageError(String s) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mRecyclerView, getActivity().getApplicationContext());
        snackbar.setText(s);
        snackbar.show();
    }

    private void onSearchResults(List<Track> tracks) {
        if(tracks != null) {
            if(tracks.size() > 0) {
                mMessage.setVisibility(View.GONE);
            }
            else {
                if(mQuery != null) {
                    Toast toast = AndroidUtils.getToast(getActivity());
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.setText(String.format(getString(R.string.no_found_items), mQuery));
                    toast.show();
                }
                mMessage.setVisibility(View.VISIBLE);
            }
        }
        mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onPause(){
        super.onPause();
        mRecyclerView.stopScroll();
    }

    @Override
    public void onItemClick(int position, View view) {
        mRecyclerView.stopScroll();
        Track track = mAdapter.getDatasource().get(position);
        ViewWrapper viewWrapper = new ViewWrapper();
        viewWrapper.track = track;
        viewWrapper.mode = CorrectionActions.SEMI_AUTOMATIC;
        mSearchListViewModel.onItemClick(viewWrapper);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        getParentFragment().getChildFragmentManager().popBackStack();
        mRecyclerView.stopScroll();
        mAdapter.destroy();
        mMessage = null;
        mRecyclerView = null;
        mAdapter = null;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = super.onCreateAnimation(transit, enter, nextAnim);

        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if(isRemoving()) {
                        //getActivity().invalidateOptionsMenu();
                        ((MainActivity)getActivity()).mSearchBox.setVisibility(View.GONE);
                        ((MainActivity)getActivity()).mSearchBox.setText("");
                        ((MainActivity)getActivity()).mSearchBox.setOnEditorActionListener(null);
                        hideKeyboard();
                        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }

        if (animation != null && getView() != null)
            getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

        return animation;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
