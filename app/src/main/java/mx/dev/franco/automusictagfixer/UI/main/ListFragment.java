package mx.dev.franco.automusictagfixer.UI.main;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.sd_card_instructions.SdCardInstructionsActivity;
import mx.dev.franco.automusictagfixer.UI.track_detail.TrackDetailsActivity;
import mx.dev.franco.automusictagfixer.modelsUI.main.ListViewModel;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;

public class ListFragment extends Fragment implements
        AudioItemHolder.ClickListener,Observer<List<Track>>,
        TrackAdapter.OnSortingListener{
    private static final String TAG = ListFragment.class.getName();

    private GridLayoutManager mGridLayoutManager;
    //A simple text view to show a message when no songs were identificationFound
    private TextView mMessage;
    //swipe refresh mLayout for give to user the
    //ability to re scan the library making a swipe down gesture.
    private SwipeRefreshLayout mSwipeRefreshLayout;
    //recycler view is a component that delivers
    //better performance with huge data sources
    private RecyclerView mRecyclerView;
    private TrackAdapter mAdapter;
    private FloatingActionButton mFabStartTask;
    private FloatingActionButton mFabStopTask;
    private ListViewModel mListViewModel;
    private ActionBar mActionBar;
    private View mLayout;

    @Inject
    ServiceUtils serviceUtils;

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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AutoMusicTagFixer.getContextComponent().inject(this);
        mAdapter = new TrackAdapter(this);
        mListViewModel = ViewModelProviders.of(this).get(ListViewModel.class);

        mListViewModel.actionShowMessage().observe(this, this::showMessageError);
        mListViewModel.isTrackProcessing().observe(this, this::showMessageError);
        mListViewModel.actionTrackEvaluatedSuccessfully().observe(this, this::showDialog);
        mListViewModel.actionCanRunService().observe(this, this::showMessage);
        mListViewModel.actionCanOpenDetails().observe(this, this::openDetails);
        mListViewModel.actionCanStartAutomaticMode().observe(this, this::startCorrection);
        mListViewModel.actionIsTrackInaccessible().observe(this, this::showInaccessibleTrack);
        mListViewModel.noFilesFound().observe(this, this::noFilesFoundMessage);
        mListViewModel.showProgress().observe(this, this::showProgress);
        mListViewModel.getAllTracks().observe(this, this);
        mListViewModel.getAllTracks().observe(this, mAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLayout = inflater.inflate(R.layout.fragment_list, container, false);
        return mLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Inflate the mLayout for this fragment
        mRecyclerView = mLayout.findViewById(R.id.tracks_recycler_view);
        mSwipeRefreshLayout = mLayout.findViewById(R.id.refresh_layout);
        mMessage = mLayout.findViewById(R.id.message);
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();


        //attach adapter to our recyclerview
        mGridLayoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Glide.with(getActivity()).resumeRequests();
                }
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING
                        || newState == RecyclerView.SCROLL_STATE_SETTLING) {
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

        //Color of progress bar of refresh layout
        mSwipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.grey_900),
                ContextCompat.getColor(getActivity(), R.color.grey_800),
                ContextCompat.getColor(getActivity(), R.color.grey_700)

        );
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getActivity().
                getResources().getColor(R.color.primaryColor));

        boolean hasPermission = ContextCompat.
                checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;


        mSwipeRefreshLayout.setOnRefreshListener(()->{
            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
            }
            else {
                mListViewModel.updateTrackList();
            }
        });

        if(!hasPermission) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
        else {
            mMessage.setText(R.string.loading_tracks);
        }

        setHasOptionsMenu(true);
        setRetainInstance(true);
        //App is opened again
        int id = getActivity().getIntent().getIntExtra(Constants.MEDIA_STORE_ID, -1);
        scrollTo(id);


        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            mListViewModel.getInfoForTracks();

        boolean isPresentSD = StorageHelper.getInstance(getActivity().getApplicationContext()).
                isPresentRemovableStorage();
        if(AndroidUtils.getUriSD(getActivity().getApplicationContext()) == null && isPresentSD)
            getActivity().startActivity(new Intent(getActivity(), SdCardInstructionsActivity.class));

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFabStartTask = ((MainActivity)getActivity()).mStartTaskFab;
        mFabStopTask = ((MainActivity)getActivity()).mStopTaskFab;
        mFabStartTask.setOnClickListener(v -> startCorrection(-1));
        mFabStopTask.setOnClickListener(v -> stopCorrection());
    }

    private void noFilesFoundMessage(Boolean aBoolean) {
        mFabStopTask.hide();
        mFabStartTask.hide();
        mMessage.setVisibility(View.VISIBLE);
        mMessage.setText(R.string.no_items_found);
    }


    private void showMessageError(String s) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mLayout, getActivity().getApplicationContext());
        snackbar.setText(s);
        snackbar.show();
    }

    private void showProgress(Boolean showProgress) {
        mSwipeRefreshLayout.setRefreshing(showProgress);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mRecyclerView.stopScroll();
        mGridLayoutManager.setSpanCount(newConfig.orientation
                == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
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

    public boolean sort(String by, int order){
        return mListViewModel.sortTracks(by, order, mAdapter.getDatasource());
    }

    public void checkAll(){
        mListViewModel.checkAllItems();
    }

    public void updateList(){
        mListViewModel.updateTrackList();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onPause(){
        super.onPause();
        mRecyclerView.stopScroll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Stop correction task if "Usar corrección en segundo plano" from Settings is off.
        if(!PreferenceManager.getDefaultSharedPreferences(getActivity().
                getApplicationContext()).getBoolean("key_background_service", true)){
            Intent intent = new Intent(getActivity(),FixerTrackService.class);
            getActivity().stopService(intent);
        }
    }

    public void showViewPermissionMessage() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_dialog_permision).
                setMessage(R.string.explanation_permission_access_files);
        builder.setPositiveButton(R.string.ok_button, (dialog, which) -> {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
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
        Toast t = AndroidUtils.getToast(getActivity());
        t.setText(status);
        t.show();
    }

    @Override
    public void onItemClick(int position, View view) {
        ViewWrapper viewWrapper = new ViewWrapper();
        viewWrapper.track = mAdapter.getDatasource().get(position);
        viewWrapper.view = view;
        viewWrapper.mode = Constants.CorrectionModes.SEMI_AUTOMATIC;
        mListViewModel.onItemClick(viewWrapper);
    }

    public void showDialog(ViewWrapper viewWrapper){
        openDetails(viewWrapper);
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
        Snackbar snackbar = AndroidUtils.getSnackbar(mRecyclerView, getActivity());
        snackbar.setText(message);
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.show();
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
        if(tracks != null) {
            if(tracks.isEmpty()) {
                mFabStopTask.hide();
                mFabStartTask.hide();
                mMessage.setVisibility(View.VISIBLE);
                mMessage.setText(R.string.no_items_found);
            }
            else {
                boolean isServiceRunning = serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME);
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

                    Intent stopIntent = new Intent(getActivity(), FixerTrackService.class);
                    stopIntent.setAction(Constants.Actions.ACTION_COMPLETE_TASK);
                    getActivity().getApplicationContext().startService(stopIntent);
                    Toast t = AndroidUtils.getToast(getContext());
                    t.setDuration(Toast.LENGTH_SHORT);
                    t.setText(R.string.cancelling);
                    t.show();
                    mFabStopTask.setEnabled(false);
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showInaccessibleTrack(ViewWrapper viewWrapper) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format(getString(R.string.file_error), viewWrapper.track.getPath())).
                setPositiveButton(R.string.remove_from_list, (dialog, which) -> {
                    mListViewModel.removeTrack(viewWrapper.track);
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void stopScroll() {
        mRecyclerView.stopScroll();
    }

    private void startCorrection(int id) {
        Intent intent = new Intent(getActivity(),FixerTrackService.class);
        intent.putExtra(Constants.MEDIA_STORE_ID, id);
        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        //    getContext().startForegroundService(intent);
        //}
        //else {
            getContext().startService(intent);
        //}
    }

    public void correctionStarted() {
        mFabStartTask.hide();
        mFabStopTask.show();
    }

    public void correctionCompleted(){
        mFabStartTask.show();
        mFabStopTask.setEnabled(true);
        mFabStopTask.hide();
    }

    @Override
    public void onStartSorting() {
        mSwipeRefreshLayout.setEnabled(false);
    }

    @Override
    public void onFinishSorting() {
        mSwipeRefreshLayout.setEnabled(true);
        mRecyclerView.scrollToPosition(0);
    }

    public static class ViewWrapper{
        public View view;
        public Track track;
        public int mode;
    }

    public void scrollTo(int id) {
        if(id == -1)
            return;

        Track track = mAdapter.getTrackById(id);
        int position = mAdapter.getDatasource().indexOf(track);
        mRecyclerView.scrollToPosition(position);
    }

    public List<Track> getDatasource() {
        return mAdapter.getDatasource();
    }

}
