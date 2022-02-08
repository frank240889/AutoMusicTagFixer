package mx.dev.franco.automusictagfixer.ui.trackdetail
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import mx.dev.franco.automusictagfixer.BuildConfig
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.common.Action
import mx.dev.franco.automusictagfixer.databinding.ActivityTrackDetailBinding
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams
import mx.dev.franco.automusictagfixer.ui.AndroidViewModelFactory
import mx.dev.franco.automusictagfixer.ui.InformativeFragmentDialog
import mx.dev.franco.automusictagfixer.ui.sdcardinstructions.SdCardInstructionsActivity
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils
import mx.dev.franco.automusictagfixer.utilities.Constants
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions
import mx.dev.franco.automusictagfixer.utilities.SnackbarMessage
import javax.inject.Inject
import kotlin.math.abs


open class TrackDetailActivity : AppCompatActivity(),
    ManualCorrectionDialogFragment.OnManualCorrectionListener, HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var androidViewModelFactory: AndroidViewModelFactory

    //@Inject
    //lateinit var mPlayer: SimpleMediaPlayer

    private var mPlayPreviewMenuItem: MenuItem? = null
    private var mManualEditMenuItem: MenuItem? = null
    private var mSearchInWebMenuItem: MenuItem? = null
    private var mTrackDetailsMenuItem: MenuItem? = null
    private var mTrackDetailFragment: TrackDetailFragment? = null
    private var mTrackDetailViewModel: TrackDetailViewModel? = null
    private lateinit var dataBinding: ActivityTrackDetailBinding
    private var mActionBar: ActionBar? = null
    @JvmField
    var mEditMode = false
    private var mNoDismissibleSnackbar: Snackbar? = null
    private var mRenameTrackItem: MenuItem? = null
    private var mOffsetChangeListener: AppBarLayout.OnOffsetChangedListener? = null
    private var mReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_track_detail)
        mTrackDetailViewModel = ViewModelProvider(
            this,
            androidViewModelFactory
        )[TrackDetailViewModel::class.java]

        dataBinding.viewModel = mTrackDetailViewModel
        dataBinding.lifecycleOwner = this

        mTrackDetailViewModel!!.observeLoadingMessage().observe(this,
            { message: Int -> this.onLoadingMessage(message) })
        /*mTrackDetailViewModel!!.observeErrorWriting().observe(
            this,
            Observer { snackbarMessage: SnackbarMessage<*> -> showSnackbarMessage(snackbarMessage) })*/

        mTrackDetailViewModel!!.observeConfirmationRemoveCover()
            .observe(this, { voids: Void -> onConfirmRemovingCover(voids) })

        mTrackDetailViewModel!!.observeCoverSavingResult().observe(this) {
                snackbarMessage: SnackbarMessage<String> ->
            showSnackbarMessage(snackbarMessage)
        }

        mTrackDetailViewModel!!.onMessage()
            .observe(this, { s: String -> this.onLoadingMessage(s) })

        setSupportActionBar(dataBinding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setTitle(R.string.details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        dataBinding.collapsingToolbarLayout.isTitleEnabled = false
        hideFabs()

        if (savedInstanceState == null)
            mTrackDetailFragment = TrackDetailFragment
                .newInstance(intent!!.getIntExtra(Constants.MEDIA_STORE_ID, -1))

        supportFragmentManager.beginTransaction().replace(
            R.id.track_detail_container_fragments, mTrackDetailFragment!!,
            mTrackDetailFragment!!.javaClass.name
        ).commit()

        //setupMediaPlayer();
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_details_track_dialog, menu)
        mPlayPreviewMenuItem = menu.findItem(R.id.action_play)
        mManualEditMenuItem = menu.findItem(R.id.action_edit_manual)
        mSearchInWebMenuItem = menu.findItem(R.id.action_web_search)
        mTrackDetailsMenuItem = menu.findItem(R.id.action_details)
        mRenameTrackItem = menu.findItem(R.id.action_rename)
        mTrackDetailViewModel!!.observeReadingResult()
            .observe(this, { voids: Unit -> onSuccessLoad(voids) })
        mTrackDetailViewModel!!.observeAudioData().observe(this, { aVoid: Void? -> })
        mTrackDetailViewModel!!.observeInvalidInputsValidation().observe(
            this,
            Observer { validationWrapper: ValidationWrapper -> onInputDataInvalid(validationWrapper) })
        mTrackDetailViewModel!!.observeWritingFinishedEvent()
            .observe(this, { voids: Void -> onWritingResult(voids) })
        mTrackDetailViewModel!!.observeLoadingState()
            .observe(this, { showProgress: Boolean -> loading(showProgress) })
        setupIdentificationObserves()
        return true
    }

    private fun onInputDataInvalid(validationWrapper: ValidationWrapper) {
        dataBinding.fabIdentification.hide()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        mTrackDetailFragment =
            supportFragmentManager.findFragmentByTag(TrackDetailFragment::class.java.name) as TrackDetailFragment?
        if (mEditMode) editMode()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mEditMode = savedInstanceState.getBoolean("edit_mode", false)
    }

    override fun onStop() {
        super.onStop()
        //mPlayer.stopPreview();
    }

    override fun onBackPressed() {
        if (mEditMode) {
            enableEditModeElements()
            enableAppBarLayout()
            mTrackDetailFragment!!.disableFields()
            mTrackDetailViewModel!!.restorePreviousValues()
            showFabs()
        } else {
            dataBinding!!.appBarLayout.removeOnOffsetChangedListener(mOffsetChangeListener)
            if (dataBinding!!.appBarLayout.height - dataBinding!!.appBarLayout.bottom > 0) {
                dataBinding!!.appBarLayout.addOnOffsetChangedListener(object :
                    AppBarLayout.OnOffsetChangedListener {
                    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                        if (verticalOffset == 0) {
                            dataBinding!!.appBarLayout.removeOnOffsetChangedListener(this)
                            super@TrackDetailActivity.onBackPressed()
                        }
                    }
                })
                dataBinding!!.appBarLayout.setExpanded(true, true)
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mNoDismissibleSnackbar != null) mNoDismissibleSnackbar!!.dismiss()
    }

    /**
     * Callback for processing the result from startActivityForResult call,
     * here we process the image picked by user and apply to audio file
     * @param requestCode is the code from what component
     * makes the request this can be snack bar,
     * toolbar or text "Añadir caratula de galería"
     * @param resultCode result code is the action requested, in this case
     * Intent.ACTION_PICK
     * @param data Data received, can be null
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            INTENT_GET_AND_UPDATE_FROM_GALLERY, INTENT_OPEN_GALLERY -> if (data != null) {
                val imageData: Uri = data.data!!
                val asyncBitmapDecoder = AndroidUtils.AsyncBitmapDecoder()
                dataBinding!!.appBarLayout.setExpanded(true, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(
                        applicationContext.contentResolver,
                        imageData
                    )
                    asyncBitmapDecoder.decodeBitmap(source, getBitmapDecoderCallback(requestCode))
                } else {
                    asyncBitmapDecoder.decodeBitmap(
                        applicationContext.contentResolver,
                        imageData, getBitmapDecoderCallback(requestCode)
                    )
                }
            }
            RequiredPermissions.REQUEST_PERMISSION_SAF -> {
                val msg: String = if (resultCode == Activity.RESULT_OK) {
                    // The document selected by the user won't be returned in the intent.
                    // Instead, a URI to that document will be contained in the return intent
                    // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
                    val res: Boolean = AndroidUtils.grantPermissionSD(getApplicationContext(), data)
                    if (res) {
                        getString(R.string.toast_apply_tags_again)
                    } else {
                        getString(R.string.could_not_get_permission)
                    }
                } else {
                    getString(R.string.saf_denied)
                }
                AndroidUtils.createSnackbar(dataBinding!!.rootContainerDetails, true)
                    .setText(msg).show()
            }
        }
    }

    /**
     * Opens a dialog to select a image
     * to apply as new embed cover art.
     * @param codeIntent The code to distinguish if we pressed the cover toolbar,
     * the action button "Galería" from snackbar or "Añadir carátula de galería"
     * from main container.
     */
    fun editCover(codeIntent: Int) {
        val selectorImageIntent = Intent(Intent.ACTION_PICK)
        selectorImageIntent.setType("image/*")
        startActivityForResult(selectorImageIntent, codeIntent)
    }

    /**
     * Callback from [SemiAutoCorrectionDialogFragment] when
     * user pressed apply only missing tags button
     */
    fun onMissingTagsButton(correctionParams: CorrectionParams?) {
        //mPlayer.stopPreview();
        mTrackDetailViewModel!!.performCorrection(correctionParams!!)
    }

    /**
     * Callback from [SemiAutoCorrectionDialogFragment] when
     * user pressed apply all tags button
     */
    fun onOverwriteTagsButton(correctionParams: CorrectionParams?) {
        //mPlayer.stopPreview();
        mTrackDetailViewModel!!.performCorrection(correctionParams!!)
    }

    override fun onManualCorrection(correctionParams: CorrectionParams?) {
        //mPlayer.stopPreview();
        mTrackDetailViewModel!!.performCorrection(correctionParams!!)
    }

    override fun onCancelManualCorrection() {
        enableEditModeElements()
        mTrackDetailFragment!!.disableFields()
        enableAppBarLayout()
        mTrackDetailViewModel!!.restorePreviousValues()
        showFabs()
    }

    fun saveAsImageButton(id: String?) {
        mTrackDetailViewModel!!.saveAsImageFileFrom(id)
    }

    fun saveAsCover(coverCorrectionParams: CorrectionParams?) {
        //mPlayer.stopPreview();
        mTrackDetailViewModel!!.performCorrection(coverCorrectionParams!!)
    }

    private fun getBitmapDecoderCallback(requestCode: Int): AndroidUtils.AsyncBitmapDecoder.AsyncBitmapDecoderCallback {
        return object : AndroidUtils.AsyncBitmapDecoder.AsyncBitmapDecoderCallback {
            override fun onBitmapDecoded(bitmap: Bitmap) {
                val imageWrapper = ImageWrapper()
                imageWrapper.width = bitmap.getWidth()
                imageWrapper.height = bitmap.getHeight()
                imageWrapper.bitmap = bitmap
                imageWrapper.requestCode = requestCode
                mTrackDetailViewModel!!.fastCoverChange(imageWrapper)
            }

            override fun onDecodingError(throwable: Throwable) {
                val snackbar: Snackbar = AndroidUtils.createSnackbar(
                    dataBinding!!.rootContainerDetails,
                    true
                )
                val msg: String = getString(R.string.error_load_image) + ": " + throwable.message
                snackbar.setText(msg)
                snackbar.setDuration(Snackbar.LENGTH_SHORT)
                snackbar.show()
            }
        }
    }

    private fun setupMediaPlayer() {
        /*mPlayer.addListener(new SimpleMediaPlayer.OnMediaPlayerEventListener() {
            @Override
            public void onStartPlaying() {
                mPlayPreviewMenuItem.setIcon(
                        ContextCompat.getDrawable(TrackDetailActivity.this, R.drawable.ic_stop_white_24dp));
                addStopAction();
            }
            @Override
            public void onStopPlaying() {
                mPlayPreviewMenuItem.setIcon(
                        ContextCompat.getDrawable(TrackDetailActivity.this,R.drawable.ic_play_arrow_white_24px));
                addPlayAction();
            }
            @Override
            public void onCompletedPlaying() {
                mPlayPreviewMenuItem.setIcon(
                        ContextCompat.getDrawable(TrackDetailActivity.this,R.drawable.ic_play_arrow_white_24px));
                addPlayAction();
            }
            @Override
            public void onErrorPlaying(int what, int extra) {
                mPlayPreviewMenuItem.setEnabled(false);
            }
        });*/
    }

    /**
     * Alternates the stop to the play action.
     */
    private fun addPlayAction() {
        mPlayPreviewMenuItem!!.setOnMenuItemClickListener(null)
        mPlayPreviewMenuItem!!.setOnMenuItemClickListener { item: MenuItem? ->
            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getBoolean("key_use_embed_player", true)
            ) {
                /*try {
                    //mPlayer.playPreview();
                } catch (IOException e) {
                    Snackbar snackbar = AndroidUtils.createSnackbar(mViewDataBinding.rootContainerDetails, true);
                    snackbar.setText(R.string.cannot_play_track);
                    snackbar.show();
                }*/
            } else {
                AndroidUtils.openInExternalApp(
                    mTrackDetailViewModel!!.absolutePath.getValue(),
                    this
                )
            }
            false
        }
    }

    /**
     * Alternates the play to the stop action.
     */
    private fun addStopAction() {
        mPlayPreviewMenuItem!!.setOnMenuItemClickListener(null)
        mPlayPreviewMenuItem!!.setOnMenuItemClickListener { item: MenuItem? -> false }
    }

    /**
     * This method creates the references to visual elements
     * in layout
     */
    private fun hideFabs() {
        dataBinding!!.fabIdentification.hide()
        dataBinding!!.fabSaveInfo.hide()
    }

    /**
     * Add listeners for corresponding objects to
     * respond to user interactions
     */
    private fun addFloatingActionButtonListeners() {

        dataBinding!!.fabSaveInfo.setOnClickListener { v: View? ->
            val manualCorrectionDialogFragment: ManualCorrectionDialogFragment =
                ManualCorrectionDialogFragment.newInstance(
                    mTrackDetailViewModel!!.title.value
                )
            manualCorrectionDialogFragment.show(
                supportFragmentManager,
                manualCorrectionDialogFragment.javaClass.getCanonicalName()
            )
        }
    }

    /**
     * Adds a effect to fading down the cover when user scroll up and fading up to the cover when user
     * scrolls down.
     */
    private fun addAppBarOffsetListener() {
        mOffsetChangeListener = AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                if (verticalOffset < 0) {
                    if (!dataBinding!!.fabIdentification.isExtended) {
                        dataBinding!!.fabIdentification.extend()
                        dataBinding!!.fabSaveInfo.extend()
                    }
                } else {
                    if (dataBinding!!.fabIdentification.isExtended) {
                        dataBinding!!.fabIdentification.shrink()
                        dataBinding!!.fabSaveInfo.shrink()
                    }
                }

                //set alpha of cover depending on offset of expanded toolbar cover height,
                dataBinding.cardContainerCover.alpha =
                    1.0f - abs(verticalOffset / appBarLayout.totalScrollRange)
            }
        dataBinding.appBarLayout.addOnOffsetChangedListener(mOffsetChangeListener)
    }

    private fun onWritingResult(voids: Void) {
        enableEditModeElements()
        showFabs()
        enableAppBarLayout()
    }

    /**
     * Disable the Save Fab button.
     */
    private fun showFabs() {
        dataBinding!!.fabSaveInfo.hide()
        dataBinding!!.fabIdentification.show()
    }

    /**
     * Enable the Save Fab button.
     */
    private fun editMode() {
        disableAppBarLayout()
        disableEditModeElements()
        dataBinding!!.fabIdentification.hide()
        dataBinding!!.fabSaveInfo.show()
        mTrackDetailFragment!!.enableFieldsToEdit()
    }

    private fun disableEditModeElements() {
        mManualEditMenuItem!!.isEnabled = false
        dataBinding!!.coverArtMenu.isEnabled = false
        dataBinding!!.coverArtMenu.visibility = View.GONE
        dataBinding!!.fabIdentification.hide()
    }

    private fun enableEditModeElements() {
        mManualEditMenuItem!!.isEnabled = true
        dataBinding!!.coverArtMenu.isEnabled = true
        dataBinding!!.coverArtMenu.visibility = View.VISIBLE
        dataBinding!!.fabIdentification.show()
    }

    /**
     * Enters edit mode, for modify manually
     * the information about the song
     */
    private fun disableAppBarLayout() {
        dataBinding!!.appBarLayout.setExpanded(false)
    }

    /**
     * Exits edit mode, for modify manually
     * the information about the song
     */
    private fun enableAppBarLayout() {
        //shrink toolbar to make it easy to user
        //focus in editing tags
        dataBinding!!.appBarLayout.setExpanded(true)
    }

    /**
     * Starts a external app to search info about the current track.
     */
    private fun searchInfoForTrack() {
        mTrackDetailFragment!!.searchInfoForTrack()
    }

    /**
     * Callback when data from track is completely
     * loaded.
     * @param voids null object.
     */
    private fun onSuccessLoad(voids: Unit) {
        Log.e(javaClass.name, "onSuccessLoad")
        //mPlayer.setPath(mTrackDetailViewModel.getCurrentTrack().getPath());
        addFloatingActionButtonListeners()
        addAppBarOffsetListener()
        addToolbarButtonsListeners()
        addListenerCoverMenu()
        showFabs()
        dataBinding!!.fabSaveInfo.shrink()
        dataBinding!!.fabIdentification.shrink()
        if (mEditMode) editMode()
    }

    /**
     * Add the observers for identification events.
     */
    private fun setupIdentificationObserves() {
    }

    /**
     * Set the listeners to toolbar buttons.
     */
    private fun addToolbarButtonsListeners() {
        addPlayAction()

        //performs a web trackSearch in navigator
        //using the title and artist name
        mSearchInWebMenuItem!!.setOnMenuItemClickListener { item: MenuItem? ->
            searchInfoForTrack()
            false
        }
        mManualEditMenuItem!!.setOnMenuItemClickListener { item: MenuItem? ->
            editMode()
            false
        }
        mTrackDetailsMenuItem!!.setOnMenuItemClickListener { item: MenuItem? ->
            var metadataDetailsFragment: MetadataDetailsFragment? =
                supportFragmentManager.findFragmentByTag(
                    MetadataDetailsFragment::class.java.getName()
                ) as MetadataDetailsFragment?
            if (metadataDetailsFragment == null) metadataDetailsFragment =
                MetadataDetailsFragment.newInstance()
            metadataDetailsFragment.show(
                supportFragmentManager,
                metadataDetailsFragment.javaClass.getName()
            )
            false
        }
        mRenameTrackItem!!.setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener {
            val changeFilenameDialogFragment = ChangeFilenameDialogFragment.newInstance(
                mTrackDetailViewModel!!.currentTrack.mediaStoreId.toString() + ""
            )
            changeFilenameDialogFragment.setOnChangeNameListener(object :
                ChangeFilenameDialogFragment.OnChangeNameListener {
                override fun onAcceptNewName(inputParams: CorrectionParams?) {
                    mTrackDetailViewModel!!.renameFile(inputParams!!)
                }

                override fun onCancelRename() {
                    //changeFilenameDialogFragment.dismiss()
                }
            })
            /*changeFilenameDialogFragment.show(
                getSupportFragmentManager(),
                changeFilenameDialogFragment.javaClass.name
            )*/
            false
        })
    }

    /**
     * Set the listener to create the pop up cover art menu, and respond to
     * these actions.
     */
    private fun addListenerCoverMenu() {
        dataBinding!!.coverArtMenu.setOnClickListener { v: View? ->
            val popupMenu: PopupMenu = PopupMenu(this@TrackDetailActivity, v)
            val menuInflater: MenuInflater = popupMenu.getMenuInflater()
            menuInflater.inflate(R.menu.menu_cover_art_options, popupMenu.getMenu())
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem ->
                when (item.getItemId()) {
                    R.id.action_update_cover -> editCover(TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY)
                    R.id.action_extract_cover -> mTrackDetailViewModel!!.extractCover()
                    R.id.action_remove_cover -> mTrackDetailViewModel!!.removeCover()
                }
                false
            })
        }
    }

    private fun onLoadingMessage(s: String) {
        val snackbar: Snackbar = AndroidUtils.createSnackbar(
            dataBinding!!.rootContainerDetails,
            true
        )
        snackbar.setText(s)
        snackbar.setDuration(Snackbar.LENGTH_SHORT)
        snackbar.show()
    }

    private fun onLoadingMessage(message: Int) {
        onLoadingMessage(getString(message))
    }

    /**
     * Callback to confirm the deletion of current cover;
     */
    private fun onConfirmRemovingCover(voids: Void) {
        val informativeFragmentDialog: InformativeFragmentDialog =
            InformativeFragmentDialog.newInstance(
                R.string.attention,
                R.string.message_remove_cover_art_dialog,
                R.string.accept, R.string.cancel_button, this
            )
        informativeFragmentDialog.showNow(
            supportFragmentManager,
            informativeFragmentDialog.javaClass.canonicalName
        )
        informativeFragmentDialog.setOnClickBasicFragmentDialogListener(
            object : InformativeFragmentDialog.OnClickBasicFragmentDialogListener {
                override fun onPositiveButton() {
                    informativeFragmentDialog.dismiss()
                    mTrackDetailViewModel!!.confirmRemoveCover()
                }

                override fun onNegativeButton() {
                    informativeFragmentDialog.dismiss()
                }
            }
        )
    }

    private fun showInformativeMessage(message: String) {
        val snackbar: Snackbar =
            AndroidUtils.createSnackbar(dataBinding!!.rootContainerDetails, true)
        snackbar.setText(message)
        snackbar.setDuration(Snackbar.LENGTH_SHORT)
        snackbar.show()
    }

    private fun showSnackbarMessage(snackbarMessage: SnackbarMessage<*>) {
        val snackbar: Snackbar = AndroidUtils.createSnackbar(
            dataBinding!!.rootContainerDetails,
            snackbarMessage.isDismissible
        )
        snackbar.setText(snackbarMessage.body!!)
        if (snackbarMessage.mainAction != Action.NONE) {
            snackbar.setAction(
                snackbarMessage.mainActionText,
                createOnClickListener(snackbarMessage)
            )
        }
        snackbar.duration = snackbarMessage.duration
        snackbar.show()
    }

    /**
     * Creates a OnClickListener object to respond according to an Action object.
     * @param snackbarMessage The message suitable to be shown by snackbar.
     * @return A OnclickListener object.
     */
    private fun createOnClickListener(snackbarMessage: SnackbarMessage<*>): View.OnClickListener? {
        when (snackbarMessage.mainAction) {
            Action.URI_ERROR -> return View.OnClickListener { view: View? ->
                startActivity(
                    Intent(
                        this,
                        SdCardInstructionsActivity::class.java
                    )
                )
            }
            Action.MANUAL_CORRECTION -> return View.OnClickListener { view: View? -> editMode() }
            Action.WATCH_IMAGE -> return View.OnClickListener { view: View ->
                AndroidUtils.openInExternalApp(
                    snackbarMessage.data.toString(),
                    view.context
                )
            }
        }
        return null
    }

    private fun loading(showProgress: Boolean) {
        if (showProgress) {
            dataBinding!!.progressView.containerProgress.visibility = View.VISIBLE
            disableEditModeElements()
        } else {
            dataBinding!!.progressView.containerProgress.visibility = View.GONE
            enableEditModeElements()
        }
    }

    companion object {
        const val INTENT_OPEN_GALLERY = 1
        const val INTENT_GET_AND_UPDATE_FROM_GALLERY = 2
        const val TRACK_DATA = BuildConfig.APPLICATION_ID + ".track_data"
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}