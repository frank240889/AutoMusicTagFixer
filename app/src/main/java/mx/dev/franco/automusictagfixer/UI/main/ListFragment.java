package mx.dev.franco.automusictagfixer.UI.main;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.track_detail.TrackDetailsActivity;
import mx.dev.franco.automusictagfixer.datasource.AudioItemHolder;
import mx.dev.franco.automusictagfixer.datasource.TrackAdapter;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.services.ServiceHelper;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.ViewUtils;

public class ListFragment extends Fragment implements AudioItemHolder.ClickListener,Observer<List<Track>> {
    private static final String TAG = ListFragment.class.getName();
    public interface OnInteractionFragment{
        void onClickCover();
    }

    private GridLayoutManager mGridLayoutManager;
    //A simple texview to show a message when no songs were identificationFound
    private TextView mMessage;
    //swipe refresh mLayout for give to user the
    //ability to re scan the library making a swipe down gesture.
    private SwipeRefreshLayout mSwipeRefreshLayout;
    //recycler view is a component that delivers
    //better performance with huge data sources
    private RecyclerView mRecyclerView;
    private TrackAdapter mAdapter;
    private Menu mMenu;
    private SearchView mSearchViewWidget;
    private FloatingActionButton mFabStartTask;
    private FloatingActionButton mFabStopTask;
    private ListViewModel mListViewModel;
    private ActionBar mActionBar;
    private View mLayout;
    private OnInteractionFragment mListener;

    @Inject
    ServiceHelper serviceHelper;

    public static ListFragment newInstance() {
        ListFragment fragment = new ListFragment();
        return fragment;
    }

    public ListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (OnInteractionFragment) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AutoMusicTagFixer.getContextComponent().inject(this);
        mFabStartTask = ((MainActivity)getActivity()).mStartTaskFab;
        mFabStopTask = ((MainActivity)getActivity()).mStopTaskFab;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLayout = inflater.inflate(R.layout.fragment_list, container, false);

        // Inflate the mLayout for this fragment
        mRecyclerView = mLayout.findViewById(R.id.tracks_recycler_view);
        mSwipeRefreshLayout = mLayout.findViewById(R.id.refresh_layout);
        mMessage = mLayout.findViewById(R.id.message);
        // setup menu icon
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();


        //attach adapter to our recyclerview
        mGridLayoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        mAdapter = new TrackAdapter(this);
        Log.d(TAG, mAdapter.getItemCount()+"");
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                Log.d(TAG, "State: " + newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Glide.with(getActivity()).resumeRequests();
                }
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    Glide.with(getActivity()).pauseRequests();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        //Lets implement functionality for refresh mLayout listener
        mSwipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.primaryColor),
                ContextCompat.getColor(getActivity(), R.color.primaryDarkColor),
                ContextCompat.getColor(getActivity(), R.color.primaryLightColor)
        );
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getActivity().getResources().getColor(R.color.grey_900));

        mFabStartTask.setOnClickListener(v -> startCorrection(-1));
        mFabStopTask.setOnClickListener(v -> stopCorrection());

        boolean hasPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        mListViewModel = ViewModelProviders.of(this).get(ListViewModel.class);

        mListViewModel.actionShowMessage().observe(this, this::showMessageError);
        mListViewModel.isTrackProcessing().observe(this, this::showMessageError);
        mListViewModel.actionTrackEvaluatedSuccessfully().observe(this, this::showDialog);
        mListViewModel.actionCanRunService().observe(this, this::showMessage);
        mListViewModel.actionCanOpenDetails().observe(this, this::openDetails);
        mListViewModel.actionCanStartAutomaticMode().observe(this, this::startCorrection);
        mListViewModel.actionIsTrackInaccessible().observe(this, this::showInnaccesibleTrack);
        mListViewModel.showProgress().observe(this, this::showProgress);
        mListViewModel.getAllTracks().observeForever(this);
        mListViewModel.getAllTracks().observeForever(mAdapter);
        mSwipeRefreshLayout.setOnRefreshListener(()->{
                if(!hasPermission) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
                }
                else {
                    mListViewModel.updateTrackList();
                }
            });

        if(!hasPermission) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
        else {
            mMessage.setText(R.string.loading_tracks);
        }

        setHasOptionsMenu(true);
        setRetainInstance(true);

        return mLayout;
    }



    private void showMessageError(String s) {
        Snackbar snackbar = ViewUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
        snackbar.setText(s);
        snackbar.show();
    }

    private void showProgress(Boolean showProgress) {
        if(showProgress)
            mSwipeRefreshLayout.setRefreshing(true);
        else
            mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onResume(){
        super.onResume();
    }


    @Override
    public void onStop(){
        super.onStop();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mRecyclerView.stopScroll();
        mGridLayoutManager.setSpanCount(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1);
        super.onConfigurationChanged(newConfig);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        //Check permission to access files and execute scan if were granted
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            mMessage.setText(R.string.loading_tracks);
            mListViewModel.getInfoForTracks();

        }
        else {
            mFabStopTask.hide();
            mFabStartTask.hide();
            mMessage.setVisibility(View.VISIBLE);
            mMessage.setText(R.string.permission_denied);
            mListViewModel.setProgress(false);
            showViewPermissionMessage();
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        return super.onOptionsItemSelected(menuItem);
    }

    public boolean sort(String by, int order){
        return mAdapter.sortBy(by, order);
    }

    public void checkAll(){
        mListViewModel.checkAllItems();
    }

    public void updateList(){
        mListViewModel.updateTrackList();
    }

    public static Animator createRevealWithDelay(View view, int centerX, int centerY, float startRadius, float endRadius) {
        Animator delayAnimator = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, startRadius);
        delayAnimator.setDuration(100);
        Animator revealAnimator = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius);
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(delayAnimator, revealAnimator);
        return set;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        // Define an expand listener for search widget
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //mSearchViewWidget = (SearchView) searchItem.getActionView();
            //mSearchViewWidget.getLayoutTransition()
            //        .enableTransitionType(LayoutTransition.CHANGING);
            MenuItem.OnActionExpandListener expandListener = new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {

                    // When no searching a track, activate refresh listener
                    // of swipe mLayout
                    //mSwipeRefreshLayout.setEnabled(true);
                    //mSearchViewWidget.setOnQueryTextListener(null);
                    return true;  // Return true to collapse action widget
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // When searching a song, deactivate the swipe refresh mLayout
                    // and don't let update with swipe gesture
                    //mSwipeRefreshLayout.setEnabled(false);
                    Animator animator = createRevealWithDelay(searchView,0,0,0,20);
                    animator.start();
                    // Attach a listener that returns results while user is searching his/her song
                    mSearchViewWidget.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            //mRecyclerView.stopScroll();
                            //mAdapter.filter(newText);
                            return true;
                        }
                    });
                    return true;  // Return true to expand action mSwipeRefreshLayout
                }
            };

            // Assign the listener to searchItem
            //searchItem.setOnActionExpandListener(expandListener);
        }
        else {
            //mSearchViewWidget = (SearchView) MenuItemCompat.getActionView(searchItem);
            MenuItemCompat.OnActionExpandListener expandListener = new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    //when no searching a track, activate refresh listener
                    //of swipe mLayout
                    //mSwipeRefreshLayout.setEnabled(true);
                    //mSearchViewWidget.setOnQueryTextListener(null);
                    return true;  // Return true to collapse action widget
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    //when searching a song, deactivate the swipe refresh mLayout
                    //and don't let update with swipe gesture
                    //mSwipeRefreshLayout.setEnabled(false);
                    Animator animator = createRevealWithDelay(searchView,0,0,0,20);
                    animator.start();
                    //attach a listener that returns results while user is searching his/her song
                    mSearchViewWidget.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            //mRecyclerView.stopScroll();
                            //mAdapter.filter(newText);
                            return true;
                        }
                    });
                    return true;  // Return true to expand action mSwipeRefreshLayout
                }
            };

            // Assign the listener to searchItem
            //MenuItemCompat.setOnActionExpandListener(searchItem, expandListener);
        }*/
    }

    @Override
    public void onPause(){
        super.onPause();
        //Deregister filters if FixerTrackService if not processing any task,
        //useful for saving resources

        mRecyclerView.stopScroll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListViewModel.getAllTracks().removeObserver(mAdapter);
        mListViewModel.getAllTracks().removeObserver(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void showViewPermissionMessage() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_dialog_permision).setMessage(R.string.explanation_permission_access_files);
        builder.setPositiveButton(R.string.ok_button, (dialog, which) -> {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
        });
        final AlertDialog dialog =  builder.create();
        dialog.show();
    }

    @Override
    public void onCoverClick(int position, View view) {
        ViewWrapper viewWrapper = new ViewWrapper();
        viewWrapper.view = view;
        viewWrapper.track = mAdapter.getDatasource().get(position);
        viewWrapper.mode = Constants.CorrectionModes.VIEW_INFO;
        mListViewModel.onClickCover(viewWrapper);
    }

    @Override
    public void onCheckboxClick(int position) {
        Track track = mAdapter.getDatasource().get(position);
        mListViewModel.updateTrack(track);
    }

    @Override
    public void onCheckMarkClick(int position) {
        String status =  mListViewModel.getState(mAdapter.getDatasource().get(position).getState());
        Toast t = Toast.makeText(getActivity().getApplicationContext(), status, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER,0,0);
        t.show();
    }

    @Override
    public void onItemClick(int position, View view) {
        ViewWrapper viewWrapper = new ViewWrapper();
        viewWrapper.track = mAdapter.getDatasource().get(position);
        viewWrapper.view = view;
        mListViewModel.onItemClick(viewWrapper);
    }

    public void showDialog(ViewWrapper viewWrapper){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format(getString(R.string.message_track), AudioItem.getPath(viewWrapper.track.getPath()))).
        setPositiveButton(R.string.automatic, (dialog, which) -> mListViewModel.onAutomaticMode(viewWrapper)).
        setNegativeButton(R.string.semiautomatic, (dialog, which) -> {
            viewWrapper.mode = Constants.CorrectionModes.SEMI_AUTOMATIC;
            openDetails(viewWrapper);
        }).
        setNeutralButton(R.string.manual, (dialog, which) -> {
            viewWrapper.mode = Constants.CorrectionModes.MANUAL;
            openDetails(viewWrapper);
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showMessage(int code){
        String message = "";
        if (code == 1) {
            message = getActivity().getString(R.string.no_internet_connection_automatic_mode);
        } else if (code == 2) {
            message = getActivity().getString(R.string.could_not_init_api);
        } else if (code == 3) {
            message = getActivity().getString(R.string.correction_in_progress);
        }
        Snackbar.make(mRecyclerView,message,Snackbar.LENGTH_LONG).show();
    }


    /**
     * Opens new activity showing up the details from current audio item list pressed
     * @param viewWrapper a wrapper object containing the track, view , and mode of correction.
     */
    public void openDetails(ViewWrapper viewWrapper){
        mRecyclerView.stopScroll();
        Intent intent = new Intent(getActivity(), TrackDetailsActivity.class);
        intent.putExtra(Constants.MEDIA_STORE_ID, viewWrapper.track.getMediaStoreId());
        intent.putExtra(Constants.CorrectionModes.MODE, viewWrapper.mode);
        getActivity().startActivity(intent);
    }

    /**
     * Runs when a modification in database
     * occurred
     * @param tracks The list with the new data.
     */
    @Override
    public void onChanged(@Nullable List<Track> tracks) {
        mListViewModel.setProgress(false);
        if(tracks != null) {
            if(tracks.isEmpty()) {
                mFabStopTask.hide();
                mFabStartTask.hide();
                mMessage.setVisibility(View.VISIBLE);
                mMessage.setText(R.string.no_items_found);
            }
            else {
                boolean isServiceRunning = serviceHelper.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME);
                if(!isServiceRunning){
                    mFabStartTask.show();
                    mFabStopTask.hide();
                }
                else {
                    mFabStartTask.hide();
                    mFabStopTask.show();
                }
                mActionBar.setTitle(tracks.size() + " " +getString(R.string.tracks));
                mMessage.setVisibility(View.GONE);
            }
        }
    }


    private void stopCorrection() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.cancel_task)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    //stops service, and sets starting state to FAB
                    Intent intent = new Intent(getActivity(),FixerTrackService.class);
                    getActivity().getApplicationContext().stopService(intent);
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Converts from status code from audioitem object
     * to human readable status text
     * @return msg Is the string code
     */

    /*private String getStatusText(int status){
        String msg = "";
        switch (status){
            case AudioItem.STATUS_ALL_TAGS_FOUND:
                msg = getResources().getString(R.string.file_status_ok);
                break;
            case AudioItem.STATUS_ALL_TAGS_NOT_FOUND:
                msg = getResources().getString(R.string.file_status_incomplete);
                break;
            case AudioItem.STATUS_NO_TAGS_FOUND:
                msg = getResources().getString(R.string.file_status_bad);
                break;
            case AudioItem.STATUS_TAGS_EDITED_BY_USER:
                msg = getResources().getString(R.string.file_status_edit_by_user);
                break;
            case AudioItem.FILE_ERROR_READ:
                msg = getString(R.string.file_status_error_read);
                break;
            case AudioItem.STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE:
                msg = getString(R.string.file_status_corrected_by_semiautomatic_mode);
                break;
            case AudioItem.STATUS_FILE_IN_SD_WITHOUT_PERMISSION:
                msg = getString(R.string.file_status_in_sd_without_permission);
                break;
            case AudioItem.STATUS_COULD_NOT_APPLIED_CHANGES:
                msg = getString(R.string.could_not_apply_changes);
                break;
            case AudioItem.STATUS_COULD_RESTORE_FILE_TO_ITS_LOCATION:
                msg = getString(R.string.could_not_copy_to_its_original_location);
                break;
            case AudioItem.STATUS_COULD_NOT_CREATE_AUDIOFILE:
                msg = getString(R.string.could_not_create_audiofile);
                break;
            case AudioItem.STATUS_COULD_NOT_CREATE_TEMP_FILE:
                msg = getString(R.string.could_not_create_temp_file);
                break;
            default:
                msg = getResources().getString(R.string.file_status_no_processed);
                break;
        }

        return msg;
    }*/

    private void showInnaccesibleTrack(ViewWrapper viewWrapper) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format(getString(R.string.file_error), viewWrapper.track.getPath())).
                setPositiveButton(R.string.remove_from_list, (dialog, which) -> {
                    mListViewModel.removeTrack(viewWrapper.track);
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void startCorrection(int id) {
        Intent intent = new Intent(getActivity(),FixerTrackService.class);
        intent.putExtra(Constants.MEDIA_STORE_ID, id);
        getContext().startService(intent);
    }

    public void correctionStarted() {
        mFabStartTask.hide();
        mFabStopTask.show();
    }

    public void correctionCompleted(){
        mFabStartTask.show();
        mFabStopTask.hide();
    }

    public static class ViewWrapper{
        public View view;
        public Track track;
        public int mode;
    }

    public void updateItem(int id, Intent intent) {
        //Bundle bundle = new Bundle();
        //bundle.putBoolean("should_reload_cover", intent.getBooleanExtra("should_reload_cover", true));
        //bundle.putBoolean("processing", intent.getBooleanExtra("processing", false));
        Track track = mAdapter.getTrackById(id);
        int position = mAdapter.getDatasource().indexOf(track);
        mAdapter.notifyItemChanged(position, intent.getExtras());
    }

    public List<Track> getDatasource() {
        return mAdapter.getDatasource();
    }

}
