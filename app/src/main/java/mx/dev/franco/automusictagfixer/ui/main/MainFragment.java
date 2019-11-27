package mx.dev.franco.automusictagfixer.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.interfaces.LongRunningTaskListener;
import mx.dev.franco.automusictagfixer.interfaces.ProcessingListener;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.database.TrackContract;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.ui.BaseViewModelFragment;
import mx.dev.franco.automusictagfixer.ui.InformativeFragmentDialog;
import mx.dev.franco.automusictagfixer.ui.MainActivity;
import mx.dev.franco.automusictagfixer.ui.sdcardinstructions.SdCardInstructionsActivity;
import mx.dev.franco.automusictagfixer.ui.search.ResultSearchFragment;
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailFragment;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class MainFragment extends BaseViewModelFragment<ListViewModel> implements
        AudioItemHolder.ClickListener,
        LongRunningTaskListener, ProcessingListener {
    private static final String TAG = MainFragment.class.getName();

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
    //private ActionBar mActionBar;
    private Menu mMenu;
    //private AppBarLayout mAppBarLayout;
    //private Toolbar mToolbar;
    private ExtendedFloatingActionButton mStartTaskFab;
    private FloatingActionButton mStopTaskFab;
    private List<Track> mCurrentTracks;

    @Inject
    ServiceUtils serviceUtils;
    @Inject
    AbstractSharedPreferences mAbstractSharedPreferences;
    private int mVerticalOffset = 0;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    public MainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAdapter = new TrackAdapter(this);
        mListViewModel = getViewModel();

        mListViewModel.observeAccessibleTrack().observe(this, this::openDetails);
        mListViewModel.observeActionCanOpenDetails().observe(this, this::openDetails);
        mListViewModel.observeActionCanStartAutomaticMode().observe(this, this::startCorrection);
        mListViewModel.observeIsTrackInaccessible().observe(this, this::showInaccessibleTrack);
        mListViewModel.observeResultFilesFound().observe(this, this::noResultFilesFound);
        mListViewModel.observeLoadingState().observe(this, this::loading);
        mListViewModel.observeActionCheckAll().observe(this, this::onCheckAll);
        mListViewModel.observeSizeResultsMediaStore().observe(this, message -> {
            if(message == null)
                return;

            Snackbar snackbar = AndroidUtils.createSnackbar(mRecyclerView, message);
            snackbar.show();
        });
        mListViewModel.getTracks().observe(this, tracks -> {
            if(tracks == null)
                return;

            mAdapter.onChanged(tracks);
            mCurrentTracks = tracks;
            updateToolbar(mCurrentTracks);
        });

        mListViewModel.observeInformativeMessage().observe(this, this::onMessage);
        mListViewModel.observeOnSortTracks().observe(this, this::onSorted);
        mListViewModel.observeOnSdPresent().observe(this, this::onSdPresent);

        getChildFragmentManager().addOnBackStackChangedListener(this::onBackStackChangeListener);
    }

    private void onBackStackChangeListener() {
        if(getChildFragmentManager().getBackStackEntryCount() == 0) {
            updateToolbar(mCurrentTracks);
        }
    }

    private void updateToolbar(List<Track> tracks) {
        if(tracks == null)
            return;

        if(tracks.isEmpty()) {
            //mStopTaskFab.hide();
            mStartTaskFab.hide();
            mMessage.setVisibility(View.VISIBLE);
            mMessage.setText(R.string.no_items_found);
        }
        else {
            boolean isServiceRunning = serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME);
            if(!isServiceRunning){
                mStartTaskFab.show();
                //mStopTaskFab.hide();
            }
            else {
                mStartTaskFab.hide();
                //mStopTaskFab.show();
            }
            ((MainActivity)getActivity()).mMainToolbar.setTitle(tracks.size() + " " +getString(R.string.tracks));
            ((MainActivity)getActivity()).mActionBar.setTitle(tracks.size() + " " +getString(R.string.tracks));
            mMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mVerticalOffset = ((MainActivity)getActivity()).mMainAppbar.getHeight();
        mRecyclerView = view.findViewById(R.id.tracks_recycler_view);
        mSwipeRefreshLayout = view.findViewById(R.id.refresh_layout);
        mMessage = view.findViewById(R.id.message);
        mStartTaskFab = ((MainActivity)getActivity()).mStartTaskFab;//view.findViewById(R.id.fab_start_stop);
        //mStopTaskFab = view.findViewById(R.id.fab_stop);
        mStartTaskFab.setOnClickListener(v -> startCorrection(-1));
        //mStopTaskFab.setOnClickListener(v -> stopCorrection());
        mStartTaskFab.hide();
        //mStopTaskFab.hide();

        //attach adapter recyclerview
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        //mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setHapticFeedbackEnabled(true);
        mRecyclerView.setSoundEffectsEnabled(true);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout.setOnRefreshListener(()->{
            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                mViewModel.notifyPermissionNotGranted();
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
            }
            else {
                rescan();
            }
        });

        //Color of background and progress tint of progress bar of refresh layout
        mSwipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.progressTintSwipeRefreshLayout)
        );
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getActivity().
                getResources().getColor(R.color.progressSwipeRefreshLayoutBackgroundTint));

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
        //((MainActivity)getActivity()).mDrawer.addDrawerListener(((MainActivity)getActivity()).toggle);
        //((MainActivity)getActivity()).mActionBar.setDefaultDisplayHomeAsUpEnabled(false);
        //((MainActivity)getActivity()).toggle.syncState();
        //pressing back from toolbar, close activity
        //((MainActivity)getActivity()).mMainToolbar.setNavigationOnClickListener(null);
        getActivity().invalidateOptionsMenu();
        ((MainActivity)getActivity()).toggle.setDrawerIndicatorEnabled(true);
        ((MainActivity)getActivity()).toggle.syncState();
        ((MainActivity)getActivity()).mDrawer.addDrawerListener(((MainActivity)getActivity()).toggle);
        updateToolbar(mCurrentTracks);
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
                    ResultSearchFragment resultSearchListFragment = (ResultSearchFragment)
                            getChildFragmentManager().findFragmentByTag(ResultSearchFragment.class.getName());

                    if(resultSearchListFragment == null) {
                        resultSearchListFragment = ResultSearchFragment.newInstance();
                    }
                    mStartTaskFab.hide();

                    getChildFragmentManager().beginTransaction().
                            setCustomAnimations(R.anim.slide_in_right,
                                    R.anim.slide_out_left, R.anim.slide_in_left,
                                    R.anim.slide_out_right).
                            addToBackStack(ResultSearchFragment.class.getName()).
                            add(R.id.child_fragment_container, resultSearchListFragment,
                                    ResultSearchFragment.class.getName()).
                            commit();


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
        //mStopTaskFab.hide();
        mStartTaskFab.hide();
        mMessage.setVisibility(View.VISIBLE);
    }

    @Override
    protected ListViewModel getViewModel() {
        return ViewModelProviders.of(this, androidViewModelFactory).get(ListViewModel.class);
    }

    @Override
    protected void loading(boolean isLoading) {
        if(isLoading) {
            mRecyclerView.setEnabled(false);
            mRecyclerView.animate().alpha(0).setDuration(100);
        }
        else {
            mRecyclerView.animate().alpha(1).setDuration(100);
            mRecyclerView.setEnabled(false);
        }
        mSwipeRefreshLayout.setRefreshing(isLoading);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mRecyclerView.stopScroll();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //Check permission to access files and execute scan if were granted
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            mListViewModel.fetchTracks();

        }
        else {
            mSwipeRefreshLayout.setEnabled(true);
            //mStopTaskFab.hide();
            mStartTaskFab.hide();
            mMessage.setVisibility(View.VISIBLE);
            mMessage.setText(R.string.permission_denied);
            mListViewModel.setLoading(false);
            showViewPermissionMessage();
        }

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

    private void showViewPermissionMessage() {
        InformativeFragmentDialog informativeFragmentDialog = InformativeFragmentDialog.
            newInstance(R.string.title_dialog_permision,
                R.string.explanation_permission_access_files,
                R.string.accept, R.string.cancel_button, getActivity());
        informativeFragmentDialog.show(getChildFragmentManager(),
            informativeFragmentDialog.getClass().getCanonicalName());

        informativeFragmentDialog.setOnClickBasicFragmentDialogListener(
            new InformativeFragmentDialog.OnClickBasicFragmentDialogListener() {
                @Override
                public void onPositiveButton() {
                    informativeFragmentDialog.dismiss();
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
                }

                @Override
                public void onNegativeButton() {
                    informativeFragmentDialog.dismiss();
                }
            }
        );
    }

    @Override
    public void onCoverClick(int position, View view) {
        ViewWrapper viewWrapper = new ViewWrapper();
        viewWrapper.position = position;
        viewWrapper.mode = CorrectionActions.VIEW_INFO;
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
        viewWrapper.mode = CorrectionActions.SEMI_AUTOMATIC;
        mListViewModel.onItemClick(viewWrapper);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = super.onCreateAnimation(transit, enter, nextAnim);

        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        }

        if (animation != null && getView() != null) {
            getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //For Android Marshmallow and Lollipop, there is no need to request permissions
                    //at runtime.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                        mListViewModel.fetchTracks();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
        else {
            //For Android Marshmallow and Lollipop, there is no need to request permissions
            //at runtime.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                mListViewModel.fetchTracks();
        }
        return animation;
    }

    /**
     * Opens new activity showing up the details from current audio item list pressed
     * @param viewWrapper a wrapper object containing the track, view , and mode of correction.
     */
    private void openDetails(ViewWrapper viewWrapper){
        mRecyclerView.stopScroll();
        mStartTaskFab.hide();
        openFragment(viewWrapper);
    }

    private void openFragment(ViewWrapper viewWrapper){
        final TrackDetailFragment[] trackDetailFragment = new TrackDetailFragment[1];
        trackDetailFragment[0] = (TrackDetailFragment) getChildFragmentManager()
                .findFragmentByTag(TrackDetailFragment.class.getName());
        if (trackDetailFragment[0] != null) {
            trackDetailFragment[0].loadTrackData(AndroidUtils.getBundle(viewWrapper.track.getMediaStoreId(),
                    viewWrapper.mode));
        } else {
            trackDetailFragment[0] = TrackDetailFragment.newInstance(
                    viewWrapper.track.getMediaStoreId(),
                    viewWrapper.mode);
            getChildFragmentManager().beginTransaction().
                    setCustomAnimations(R.anim.slide_in_right,
                            R.anim.slide_out_left, R.anim.slide_in_left,
                            R.anim.slide_out_right).
                    addToBackStack(trackDetailFragment[0].getTagName()).
                    add(R.id.child_fragment_container,
                            trackDetailFragment[0], trackDetailFragment[0].getTagName()).
                    commit();
        }
    }

    private void stopCorrection() {
        InformativeFragmentDialog informativeFragmentDialog = InformativeFragmentDialog.
                newInstance(R.string.attention,
                        R.string.cancel_task,
                        R.string.yes, R.string.no, getActivity());
        informativeFragmentDialog.show(getChildFragmentManager(),
                informativeFragmentDialog.getClass().getCanonicalName());

        informativeFragmentDialog.setOnClickBasicFragmentDialogListener(
                new InformativeFragmentDialog.OnClickBasicFragmentDialogListener() {
                    @Override
                    public void onPositiveButton() {
                        //stops service, and sets starting state to FAB

                        Intent stopIntent = new Intent(getActivity(), FixerTrackService.class);
                        stopIntent.setAction(Constants.Actions.ACTION_STOP_TASK);
                        getActivity().getApplicationContext().startService(stopIntent);
                        Toast t = AndroidUtils.getToast(getContext());
                        t.setDuration(Toast.LENGTH_SHORT);
                        t.setText(R.string.cancelling);
                        t.show();
                        //mStopTaskFab.setEnabled(false);
                    }

                    @Override
                    public void onNegativeButton() {
                        informativeFragmentDialog.dismiss();
                    }
                }
        );
    }

    private void showInaccessibleTrack(ViewWrapper viewWrapper) {
        InformativeFragmentDialog informativeFragmentDialog = InformativeFragmentDialog.
                newInstance(getString(R.string.attention),
                        String.format(getString(R.string.file_error), viewWrapper.track.getPath()),
                        getString(R.string.remove_from_list), null);
        informativeFragmentDialog.show(getChildFragmentManager(),
                informativeFragmentDialog.getClass().getCanonicalName());

        informativeFragmentDialog.setOnClickBasicFragmentDialogListener(
                new InformativeFragmentDialog.OnClickBasicFragmentDialogListener() {
                    @Override
                    public void onPositiveButton() {
                        mListViewModel.removeTrack(viewWrapper.track);
                    }

                    @Override
                    public void onNegativeButton() {
                        informativeFragmentDialog.dismiss();
                    }
                }
        );
    }

    private void stopScroll() {
        mRecyclerView.stopScroll();
    }

    private void startCorrection(int id) {
        Intent intent = new Intent(getActivity(),FixerTrackService.class);
        intent.putExtra(Constants.MEDIA_STORE_ID, id);
        intent.setAction(Constants.Actions.ACTION_START_TASK);
        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        //    getContext().startForegroundService(intent);
        //}
        //else {
            Objects.requireNonNull(getActivity()).startService(intent);
        //}
    }

    private void onCheckAll(Boolean checkAll) {
        if(!checkAll)
            mListViewModel.checkAllItems();
    }

    @Override
    public void onLongRunningTaskStarted() {
        mStartTaskFab.hide();
        //mStopTaskFab.show();
    }

    @Override
    public void onStartProcessingFor(int id) {
        int index = mListViewModel.getTrackPosition(id);
        mRecyclerView.scrollToPosition(index);
    }

    @Override
    public void onLongRunningTaskMessage(String error) {
        Snackbar snackbar = AndroidUtils.createSnackbar(getView().findViewById(R.id.root_container), error);
        //snackbar.setAnchorView(mStartTaskFab);
        snackbar.show();
    }

    @Override
    public void onLongRunningTaskFinish() {
        mStartTaskFab.show();
        //mStopTaskFab.setEnabled(true);
        //mStopTaskFab.hide();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getChildFragmentManager().removeOnBackStackChangedListener(this::onBackStackChangeListener);
        ((MainActivity)getActivity()).mDrawer.removeDrawerListener(((MainActivity)getActivity()).toggle);
    }

    private void onMessage(Integer integer) {
        Snackbar snackbar = AndroidUtils.getSnackbar(getView().findViewById(R.id.root_container),
                getActivity().getApplicationContext());
        snackbar.setText(integer);
        snackbar.setAnchorView(mStartTaskFab);
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
