package mx.dev.franco.automusictagfixer.UI.main;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseFragment;
import mx.dev.franco.automusictagfixer.UI.sd_card_instructions.SdCardInstructionsActivity;
import mx.dev.franco.automusictagfixer.UI.search.ResultSearchListFragment;
import mx.dev.franco.automusictagfixer.UI.track_detail.TrackDetailFragment;
import mx.dev.franco.automusictagfixer.interfaces.LongRunningTaskListener;
import mx.dev.franco.automusictagfixer.interfaces.ProcessingListener;
import mx.dev.franco.automusictagfixer.modelsUI.main.ListViewModel;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.database.TrackContract;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class ListFragment extends BaseFragment implements
        AudioItemHolder.ClickListener,
        LongRunningTaskListener, ProcessingListener {
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
    private ListViewModel mListViewModel;
    private ActionBar mActionBar;
    private Menu mMenu;
    private Toolbar mToolbar;
    private FloatingActionButton mStartTaskFab;
    private FloatingActionButton mStopTaskFab;

    @Inject
    ServiceUtils serviceUtils;

    public static ListFragment newInstance() {
        return new ListFragment();
    }

    public ListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new TrackAdapter(this);
        mListViewModel = ViewModelProviders.of(this).get(ListViewModel.class);

        mListViewModel.observeAccessibleTrack().observe(this, this::openDetails);
        mListViewModel.observeActionCanOpenDetails().observe(this, this::openDetails);
        mListViewModel.observeActionCanStartAutomaticMode().observe(this, this::startCorrection);
        mListViewModel.observeIsTrackInaccessible().observe(this, this::showInaccessibleTrack);
        mListViewModel.observeResultFilesFound().observe(this, this::noResultFilesFound);
        mListViewModel.observeLoadingState().observe(this, this::loading);
        mListViewModel.observeActionCheckAll().observe(this, this::onCheckAll);
        mListViewModel.getTracks().observe(this, tracks -> {
            if(tracks == null)
                return;

            mAdapter.onChanged(tracks);
            if(tracks.isEmpty()) {
                mStopTaskFab.hide();
                mStartTaskFab.hide();
                mMessage.setVisibility(View.VISIBLE);
                mMessage.setText(R.string.no_items_found);
            }
            else {
                boolean isServiceRunning = serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME);
                if(!isServiceRunning){
                    mStartTaskFab.show();
                    mStopTaskFab.hide();
                }
                else {
                    mStartTaskFab.hide();
                    mStopTaskFab.show();
                }
                mActionBar.setTitle(tracks.size() + " " +getString(R.string.tracks));
                mMessage.setVisibility(View.GONE);
            }
        });

        mListViewModel.observeInformativeMessage().observe(this, this::onMessage);
        mListViewModel.observeOnSortTracks().observe(this, this::onSorted);
        mListViewModel.observeOnSdPresent().observe(this, this::onSdPresent);

        //For Android Marshmallow and Lollipop, there is no need to request permissions
        //at runtime.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            mListViewModel.fetchTracks();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.layout_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        mRecyclerView = view.findViewById(R.id.tracks_recycler_view);
        mSwipeRefreshLayout = view.findViewById(R.id.refresh_layout);
        mMessage = view.findViewById(R.id.message);

        mStartTaskFab = view.findViewById(R.id.fab_start);
        mStopTaskFab = view.findViewById(R.id.fab_stop);
        mStartTaskFab.setOnClickListener(v -> startCorrection(-1));
        mStopTaskFab.setOnClickListener(v -> stopCorrection());
        mStartTaskFab.hide();
        mStopTaskFab.hide();
        mToolbar = view.findViewById(R.id.toolbar);

        //attach adapter recyclerview
        mGridLayoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout.setOnRefreshListener(()->{
            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
            }
            else {
                rescan();
            }
        });

        //Color of progress bar of refresh layout
        mSwipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.true_black),
                ContextCompat.getColor(getActivity(), R.color.grey_900),
                ContextCompat.getColor(getActivity(), R.color.transparent),
                ContextCompat.getColor(getActivity(), R.color.grey_850),
                ContextCompat.getColor(getActivity(), R.color.grey_800),
                ContextCompat.getColor(getActivity(), R.color.grey_700)

        );
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getActivity().
                getResources().getColor(R.color.primaryColor));

        setHasOptionsMenu(true);

        boolean hasPermission = ContextCompat.
                checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        if(!hasPermission) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
        else {
            mMessage.setText(R.string.loading_tracks);
        }

        //App is opened again, then scroll to the track being processed.
        int id = getActivity().getIntent().getIntExtra(Constants.MEDIA_STORE_ID, -1);
        int pos = mListViewModel.getTrackPosition(id);
        mRecyclerView.scrollToPosition(pos);

        mListViewModel.checkSdIsPresent();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity)getActivity()).setSupportActionBar(mToolbar);
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar = ((MainActivity)getActivity()).getSupportActionBar();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(getActivity(),
                ((MainActivity)getActivity()).mDrawer,
                mToolbar,R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        ((MainActivity)getActivity()).mDrawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_main_activity, menu);
        mMenu = menu;
        checkItem(-1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        stopScroll();
        switch (id){
            case R.id.action_select_all:
                    mListViewModel.checkAllTracks();
                break;
            case R.id.action_search:
                    ResultSearchListFragment resultSearchListFragment = ResultSearchListFragment.newInstance();
                    getActivity().getSupportFragmentManager().beginTransaction().
                            setCustomAnimations(R.anim.slide_in_right,
                                    R.anim.slide_out_left, R.anim.slide_in_left,
                                    R.anim.slide_out_right).
                            addToBackStack(resultSearchListFragment.getClass().getName()).
                            add(R.id.container_fragments, resultSearchListFragment,
                                    resultSearchListFragment.getClass().getName()).
                            commitNow();

                break;
            case R.id.action_refresh:
                    rescan();
                break;
            case R.id.path_asc:
                    mListViewModel.sortTracks(TrackContract.TrackData.DATA, TrackRepository.ASC, id);
                break;
            case R.id.path_desc:
                    mListViewModel.sortTracks(TrackContract.TrackData.DATA, TrackRepository.DESC, id);
                break;
            case R.id.title_asc:
                    mListViewModel.sortTracks(TrackContract.TrackData.TITLE, TrackRepository.ASC, id);
                break;
            case R.id.title_desc:
                    mListViewModel.sortTracks(TrackContract.TrackData.TITLE, TrackRepository.DESC, id);
                break;
            case R.id.artist_asc:
                    mListViewModel.sortTracks(TrackContract.TrackData.ARTIST, TrackRepository.ASC, id);
                break;
            case R.id.artist_desc:
                    mListViewModel.sortTracks(TrackContract.TrackData.ARTIST, TrackRepository.DESC, id);
                break;
            case R.id.album_asc:
                    mListViewModel.sortTracks(TrackContract.TrackData.ALBUM, TrackRepository.ASC, id);
                break;
            case R.id.album_desc:
                    mListViewModel.sortTracks(TrackContract.TrackData.ALBUM, TrackRepository.DESC, id);
                break;
        }
        return true;
    }

    public void rescan(){
        boolean hasPermission = ContextCompat.
                checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        if(!hasPermission) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
        else {
            mListViewModel.rescan();
        }
    }

    /**
     * Set the menu item with an icon to mark which type of sort is select
     * and saves to shared preferences to persist its value.
     * @param selectedItem The id of item selected.
     */
    private void checkItem(int selectedItem) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
        //No previous value was found.
        if(selectedItem == -1){
            int currentSelectedItem = sharedPreferences.getInt(Constants.SELECTED_ITEM, -1);
            if(currentSelectedItem == -1){
                MenuItem defaultMenuItemSelected = mMenu.findItem(R.id.title_asc);
                defaultMenuItemSelected.setIcon(ContextCompat.getDrawable(
                        getActivity().getApplicationContext(), R.drawable.ic_done_white));
                sharedPreferences.edit().putInt(Constants.SELECTED_ITEM,
                        defaultMenuItemSelected.getItemId()).apply();
                sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM,
                        defaultMenuItemSelected.getItemId()).apply();
            }
            else {
                MenuItem menuItemSelected = mMenu.findItem(currentSelectedItem);
                if(menuItemSelected != null) {
                    menuItemSelected.setIcon(ContextCompat.
                            getDrawable(getActivity().getApplicationContext(), R.drawable.ic_done_white));
                    sharedPreferences.edit().putInt(Constants.SELECTED_ITEM,
                            menuItemSelected.getItemId()).apply();
                    sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM,
                            menuItemSelected.getItemId()).apply();
                }
            }
        }
        else {
            int lastItemSelected = sharedPreferences.getInt(Constants.LAST_SELECTED_ITEM, -1);
            //User selected the same item.
            if(selectedItem == lastItemSelected)
                return;

            MenuItem menuItemSelected = mMenu.findItem(selectedItem);
            MenuItem lastMenuItemSelected = mMenu.findItem(lastItemSelected);
            //Clear last selected
            if(lastMenuItemSelected != null)
                lastMenuItemSelected.setIcon(null);

            int selectedMenuItem = -1;
            if(menuItemSelected != null) {
                menuItemSelected.setIcon(ContextCompat.getDrawable(getActivity().getApplicationContext(),
                        R.drawable.ic_done_white));
                selectedMenuItem = menuItemSelected.getItemId();
            }
            sharedPreferences.edit().putInt(Constants.SELECTED_ITEM, selectedMenuItem).apply();
            sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM, selectedMenuItem).apply();
        }
    }


    /**
     * Run when the scan of media store has finished and no music files have
     * been found.
     * @param voids void param, not usable.
     */
    private void noResultFilesFound(Void voids) {
        mStopTaskFab.hide();
        mStartTaskFab.hide();
        mMessage.setVisibility(View.VISIBLE);
    }

    @Override
    protected void loading(boolean isLoading) {
        mSwipeRefreshLayout.setRefreshing(isLoading);
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
            mListViewModel.fetchTracks();

        }
        else {
            mSwipeRefreshLayout.setEnabled(true);
            mStopTaskFab.hide();
            mStartTaskFab.hide();
            mMessage.setVisibility(View.VISIBLE);
            mMessage.setText(R.string.permission_denied);
            mListViewModel.setLoading(false);
            showViewPermissionMessage();
        }

    }

    public void checkAll(){
        mListViewModel.checkAllItems();
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

    @Override
    public void onStop() {
        super.onStop();
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
        viewWrapper.position = position;
        viewWrapper.mode = Constants.CorrectionModes.VIEW_INFO;
        mListViewModel.onClickCover(viewWrapper);
    }

    @Override
    public void onCheckboxClick(int position) {
        mListViewModel.onCheckboxClick(position);
    }

    @Override
    public void onCheckMarkClick(int position) {
        mListViewModel.onCheckMarkClick(position);
    }

    @Override
    public void onItemClick(int position, View view) {
        ViewWrapper viewWrapper = new ViewWrapper();
        viewWrapper.position = position;
        viewWrapper.mode = Constants.CorrectionModes.SEMI_AUTOMATIC;
        mListViewModel.onItemClick(viewWrapper);
    }

    /**
     * Opens new activity showing up the details from current audio item list pressed
     * @param viewWrapper a wrapper object containing the track, view , and mode of correction.
     */
    private void openDetails(ViewWrapper viewWrapper){
        mRecyclerView.stopScroll();

        TrackDetailFragment trackDetailFragment;

        ((MainActivity)getActivity()).mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        trackDetailFragment = (TrackDetailFragment) getActivity().
                getSupportFragmentManager().findFragmentByTag(TrackDetailFragment.TAG);
        if(trackDetailFragment != null){

            trackDetailFragment.load(AndroidUtils.getBundle(viewWrapper.track.getMediaStoreId(),
                    viewWrapper.mode));
        }
        else {

            trackDetailFragment = TrackDetailFragment.newInstance(
                    viewWrapper.track.getMediaStoreId(),
                    viewWrapper.mode);
            getActivity().getSupportFragmentManager().beginTransaction().
                    setCustomAnimations(R.anim.slide_in_right,
                            R.anim.slide_out_left, R.anim.slide_in_left,
                            R.anim.slide_out_right).
                    addToBackStack(TrackDetailFragment.TAG).
                    add(R.id.container_fragments, trackDetailFragment, TrackDetailFragment.TAG).
                    commit();
        }

    }

    private void stopCorrection() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.cancel_task)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    //stops service, and sets starting state to FAB

                    Intent stopIntent = new Intent(getActivity(), FixerTrackService.class);
                    stopIntent.setAction(Constants.Actions.ACTION_STOP_TASK);
                    getActivity().getApplicationContext().startService(stopIntent);
                    Toast t = AndroidUtils.getToast(getContext());
                    t.setDuration(Toast.LENGTH_SHORT);
                    t.setText(R.string.cancelling);
                    t.show();
                    mStopTaskFab.setEnabled(false);
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showInaccessibleTrack(ViewWrapper viewWrapper) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format(getString(R.string.file_error), viewWrapper.track.getPath())).
                setPositiveButton(R.string.remove_from_list, (dialog, which) ->
                        mListViewModel.removeTrack(viewWrapper.track));

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
            Objects.requireNonNull(getActivity()).startService(intent);
        //}
    }

    @Override
    public void onBackPressed() {
        callSuperOnBackPressed();
    }

    private void onCheckAll(Boolean checkAll) {
        if(!checkAll)
            checkAll();
    }

    @Override
    public void onLongRunningTaskStarted() {
        mStartTaskFab.hide();
        mStopTaskFab.show();
    }

    @Override
    public void onStartProcessingFor(int id) {
        int index = mListViewModel.getTrackPosition(id);
        mRecyclerView.scrollToPosition(index);
    }

    @Override
    public void onLongRunningTaskMessage(String error) {
        Toast toast = AndroidUtils.getToast(getActivity().getApplicationContext());
        toast.setText(error);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void onLongRunningTaskFinish() {
        mStartTaskFab.show();
        mStopTaskFab.setEnabled(true);
        mStopTaskFab.hide();
    }

    private void onMessage(Integer integer) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mRecyclerView, getActivity().getApplicationContext());
        snackbar.setText(integer);
        snackbar.show();
    }

    private void onSdPresent(Boolean sdPresent) {
        if(sdPresent)
            getActivity().startActivity(new Intent(getActivity(), SdCardInstructionsActivity.class));
    }

    private void onSorted(Integer idResource) {
        if(idResource != -1) {
            checkItem(idResource);
        }
    }
}
