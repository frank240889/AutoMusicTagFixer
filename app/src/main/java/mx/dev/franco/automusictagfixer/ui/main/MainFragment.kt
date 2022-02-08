package mx.dev.franco.automusictagfixer.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.view.View.INVISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.databinding.FragmentMainBinding
import mx.dev.franco.automusictagfixer.fixer.StorageHelper
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.persistence.room.database.TrackContract
import mx.dev.franco.automusictagfixer.ui.BaseViewModelFragment
import mx.dev.franco.automusictagfixer.ui.InformativeFragmentDialog
import mx.dev.franco.automusictagfixer.ui.InformativeFragmentDialog.OnClickBasicFragmentDialogListener
import mx.dev.franco.automusictagfixer.ui.sdcardinstructions.SdCardInstructionsActivity
import mx.dev.franco.automusictagfixer.ui.search.ResultSearchFragment
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailActivity
import mx.dev.franco.automusictagfixer.utilities.*
import javax.inject.Inject

class MainFragment : BaseViewModelFragment<ListViewModel>(), ListListener<List<Track?>?>, AudioItemHolder.ClickListener {

    @Inject
    lateinit var storageHelper: StorageHelper

    private var _viewBinding: FragmentMainBinding? = null

    private val viewBinding: FragmentMainBinding get() = _viewBinding!!

    private val trackAdapter: TrackAdapter by lazy {
        TrackAdapter(this)
    }

    private lateinit var menu: Menu

    private var hasRequiredPermissions = false

    companion object {

        const val DEFAULT_ITEM_CHECKED = -1

        private val TAG = MainFragment::class.java.name

        fun newInstance() = MainFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentMainBinding.inflate(inflater, container, false).apply {
            _viewBinding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupRefreshLayout()
        verifyPermissions()
    }

    private fun verifyPermissions() {
        hasRequiredPermissions = (
                ContextCompat
                    .checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED)

        if (!hasRequiredPermissions) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE),
                RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION)
            viewBinding.tracksRecyclerView.visibility = View.GONE
        } else {
            val isPresentSD = storageHelper.isPresentRemovableStorage

            if (AndroidUtils.getUriSD(requireActivity()) == null && isPresentSD) {
                startActivityForResult(Intent(requireActivity(), SdCardInstructionsActivity::class.java),
                    RequiredPermissions.REQUEST_PERMISSION_SAF)
            }
            else {
                viewModel.fetchAudioFilesFromStorage()
            }

            viewBinding.tracksRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupRefreshLayout() {
        viewBinding.rlList.apply {
            setOnRefreshListener {
                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    viewBinding.tracksRecyclerView.visibility = INVISIBLE
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION)
                } else {
                    val isPresentSD = viewModel.isPresentRemovableStorage()
                    if (AndroidUtils.getUriSD(activity) == null && isPresentSD) {
                        startActivityForResult(Intent(activity, SdCardInstructionsActivity::class.java),
                            RequiredPermissions.REQUEST_PERMISSION_SAF)
                    } else {
                        viewModel.fetchAudioFilesFromStorage()
                    }
                }
            }
            //Color of background and progress tint of progress bar of refresh layout
            setColorSchemeColors(
                ContextCompat.getColor(requireActivity(), R.color.progressTintSwipeRefreshLayout)
            )

            setProgressBackgroundColorSchemeColor(
                ContextCompat
                    .getColor(requireActivity(),R.color.progressSwipeRefreshLayoutBackgroundTint)
            )
        }
    }

    private fun setupRecyclerView() {
        viewBinding.tracksRecyclerView.apply {
            addItemDecoration(DividerItemDecoration(requireActivity(), RecyclerView.VERTICAL))
            layoutManager = object : LinearLayoutManager(activity) {
                override fun isAutoMeasureEnabled(): Boolean {
                    return false
                }
            }.apply {
                isItemPrefetchEnabled = true
                isSmoothScrollbarEnabled = true
                initialPrefetchItemCount = 10
            }
            setHasFixedSize(true)
            isHapticFeedbackEnabled = true
            isSoundEffectsEnabled = true
            adapter = trackAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        Glide.with(requireActivity()).resumeRequests()
                    }
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING
                        || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                        Glide.with(requireActivity()).pauseRequests()
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewBinding.rlList.isRefreshing = false
        if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF) {
            viewModel.fetchAudioFilesFromStorage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_main_activity, menu)
        this.menu = menu
        checkItem(DEFAULT_ITEM_CHECKED)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {

        val id = menuItem.itemId

        stopScroll()

        when (id) {

            R.id.action_search -> {
                var resultSearchListFragment =
                    parentFragmentManager
                        .findFragmentByTag(ResultSearchFragment::class.java.name) as ResultSearchFragment?

                if (resultSearchListFragment == null) {
                    resultSearchListFragment = ResultSearchFragment.newInstance()
                }

                val finalResultSearchListFragment = resultSearchListFragment
                parentFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                R.anim.slide_in_left, R.anim.slide_out_right)
                        .addToBackStack(ResultSearchFragment::class.java.name)
                        .hide(this@MainFragment)
                        .add(R.id.fl_container_fragments, finalResultSearchListFragment!!,
                                ResultSearchFragment::class.java.name)
                        .commit()
            }

            R.id.action_refresh -> rescan()

            R.id.path_asc -> viewModel.sort(TrackRepository.Sort(TrackContract.TrackData.DATA,
                    TrackRepository.ASC, id))

            R.id.path_desc -> viewModel.sort(TrackRepository.Sort(
                    TrackContract.TrackData.DATA,
                    TrackRepository.DESC,
                    id)
            )

            R.id.title_asc -> viewModel.sort(TrackRepository.Sort(
                    TrackContract.TrackData.TITLE,
                    TrackRepository.ASC,
                    id)
            )

            R.id.title_desc -> viewModel.sort(TrackRepository.Sort(
                    TrackContract.TrackData.TITLE,
                    TrackRepository.DESC,
                    id)
            )

            R.id.artist_asc -> viewModel.sort(TrackRepository.Sort(
                    TrackContract.TrackData.ARTIST,
                    TrackRepository.ASC,
                    id)
            )

            R.id.artist_desc -> viewModel.sort(TrackRepository.Sort(
                    TrackContract.TrackData.ARTIST,
                    TrackRepository.DESC,
                    id)
            )

            R.id.album_asc -> viewModel.sort(TrackRepository.Sort(
                    TrackContract.TrackData.ALBUM,
                    TrackRepository.ASC,
                    id)
            )

            R.id.album_desc -> viewModel.sort(TrackRepository.Sort(
                    TrackContract.TrackData.ALBUM,
                    TrackRepository.DESC,
                    id)
            )
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun rescan() {
        val hasPermission = (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
        if (!hasPermission) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION)
        } else {
            viewModel.fetchAudioFilesFromStorage()
        }
    }

    override fun loading(isLoading: Boolean) {
        viewBinding.apply {
            if (isLoading) {
                tracksRecyclerView.isEnabled = false
            } else {
                tracksRecyclerView.isEnabled = false
            }
            rlList.isRefreshing = isLoading
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        //Check permission to access files and execute scan if were granted
        hasRequiredPermissions = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (hasRequiredPermissions) {
            val isPresentSD = storageHelper.isPresentRemovableStorage
            if (AndroidUtils.getUriSD(activity) == null && isPresentSD) {
                startActivityForResult(Intent(activity, SdCardInstructionsActivity::class.java),
                        RequiredPermissions.REQUEST_PERMISSION_SAF)
            }
            else {
                viewModel.fetchAudioFilesFromStorage()
            }
            viewBinding.tracksRecyclerView.visibility = View.VISIBLE
        }
        else {
            viewBinding.apply {
                tvMessageNoTracks.visibility = View.VISIBLE
                tracksRecyclerView.visibility = INVISIBLE
                tvMessageNoTracks.setText(R.string.permission_denied)
                showViewPermissionMessage()
            }
        }
    }

    private fun showViewPermissionMessage() {
        val informativeFragmentDialog = InformativeFragmentDialog.newInstance(R.string.title_dialog_permision,
                R.string.explanation_permission_access_files,
                R.string.accept, R.string.cancel_button, requireActivity())
        informativeFragmentDialog.show(childFragmentManager,
                informativeFragmentDialog.javaClass.canonicalName)
        informativeFragmentDialog.setOnClickBasicFragmentDialogListener(
                object : OnClickBasicFragmentDialogListener {
                    override fun onPositiveButton() {
                        informativeFragmentDialog.dismiss()
                        ActivityCompat.requestPermissions(requireActivity(),
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                RequiredPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION
                        )
                    }

                    override fun onNegativeButton() {
                        informativeFragmentDialog.dismiss()
                    }
                }
        )
    }

    private fun stopScroll() {
        viewBinding.tracksRecyclerView.stopScroll()
    }

    /**
     * Set the menu item with an icon to mark which type of sort is select
     * and saves to shared preferences to persist its value.
     * @param selectedItem The id of item selected.
     */
    private fun checkItem(selectedItem: Int) {
        val sharedPreferences = requireActivity().getSharedPreferences(
                Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE)
        //No previous value was found.
        if (selectedItem == DEFAULT_ITEM_CHECKED) {
            val currentSelectedItem = sharedPreferences.getInt(Constants.SELECTED_ITEM, -1)
            if (currentSelectedItem == -1) {
                val defaultMenuItemSelected = menu.findItem(R.id.title_asc)
                defaultMenuItemSelected.icon = ContextCompat.getDrawable(
                        requireActivity().applicationContext, R.drawable.ic_done_white)
                sharedPreferences.edit().putInt(Constants.SELECTED_ITEM,
                        defaultMenuItemSelected.itemId).apply()
                sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM,
                        defaultMenuItemSelected.itemId).apply()
            } else {
                val menuItemSelected = menu.findItem(currentSelectedItem)
                if (menuItemSelected != null) {
                    menuItemSelected.icon = ContextCompat.getDrawable(requireActivity().applicationContext, R.drawable.ic_done_white)
                    sharedPreferences.edit().putInt(Constants.SELECTED_ITEM,
                            menuItemSelected.itemId).apply()
                    sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM,
                            menuItemSelected.itemId).apply()
                }
            }
        } else {
            val lastItemSelected = sharedPreferences.getInt(Constants.LAST_SELECTED_ITEM, DEFAULT_ITEM_CHECKED)
            //User selected the same item.
            if (selectedItem == lastItemSelected) return
            val menuItemSelected = menu.findItem(selectedItem)
            val lastMenuItemSelected = menu.findItem(lastItemSelected)
            //Clear last selected
            if (lastMenuItemSelected != null) lastMenuItemSelected.icon = null
            var selectedMenuItem = -1
            if (menuItemSelected != null) {
                menuItemSelected.icon = ContextCompat.getDrawable(requireActivity().applicationContext,
                        R.drawable.ic_done_white)
                selectedMenuItem = menuItemSelected.itemId
            }
            sharedPreferences.edit().putInt(Constants.SELECTED_ITEM, selectedMenuItem).apply()
            sharedPreferences.edit().putInt(Constants.LAST_SELECTED_ITEM, selectedMenuItem).apply()
        }
    }

    /**
     * Run when the scan of media store has finished and no music files have
     * been found.
     * @param voids void param, not usable.
     */
    private fun noResultFilesFound(voids: Void) {
        viewBinding.tvMessageNoTracks.visibility = View.VISIBLE
    }

    private fun addObservers() {
        viewModel.apply {

            observeActionCanOpenDetails().observe(this@MainFragment, { track: Track ->
                startActivity(Intent(activity, TrackDetailActivity::class.java).apply {
                    putExtra(Constants.MEDIA_STORE_ID, track.mediaStoreId)
                })
            })

            observeIsTrackInaccessible().observe(this@MainFragment, { track: Track ->
                InformativeFragmentDialog
                    .newInstance(
                        getString(R.string.attention),
                        String.format(getString(R.string.file_error),
                            track.path
                        ),
                        getString(R.string.remove_from_list), null
                    ).apply {
                        setOnClickBasicFragmentDialogListener(
                            object : OnClickBasicFragmentDialogListener {
                                override fun onPositiveButton() {
                                    viewModel.removeTrack(track)
                                }
                                override fun onNegativeButton() {
                                    dismiss()
                                }
                            }
                        )
                    }
                    .show(childFragmentManager,
                        InformativeFragmentDialog::class.java.name)

            })

            observeLoadingState().observe(this@MainFragment, { isLoading: Boolean ->
                viewBinding.rlList.isRefreshing = isLoading
            })

            tracks.observe(this@MainFragment, {
                viewBinding.apply {
                    if (it.isEmpty()) {
                        tvMessageNoTracks.visibility = View.VISIBLE
                        tvMessageNoTracks.setText(R.string.no_items_found)
                    }
                    else {
                        tvMessageNoTracks.visibility = View.INVISIBLE
                        tvMessageNoTracks.text = ""
                    }
                }

                (requireActivity() as? AppCompatActivity?)?.supportActionBar?.title =
                    String.format(getString(R.string.tracks), it.size)
            })

            tracks.observe(this@MainFragment, trackAdapter)
        }
    }

    override fun onCurrentListChanged(previousList: List<List<Track?>?>, currentList: List<List<Track?>?>) {
        viewBinding.rlList.isRefreshing = false
    }

    override fun onItemClick(position: Int, view: View?) {
        viewModel.onItemClick(position)
    }

    override fun obtainViewModel() = ViewModelProvider(
        this,
        androidViewModelFactory
    )[ListViewModel::class.java]
}