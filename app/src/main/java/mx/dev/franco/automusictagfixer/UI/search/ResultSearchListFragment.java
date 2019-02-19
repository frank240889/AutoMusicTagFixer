package mx.dev.franco.automusictagfixer.UI.search;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseFragment;
import mx.dev.franco.automusictagfixer.UI.main.ListFragment;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.UI.track_detail.TrackDetailFragment;
import mx.dev.franco.automusictagfixer.modelsUI.search.SearchListViewModel;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class ResultSearchListFragment extends BaseFragment implements
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
    private String mQuery;
    private SearchListViewModel mSearchListViewModel;

    @Inject
    ServiceUtils serviceUtils;
    private View mLayout;
    private SearchView mSearchView;

    public static ResultSearchListFragment newInstance(Intent intent) {
        ResultSearchListFragment fragment = new ResultSearchListFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("intent", intent);
        fragment.setArguments(bundle);
        return fragment;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchListViewModel = ViewModelProviders.of(this).get(SearchListViewModel.class);
        mSearchListViewModel.getSearchResults().observe(this, this::onSearchResults);
        mSearchListViewModel.getSearchResults().observe(this, mAdapter);
        mSearchListViewModel.isTrackProcessing().observe(this, this::showMessageError);
        mSearchListViewModel.actionTrackEvaluatedSuccessfully().observe(this, this::showDialog);
        mSearchListViewModel.actionIsTrackInaccessible().observe(this, this::showInaccessibleTrack);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLayout = inflater.inflate(R.layout.fragment_result_search_list, container, false);
        mToolbar = mLayout.findViewById(R.id.toolbar);
        return mLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //attach adapter to our recyclerview
        mRecyclerView = view.findViewById(R.id.found_tracks_recycler_view);
        mMessage = view.findViewById(R.id.found_message);


        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.SPACE_BETWEEN);
        layoutManager.setAlignItems(AlignItems.CENTER);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Glide.with(recyclerView.getContext()).resumeRequests();
                }
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    Glide.with(recyclerView.getContext()).pauseRequests();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity)getActivity()).setSupportActionBar(mToolbar);
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        //pressing back from toolbar, close activity
        mToolbar.setNavigationOnClickListener(v -> callSuperOnBackPressed());
    }

    public void onNewIntent(Intent intent){
        performSearch(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_search_activity, menu);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        mSearchView.setIconifiedByDefault(true);
    }




    private void showInaccessibleTrack(ListFragment.ViewWrapper viewWrapper) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format(getString(R.string.file_error), viewWrapper.track.getPath())).
                setPositiveButton(R.string.remove_from_list, (dialog, which) -> {
                    mSearchListViewModel.removeTrack(viewWrapper.track);
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDialog(ListFragment.ViewWrapper viewWrapper) {
        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        TrackDetailFragment trackDetailFragment = TrackDetailFragment.
                newInstance(viewWrapper.track.getMediaStoreId(), viewWrapper.mode);

        //to hide it, call the method again
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            assert imm != null;
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        catch (Exception e){
            e.printStackTrace();
        }
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
                mActionBar.setTitle( String.format(getString(R.string.search_results),tracks.size()+"",mQuery) );
            }
            else {
                mActionBar.setTitle( String.format(getString(R.string.no_found_items),mQuery) );
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

    private void performSearch(Intent intent){
        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            String q = "%"+intent.getStringExtra(SearchManager.QUERY)+"%";
            mSearchListViewModel.search(q);
        }
    }

    @Override
    public void onItemClick(int position, View view) {
        mRecyclerView.stopScroll();
        Track track = mAdapter.getDatasource().get(position);
        ListFragment.ViewWrapper viewWrapper = new ListFragment.ViewWrapper();
        viewWrapper.track = track;
        viewWrapper.view = view;
        viewWrapper.mode = Constants.CorrectionModes.SEMI_AUTOMATIC;
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
    public void onApiInitialized() {
        //Do nothing
    }

    @Override
    public void onBackPressed() {
        if (mSearchView.isShown()) {
            mSearchView.onActionViewCollapsed();
        } else {
            callSuperOnBackPressed();
        }
    }

    @Override
    public void onNetworkConnected(Void param) {
        //Do nothing
    }

    @Override
    public void onNetworkDisconnected(Void param) {
        //Do  nothing
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = super.onCreateAnimation(transit, enter, nextAnim);

        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        }

        if (animation != null && getView() != null)
            getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if(animation != null)
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animation.setAnimationListener(null);
                    Intent intent = (Intent) (getArguments() != null ? getArguments().get("intent") : null);
                    if(intent != null)
                        performSearch(intent);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

        return animation;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }
}
