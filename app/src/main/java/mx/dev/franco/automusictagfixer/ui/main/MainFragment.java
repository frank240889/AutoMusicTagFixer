package mx.dev.franco.automusictagfixer.ui.main;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.AsyncListDiffer;
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
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.interfaces.AutomaticTaskListener;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.database.TrackContract;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.ui.BaseViewModelFragment;
import mx.dev.franco.automusictagfixer.ui.InformativeFragmentDialog;
import mx.dev.franco.automusictagfixer.ui.sdcardinstructions.SdCardInstructionsActivity;
import mx.dev.franco.automusictagfixer.ui.search.ResultSearchFragment;
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailActivity;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static android.widget.Toast.LENGTH_SHORT;

public class MainFragment extends BaseViewModelFragment<ListViewModel> implements
        AudioItemHolder.ClickListener,
        AutomaticTaskListener, AutomaticTaskListener.MessageListener, AsyncListDiffer.ListListener<List<Track>> {
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
    //private ActionBar actionBar;
    private Menu mMenu;
    private boolean mHasPermission;
    //private AppBarLayout mAppBarLayout;
    //private Toolbar mToolbar;
    public ExtendedFloatingActionButton mStartTaskFab;
    private FloatingActionButton mStopTaskFab;
    private Snackbar mStopCorrectionSnackbar;
    private List<Track> mCurrentTracks;

    @Inject
    ServiceUtils serviceUtils;
    @Inject
    AbstractSharedPreferences mAbstractSharedPreferences;
    @Inject
    AudioTagger.StorageHelper storageHelper;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    public MainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAdapter = new TrackAdapter(this);
        mViewModel.observeAccessibleTrack().observe(this, this::openDetails);
        mViewModel.observeActionCanOpenDetails().observe(this, this::openDetails);
        mViewModel.observeActionCanStartAutomaticMode().observe(this, this::startCorrection);
        mViewModel.observeIsTrackInaccessible().observe(this, this::showInaccessibleTrack);
        mViewModel.observeResultFilesFound().observe(this, this::noResultFilesFound);
        mViewModel.observeLoadingState().observe(this, this::loading);
        mViewModel.observeActionCheckAll().observe(this, this::onCheckAll);
        mViewModel.observeSizeResultsMediaStore().observe(this, message -> {
            if(message == null)
                return;

            mMessage.setText(message.getIdResourceMessage());
            Snackbar snackbar = AndroidUtils.createSnackbar(mSwipeRefreshLayout, message);
            snackbar.show();
        });
        mViewModel.getTracks().observe(this, tracks -> {
            mAdapter.onChanged(tracks);
            mCurrentTracks = tracks;
            updateToolbar(mViewModel.getTrackList());
        });

        mViewModel.observeInformativeMessage().observe(this, this::onMessage);
        mViewModel.observeSorting().observe(this, sort -> {
            if (sort != null && sort.idResource != -1)
                checkItem(sort.idResource);
        });

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mRecyclerView = view.findViewById(R.id.tracks_recycler_view);
        mSwipeRefreshLayout = view.findViewById(R.id.refresh_layout);
        mMessage = view.findViewById(R.id.message);
        mStartTaskFab = ((MainActivity)getActivity()).startTaskFab;
        mStartTaskFab.setOnClickListener(v -> startCorrection(-1));
        mStartTaskFab.hide();

        //attach adapter recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean isAutoMeasureEnabled() {
                return false;
            }
        };
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setSmoothScrollbarEnabled(true);
        layoutManager.setInitialPrefetchItemCount(10);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
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
                boolean isPresentSD = storageHelper.isPresentRemovableStorage();
                if(AndroidUtils.getUriSD(getActivity()) == null && isPresentSD) {
                    startActivityForResult(new Intent(getActivity(), SdCardInstructionsActivity.class),
                            RequiredPermissions.REQUEST_PERMISSION_SAF);
                }
                else {
                    mViewModel.fetchTracks(null);
                }
            }
        });

        //Color of background and progress tint of progress bar of refresh layout
        mSwipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(requireActivity(), R.color.progressTintSwipeRefreshLayout)
        );
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(requireActivity().
                getResources().getColor(R.color.progressSwipeRefreshLayoutBackgroundTint));
        mHasPermission = ContextCompat.
                checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        if(!mHasPermission) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION);
            mRecyclerView.setVisibility(View.GONE);
        }
        else {
            boolean isPresentSD = storageHelper.isPresentRemovableStorage();
            if(AndroidUtils.getUriSD(requireActivity()) == null && isPresentSD) {
                startActivityForResult(new Intent(requireActivity(), SdCardInstructionsActivity.class),
                        RequiredPermissions.REQUEST_PERMISSION_SAF);
            }
            else {
                mViewModel.fetchTracks(null);
            }
            mRecyclerView.setVisibility(View.VISIBLE);
            mMessage.setText(R.string.loading_tracks);
        }
        //App is opened again, then scroll to the track being processed.
        /*int id = getActivity().getIntent().getIntExtra(Constants.MEDIA_STORE_ID, -1);
        int pos = mListViewModel.getTrackPosition(id);
        mRecyclerView.scrollToPosition(pos);*/
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mSwipeRefreshLayout.setRefreshing(false);
        if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF) {
            mViewModel.fetchTracks(null);
        }
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
                    mViewModel.checkAllTracks();
                break;
            case R.id.action_search:
                    ResultSearchFragment resultSearchListFragment = (ResultSearchFragment)
                            getParentFragmentManager().findFragmentByTag(ResultSearchFragment.class.getName());

                    if(resultSearchListFragment == null) {
                        resultSearchListFragment = ResultSearchFragment.newInstance();
                    }

                ResultSearchFragment finalResultSearchListFragment = resultSearchListFragment;

                ((MainActivity)requireActivity()).startTaskFab.hide(new ExtendedFloatingActionButton.OnChangedCallback() {
                    @Override
                    public void onHidden(ExtendedFloatingActionButton extendedFab) {
                        getParentFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                        R.anim.slide_in_left, R.anim.slide_out_right)
                                .addToBackStack(ResultSearchFragment.class.getName())
                                .hide(MainFragment.this)
                                .add(R.id.container_fragments, finalResultSearchListFragment,
                                        ResultSearchFragment.class.getName())
                                .commit();
                    }
                });


                break;
            case R.id.action_refresh:
                    rescan();
                break;
            case R.id.path_asc:
                    mViewModel.fetchTracks(new TrackRepository.Sort(TrackContract.TrackData.DATA,
                                    TrackRepository.ASC, id));
                break;
            case R.id.path_desc:
                    mViewModel.fetchTracks(new TrackRepository.Sort(
                                    TrackContract.TrackData.DATA,
                                    TrackRepository.DESC,
                                    id)
                            );
                break;
            case R.id.title_asc:
                    mViewModel.fetchTracks(new TrackRepository.Sort(
                                    TrackContract.TrackData.TITLE,
                                    TrackRepository.ASC,
                                    id)
                            );
                break;
            case R.id.title_desc:
                    mViewModel.fetchTracks(new TrackRepository.Sort(
                                    TrackContract.TrackData.TITLE,
                                    TrackRepository.DESC,
                                    id)
                            );
                break;
            case R.id.artist_asc:
                    mViewModel.fetchTracks(new TrackRepository.Sort(
                                    TrackContract.TrackData.ARTIST,
                                    TrackRepository.ASC,
                                    id)
                            );
                break;
            case R.id.artist_desc:
                    mViewModel.fetchTracks(new TrackRepository.Sort(
                                    TrackContract.TrackData.ARTIST,
                                    TrackRepository.DESC,
                                    id)
                            );
                break;
            case R.id.album_asc:
                    mViewModel.fetchTracks(new TrackRepository.Sort(
                                    TrackContract.TrackData.ALBUM,
                                    TrackRepository.ASC,
                                    id)
                            );
                break;
            case R.id.album_desc:
                    mViewModel.fetchTracks(new TrackRepository.Sort(
                                    TrackContract.TrackData.ALBUM,
                                    TrackRepository.DESC,
                                    id)
                            );
                break;
        }
        return super.onOptionsItemSelected(menuItem);
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
            mViewModel.fetchTracks(null);
        }
    }

    @Override
    protected ListViewModel getViewModel() {
        return new ViewModelProvider(this, androidViewModelFactory).get(ListViewModel.class);
    }

    @Override
    protected void loading(boolean isLoading) {
        if(isLoading) {
            mRecyclerView.setEnabled(false);
        }
        else {
            mRecyclerView.setEnabled(false);
        }
        mSwipeRefreshLayout.setRefreshing(isLoading);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //Check permission to access files and execute scan if were granted
        mHasPermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if(mHasPermission){
            boolean isPresentSD = storageHelper.isPresentRemovableStorage();
            if(AndroidUtils.getUriSD(getActivity()) == null && isPresentSD) {
                startActivityForResult(new Intent(getActivity(), SdCardInstructionsActivity.class),
                        RequiredPermissions.REQUEST_PERMISSION_SAF);
            }
            else {
                mViewModel.fetchTracks(null);
            }
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        else {
            mSwipeRefreshLayout.setEnabled(true);
            mStartTaskFab.hide();
            mMessage.setVisibility(View.VISIBLE);
            mMessage.setText(R.string.permission_denied);
            mViewModel.setLoading(false);
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
                    /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        mListViewModel.scan();
                    }*/
                    if(isVisible())
                        updateToolbar(mCurrentTracks);
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
                mViewModel.fetchTracks(null);
        }
        return animation;
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
        viewWrapper.view = view;
        viewWrapper.position = position;
        viewWrapper.mode = CorrectionActions.VIEW_INFO;
        mViewModel.onItemClick(viewWrapper);
    }

    @Override
    public void onCheckboxClick(int position) {
        mViewModel.onCheckboxClick(position);
    }

    @Override
    public void onItemClick(int position, View view) {
        ViewWrapper viewWrapper = new ViewWrapper();
        viewWrapper.view = view;
        viewWrapper.position = position;
        viewWrapper.mode = CorrectionActions.SEMI_AUTOMATIC;
        mViewModel.onItemClick(viewWrapper);
    }

    /**
     * Opens new activity showing up the details from current audio item list pressed
     * @param viewWrapper a wrapper object containing the track, view , and mode of correction.
     */
    private void openDetails(ViewWrapper viewWrapper){
        mRecyclerView.stopScroll();
        openFragment(viewWrapper);
    }

    private void openFragment(ViewWrapper viewWrapper){
        Intent intent = new Intent(getActivity(), TrackDetailActivity.class);
        Bundle bundle = AndroidUtils.getBundle(viewWrapper.track.getMediaStoreId(),
                viewWrapper.mode);
        intent.putExtra(TrackDetailActivity.TRACK_DATA, bundle);
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity(), viewWrapper.view, "cover_art_element").toBundle());
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
                        t.setDuration(LENGTH_SHORT);
                        t.setText(R.string.cancelling);
                        t.show();
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
                        mViewModel.removeTrack(viewWrapper.position);
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
        intent.setAction(Constants.Actions.ACTION_START_TASK);
        Objects.requireNonNull(getActivity()).startService(intent);
    }

    private void onCheckAll(Boolean checkAll) {
        if(!checkAll)
            mViewModel.checkAllItems();
    }

    @Override
    public void onStartAutomaticTask() {
        mStartTaskFab.hide();
        mStopCorrectionSnackbar = AndroidUtils.createNoDismissibleSnackbar(getView(), R.string.correction_in_progress);
        mStopCorrectionSnackbar.setAction(R.string.cancel, v -> {
            Intent stopIntent = new Intent(getActivity(), FixerTrackService.class);
            stopIntent.setAction(Constants.Actions.ACTION_STOP_TASK);
            requireActivity().startService(stopIntent);
            Toast t = AndroidUtils.getToast(requireActivity());
            t.setDuration(LENGTH_SHORT);
            t.setText(R.string.cancelling);
            t.show();
        });
        mStopCorrectionSnackbar.show();
    }

    @Override
    public void onStartProcessingFor(int id) {
        int index = mViewModel.getTrackPosition(id);
        if(index != -1)
            mRecyclerView.scrollToPosition(index);
    }

    @Override
    public void onFinishedAutomaticTask() {
        mStartTaskFab.show();
        if (mStopCorrectionSnackbar != null)
            mStopCorrectionSnackbar.dismiss();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((MainActivity)getActivity()).mDrawerLayout.removeDrawerListener(((MainActivity)getActivity()).actionBarDrawerToggle);
    }

    private void onMessage(Integer integer) {
        Snackbar snackbar = AndroidUtils.getSnackbar(mSwipeRefreshLayout,
                getActivity().getApplicationContext());
        snackbar.setText(integer);
        snackbar.show();
    }

    private void updateToolbar(List<Track> tracks) {
        if(tracks == null)
            return;

        if(!mHasPermission) {
            ((MainActivity)getActivity()).mainToolbar.setTitle(R.string.title_activity_main);
            ((MainActivity)getActivity()).actionBar.setTitle(R.string.title_activity_main);
            return;
        }

        if(tracks.isEmpty()) {
            mStartTaskFab.hide();
            mMessage.setVisibility(View.VISIBLE);
            mMessage.setText(R.string.no_items_found);
            ((MainActivity)getActivity()).mainToolbar.setTitle(R.string.title_activity_main);
            ((MainActivity)getActivity()).actionBar.setTitle(R.string.title_activity_main);
        }
        else {
            boolean isServiceRunning = serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME);
            if(!isServiceRunning){
                mStartTaskFab.show();
            }
            else {
                mStartTaskFab.hide();
                onStartAutomaticTask();
            }
            ((MainActivity)getActivity()).mainToolbar.setTitle(tracks.size() + " " +getString(R.string.tracks));
            ((MainActivity)getActivity()).actionBar.setTitle(tracks.size() + " " +getString(R.string.tracks));
            mMessage.setVisibility(View.GONE);
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
        mStartTaskFab.hide();
        mMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCurrentListChanged(@NonNull List<List<Track>> previousList, @NonNull List<List<Track>> currentList) {
        mSwipeRefreshLayout.setRefreshing(false);
        if (mViewModel.isSortingOperation())
            mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onIncomingMessageListener(String message) {
        AndroidUtils.showToast(message, requireActivity());
    }
}
