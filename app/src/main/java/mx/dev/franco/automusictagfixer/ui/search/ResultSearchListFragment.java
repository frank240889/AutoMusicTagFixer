package mx.dev.franco.automusictagfixer.ui.search;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.BaseFragment;
import mx.dev.franco.automusictagfixer.ui.MainActivity;
import mx.dev.franco.automusictagfixer.ui.main.ViewWrapper;
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailFragment;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class ResultSearchListFragment extends BaseFragment<SearchListViewModel> implements
        FoundItemHolder.ClickListener{
    public static final String TAG = ResultSearchListFragment.class.getName();

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

    @Inject
    ServiceUtils serviceUtils;
    private EditText mSearchBox;

    public static ResultSearchListFragment newInstance() {
        return new ResultSearchListFragment();
    }

    public ResultSearchListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAdapter = new SearchTrackAdapter(this);
        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    protected SearchListViewModel getViewModel() {
        return ViewModelProviders.
            of(this, androidViewModelFactory).get(SearchListViewModel.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchListViewModel = ViewModelProviders.of(this).get(SearchListViewModel.class);
        mSearchListViewModel.getSearchResults().observe(this, this::onSearchResults);
        mSearchListViewModel.getSearchResults().observe(this, mAdapter);
        mSearchListViewModel.isTrackProcessing().observe(this, this::showMessageError);
        mSearchListViewModel.actionTrackEvaluatedSuccessfully().observe(this, this::openDetailTrack);
        mSearchListViewModel.actionIsTrackInaccessible().observe(this, this::showInaccessibleTrack);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result_search_list, container, false);
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
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

        mToolbar = view.findViewById(R.id.toolbar);
        mSearchBox = view.findViewById(R.id.search_box);
        mSearchBox.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED){
                mQuery = mSearchBox.getText().toString();
                mSearchListViewModel.search(mQuery);
                hideKeyboard();
            }

            return false;
        });

        //attach adapter to our recyclerview
        mRecyclerView = view.findViewById(R.id.found_tracks_recycler_view);
        mMessage = view.findViewById(R.id.found_message);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);
        mRecyclerView.setAdapter(mAdapter);
        mSearchBox.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.showSoftInput(mSearchBox, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity)getActivity()).setSupportActionBar(mToolbar);
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        //pressing back from toolbar, close activity
        mToolbar.setNavigationOnClickListener(v -> callSuperOnBackPressed());
        getActivity().invalidateOptionsMenu();
    }

    private void showInaccessibleTrack(ViewWrapper viewWrapper) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format(getString(R.string.file_error), viewWrapper.track.getPath())).
                setPositiveButton(R.string.remove_from_list, (dialog, which) -> {
                    mSearchListViewModel.removeTrack(viewWrapper.track);
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

        TrackDetailFragment trackDetailFragment = TrackDetailFragment.
                newInstance(viewWrapper.track.getMediaStoreId(), viewWrapper.mode);
        getActivity().getSupportFragmentManager().beginTransaction().
                setCustomAnimations(R.anim.slide_in_right,
                        R.anim.slide_out_left, R.anim.slide_in_left,
                        R.anim.slide_out_right).
                addToBackStack(TrackDetailFragment.TAG).
                add(R.id.container_fragments, trackDetailFragment, TrackDetailFragment.TAG).
                commit();
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
        mRecyclerView.stopScroll();
        mAdapter.destroy();
        mMessage = null;
        mRecyclerView = null;
        mAdapter = null;
    }

    @Override
    public void onBackPressed() {
        callSuperOnBackPressed();
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

    @Override
    public void onDetach() {
        super.onDetach();
        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }
}
