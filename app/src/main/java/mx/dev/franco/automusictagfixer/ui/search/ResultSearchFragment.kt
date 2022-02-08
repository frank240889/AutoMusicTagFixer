package mx.dev.franco.automusictagfixer.ui.search

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.ui.BaseViewModelFragment
import mx.dev.franco.automusictagfixer.ui.main.ViewWrapper
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailActivity
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils

class ResultSearchFragment : BaseViewModelFragment<SearchListViewModel>(),
    FoundItemHolder.ClickListener {
    //A simple texview to show a message when no songs were identificationFound
    private var mMessage: TextView? = null

    //recycler view is a component that delivers
    //better performance with huge data sources
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: SearchTrackAdapter? = null
    private var mQuery: String? = null


    companion object {
        val TAG = ResultSearchFragment::class.java.name
        fun newInstance(): ResultSearchFragment {
            return ResultSearchFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mAdapter = SearchTrackAdapter(this)
        /*obtainViewModel.searchResults.observe(this, { tracks: List<Track>? -> onSearchResults(tracks) })
        obtainViewModel.searchResults.observe(this, mAdapter!!)
        obtainViewModel.isTrackProcessing.observe(this, { s: String -> showMessageError(s) })
        obtainViewModel.actionTrackEvaluatedSuccessfully()
            .observe(this, { viewWrapper: ViewWrapper -> openDetailTrack(viewWrapper) })
        obtainViewModel.actionIsTrackInaccessible()
            .observe(this, { viewWrapper: ViewWrapper -> showInaccessibleTrack(viewWrapper) })
        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            })*/
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result_search_list, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /*(activity as MainActivity?)!!.searchBox!!.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                mQuery = (activity as MainActivity?)!!.searchBox!!.text.toString()
                obtainViewModel!!.search(mQuery)
                hideKeyboard()
            }
            false
        }*/

        //attach adapter to our recyclerview
        mRecyclerView = view.findViewById(R.id.found_tracks_recycler_view)
        mMessage = view.findViewById(R.id.found_message)
        val line = LinearLayoutManager(activity)
        mRecyclerView!!.layoutManager = line
        mRecyclerView!!.setHasFixedSize(true)
        mRecyclerView!!.setItemViewCacheSize(10)
        mRecyclerView!!.isHapticFeedbackEnabled = true
        mRecyclerView!!.isSoundEffectsEnabled = true
        mRecyclerView!!.adapter = mAdapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        /*(activity as MainActivity?)!!.mDrawerLayout!!.removeDrawerListener((activity as MainActivity?)!!.actionBarDrawerToggle!!)
        (activity as MainActivity?)!!.mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        (activity as MainActivity?)!!.actionBar!!.setDisplayHomeAsUpEnabled(false)
        (activity as MainActivity?)!!.actionBarDrawerToggle!!.isDrawerIndicatorEnabled = false
        (activity as MainActivity?)!!.actionBar!!.setDisplayHomeAsUpEnabled(true)
        (activity as MainActivity?)!!.actionBarDrawerToggle!!.toolbarNavigationClickListener =
            View.OnClickListener {
                hideKeyboard()
                parentFragmentManager.popBackStack()
            }
        (activity as MainActivity?)!!.searchBox!!.visibility = View.VISIBLE
        (activity as MainActivity?)!!.searchBox!!.requestFocus()
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(
            (activity as MainActivity?)!!.searchBox,
            InputMethodManager.SHOW_IMPLICIT
        )*/
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            imm.hideSoftInputFromWindow(requireActivity().currentFocus!!.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showInaccessibleTrack(viewWrapper: ViewWrapper) {
        /*val content = String.format(getString(R.string.file_error), viewWrapper.track!!.path)
        val informativeFragmentDialog = InformativeFragmentDialog.newInstance(
            getString(R.string.attention),
            content,
            getString(R.string.remove_from_list),
            null
        )
        informativeFragmentDialog.setOnClickBasicFragmentDialogListener(object :InformativeFragmentDialog.OnClickBasicFragmentDialogListener {
            override fun onPositiveButton() {
                obtainViewModel!!.removeTrack(viewWrapper.position)
            }

            override fun onNegativeButton() {
               informativeFragmentDialog.dismiss()
            }
        })
        informativeFragmentDialog.show(childFragmentManager, informativeFragmentDialog.tag)*/
    }

    private fun openDetailTrack(viewWrapper: ViewWrapper) {

        //to hide it, call the method again
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            imm.hideSoftInputFromWindow(requireActivity().currentFocus!!.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val intent = Intent(activity, TrackDetailActivity::class.java)
        val bundle = AndroidUtils.getBundle(
            viewWrapper.track!!.mediaStoreId,
            viewWrapper.mode
        )
        intent.putExtra(TrackDetailActivity.TRACK_DATA, bundle)
        startActivity(
            intent, ActivityOptions.makeSceneTransitionAnimation(
                activity,
                viewWrapper.view, "cover_art_element"
            ).toBundle()
        )
    }

    private fun showMessageError(s: String) {
        val snackbar = AndroidUtils.createSnackbar(mRecyclerView!!, true)
        snackbar.setText(s)
        snackbar.show()
    }

    private fun onSearchResults(tracks: List<Track>?) {
        if (tracks != null) {
            if (tracks.isNotEmpty()) {
                mMessage!!.visibility = View.GONE
            } else {
                if (mQuery != null) {
                    val toast = AndroidUtils.getToast(activity)
                    toast.duration = Toast.LENGTH_SHORT
                    toast.setText(String.format(getString(R.string.no_found_items), mQuery))
                    toast.show()
                }
                mMessage!!.visibility = View.VISIBLE
            }
        }
        mRecyclerView!!.scrollToPosition(0)
    }

    override fun onPause() {
        super.onPause()
        mRecyclerView!!.stopScroll()
    }

    override fun onItemClick(position: Int, view: View?) {
        mRecyclerView!!.stopScroll()
        /*ViewWrapper viewWrapper = new ViewWrapper();
        viewWrapper.view = view;
        viewWrapper.position = position;
        viewWrapper.mode = CorrectionActions.SEMI_AUTOMATIC;
        viewModel.onItemClick(viewWrapper);*/
    }

    override fun onDestroy() {
        super.onDestroy()
        mRecyclerView!!.stopScroll()
        mMessage = null
        mRecyclerView = null
        mAdapter = null
        //obtainViewModel.cancelSearch()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        var animation = super.onCreateAnimation(transit, enter, nextAnim)
        if (animation == null && nextAnim != 0) {
            /*animation = AnimationUtils.loadAnimation(activity, nextAnim)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    if (isRemoving) {
                        (activity as MainActivity?)!!.searchBox!!.visibility = View.GONE
                        (activity as MainActivity?)!!.searchBox!!.setText("")
                        (activity as MainActivity?)!!.searchBox!!.setOnEditorActionListener(null)
                        (activity as MainActivity?)!!.mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                        (activity as MainActivity?)!!.actionBarDrawerToggle!!.isDrawerIndicatorEnabled =
                            true
                        //((MainActivity)getActivity()).actionBar.setDisplayHomeAsUpEnabled(false);
                    }
                }

                override fun onAnimationEnd(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
            })*/
        }
        if (animation != null && view != null) requireView().setLayerType(View.LAYER_TYPE_HARDWARE, null)
        return animation
    }

    override fun obtainViewModel() = ViewModelProvider(this, androidViewModelFactory).get(
        SearchListViewModel::class.java
    )
}