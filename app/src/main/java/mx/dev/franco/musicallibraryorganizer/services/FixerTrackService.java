package mx.dev.franco.musicallibraryorganizer.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnAudioFile;
import com.gracenote.gnsdk.GnContent;
import com.gracenote.gnsdk.GnDataLevel;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileCallbackStatus;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnResponseDataMatches;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnMusicIdFileEvents;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import mx.dev.franco.musicallibraryorganizer.AudioItem;
import mx.dev.franco.musicallibraryorganizer.DataTrackDbHelper;
import mx.dev.franco.musicallibraryorganizer.R;
import mx.dev.franco.musicallibraryorganizer.SelectFolderActivity;
import mx.dev.franco.musicallibraryorganizer.SelectedOptions;
import mx.dev.franco.musicallibraryorganizer.TrackContract;

import static mx.dev.franco.musicallibraryorganizer.services.GnService.appString;

/**
 * Created by franco on 17/08/17.
 */

public class FixerTrackService extends IntentService {
    private static String TAG = FixerTrackService.class.getName();
    //response actions in order to receivers can distinguish the response and handle correctly
    public static final String ACTION_DONE = "action_done";
    public static final String ACTION_CANCEL = "action_cancel";
    public static final String ACTION_FAIL = "action_fail";
    public static final String ACTION_COMPLETE_TASK = "action_complete_task";
    //global reference to the object being processed
    private volatile AudioItem currentAudioItem = null;
    //Necessary objects to interact with Gracenote API
    private GnMusicIdFileInfoManager gnMusicIdFileInfoManager = null;
    private GnMusicIdFileInfo gnMusicIdFileInfo = null;
    private GnMusicIdFile gnMusicIdFile = null;
    private MusicIdFileEvents musicIdFileEvents = null;
    //Reference to the intent sent from activities
    private Intent intent;
    //Connection to database
    private DataTrackDbHelper dataTrackDbHelper;
    //Reference o broadcast manager that sends the intents to receivers
    private LocalBroadcastManager localBroadcastManager;
    //Intent that contaitn the response sent from broadcast manager
    private Intent broadcastResponseIntent;
    //A list to maintain references to actual objects being processed in case
    //it needs to cancel the task
    public static List<GnMusicIdFile> gnMusicIdFileList = new ArrayList<>();

    //set this flag to TRUE if we touch an element from list, means that only one track is queued to correct
    private boolean singleTrack = false;
    //Initial value, if exist audio item, this will override this initial value
    private long currentId = - 1;
    //set this flag to TRUE if we send this intent from DetailsTrackDialogActitivy class
    private boolean fromDetailsTrackDialog = false;
    private boolean downloadCover = false;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FixerTrackService() {
        super("FixerTRackService");
        Log.d(TAG, "constructor");
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d(TAG, "onCreate");
        dataTrackDbHelper = DataTrackDbHelper.getInstance(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        //Object to handle callbacks from Gracenote API
        musicIdFileEvents = new MusicIdFileEvents(this);

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        assert intent != null;
        this.intent = intent;
        singleTrack = this.intent.getBooleanExtra("singleTrack",false);
        fromDetailsTrackDialog = this.intent.getBooleanExtra("fromDetailsTrackDialog",false);
        downloadCover = this.intent.getBooleanExtra("downloadCover",false);
        broadcastResponseIntent = new Intent();

            if (singleTrack) {
                //only click on one item or autocorrect inside activity details track
                doTrackIdSingleTrack();
            } else {
                //multiple tracks selected
                doTrackIdMultipleTracks();
            }


        //When all process has finished, inform to receivers
        broadcastResponseIntent.setAction(ACTION_COMPLETE_TASK);
        boolean sent = localBroadcastManager.sendBroadcast(broadcastResponseIntent);
        SelectFolderActivity.shouldContinue = true;
        Log.d("broadcast sent", sent + "");
    }

    private synchronized void doTrackIdSingleTrack(){
        currentId = intent.getLongExtra("id", -1);
        currentAudioItem = SelectFolderActivity.getItemByIdOrPath(currentId,null);
        String fileName = currentAudioItem.getNewAbsolutePath();
        File source = new File(fileName);

        try {

            //Create the object that enables us do "trackid"
            gnMusicIdFile = new GnMusicIdFile(GnService.gnUser, musicIdFileEvents);
            //set option to get type of response
            gnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            //we add the reference to this object in case we need to cancel
            gnMusicIdFileList.add(gnMusicIdFile);
            //get the FileInfoManager
            gnMusicIdFileInfoManager = gnMusicIdFile.fileInfos();

            //add new resource and all possible metadata to increase the accuracy of identify
            gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(source.getAbsolutePath());
            gnMusicIdFileInfo.albumArtist(currentAudioItem.getArtist());
            gnMusicIdFileInfo.trackTitle(currentAudioItem.getTitle());
            gnMusicIdFileInfo.fileName(source.getAbsolutePath());

            //verify if task was not cancelled
            if(!SelectFolderActivity.shouldContinue) {
                gnMusicIdFile.cancel();
                broadcastResponseIntent.setAction(ACTION_CANCEL);
                broadcastResponseIntent.putExtra("id",currentId);
                localBroadcastManager.sendBroadcast(broadcastResponseIntent);
                SelectFolderActivity.shouldContinue = true;
            }

            //do the recognition, kQueryReturnSingle returns only the most accurate result!!!
            gnMusicIdFile.doTrackId(GnMusicIdFileProcessType.kQueryReturnSingle, GnMusicIdFileResponseType.kResponseAlbums);

        } catch (GnException e) {
            e.printStackTrace();
        }


    }

    private synchronized void doTrackIdMultipleTracks(){
        for (int j = 0; j < SelectFolderActivity.audioItemArrayAdapter.getItemCount(); j++) {
            currentAudioItem = SelectFolderActivity.audioItemList.get(j);
            currentId = currentAudioItem.getId();

            //Is not selected to fix, then jump it
            if (!currentAudioItem.isChecked()) {
                currentAudioItem = null;
                currentId = -1;
                continue;
            }

            if(!SelectFolderActivity.shouldContinue){
                broadcastResponseIntent.setAction(ACTION_CANCEL);
                broadcastResponseIntent.putExtra("id",currentId);
                localBroadcastManager.sendBroadcast(broadcastResponseIntent);
                SelectFolderActivity.shouldContinue = true;
                break;
            }

            currentAudioItem.setProcessing(true);

            synchronized (this){
                SelectFolderActivity.audioItemArrayAdapter.notifyItemChanged(j);
            }



            String fileName = currentAudioItem.getNewAbsolutePath();
            File source = new File(fileName);

            try {

                gnMusicIdFile = new GnMusicIdFile(GnService.gnUser, musicIdFileEvents);
                gnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
                gnMusicIdFileList.add(gnMusicIdFile);
                gnMusicIdFileInfoManager = gnMusicIdFile.fileInfos();
                gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(source.getAbsolutePath());
                gnMusicIdFileInfo.albumArtist(currentAudioItem.getArtist());
                gnMusicIdFileInfo.trackTitle(currentAudioItem.getTitle());
                gnMusicIdFileInfo.fileName(source.getAbsolutePath());




                gnMusicIdFile.doTrackId(GnMusicIdFileProcessType.kQueryReturnSingle, GnMusicIdFileResponseType.kResponseAlbums);
                //gnMusicIdFile.doLibraryId(GnMusicIdFileResponseType.kResponseAlbums);

            } catch (GnException e) {
                e.printStackTrace();
            }

        }

    }

    public static void cancelGnMusicIdFileProcessing(){
        Iterator<GnMusicIdFile> iterator = gnMusicIdFileList.iterator();

        while (iterator.hasNext()){
            iterator.next().cancel();
        }
    }

    /**
     * Instances of this class
     * handles the response of doTrackId method
     */


    private class MusicIdFileEvents implements IGnMusicIdFileEvents {
        private HashMap<String,String> gnStatusToDisplay;
        private String newName = "";
        private String artistName = "";
        private String albumName = "";
        private String trackNumber = "";
        private String year = "";
        private String genre = "";
        private String imageUrl = "";
        private ContentValues contentValues = new ContentValues();
        private Context context;
        private MusicMetadataSet musicMetadataSet;
        private MyID3 myID3;
        private MusicMetadata iMusicMetadata;
        private File currentFile = null;
        private File renamedFile = null;

        public MusicIdFileEvents(Context context){
            this.context =  context.getApplicationContext();
            //Put the status of downloaded info
            gnStatusToDisplay = new HashMap<>();
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingBegin",context.getResources().getString(R.string.begin_processing));
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusFileInfoQuery",context.getResources().getString(R.string.querying_info_));
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingComplete",context.getResources().getString(R.string.complete_identification));
        }

        @Override
        public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long l, long l1, IGnCancellable iGnCancellable) {
                try {
                    String status = gnMusicIdFileCallbackStatus.toString();
                    if (gnStatusToDisplay.containsKey(status)) {
                        String filename = gnMusicIdFileInfo.identifier();
                        if (filename != null) {
                            status = gnStatusToDisplay.get(status) + ": " + filename;

                        }
                        Log.d("FILE STATUS EVENT", status);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(appString, "error in retrieving musicIdFileStatus");
                }

        }

        @Override
        public void gatherFingerprint(GnMusicIdFileInfo fileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            Log.d("gatherFingerprint", "gatherFingerprint");

            //Callback to inform that fingerprint was retrieved from audiotgrack.
            //I tried to generate manually the fingerprint for improve results,
            //but I had no success, it returns worse results
            //then I better used fingerprintFromSource() method

                try {

                    if (GnAudioFile.isFileFormatSupported(fileInfo.fileName())) {
                        fileInfo.fingerprintFromSource(new GnAudioFile(new File(fileInfo.fileName())));
                    }

                }
                catch (GnException e) {
                    if (!GnError.isErrorEqual(e.errorCode(), GnError.GNSDKERR_Aborted)) {
                        Log.e(appString, "error in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());
                    }
                }

        }
        //This method is not need because we provide some metadata before
        @Override
        public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            Log.d("gatherMetadata", "gatherMetadata");
        }

        @Override
        public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {
            try {
//                GnAlbum gnAlbum = gnResponseAlbums.albums().at(0).next();
//                GnTrackIterable gnTrackIterable = gnAlbum.tracks();
//                GnTrackIterator gnTrackIterator = gnTrackIterable.getIterator();
//                while(gnTrackIterator.hasNext()){
//                    Log.d("track_matched",gnTrackIterator.next().title().display());
//                }
                Log.d("AlbumResult_number", gnResponseAlbums.albums().count()+"");
            } catch (GnException e) {
                e.printStackTrace();
            }


            try {
                newName = gnResponseAlbums.albums().at(0).next().trackMatched().title().display();
            } catch (GnException e) {
                e.printStackTrace();
                newName = "";
            }

            try {
                artistName = gnResponseAlbums.albums().at(0).next().artist().name().display();
            } catch (GnException e) {
                e.printStackTrace();
                artistName = "";
            }
            try {
                albumName = gnResponseAlbums.albums().at(0).next().title().display();
            } catch (GnException e) {
                e.printStackTrace();
                albumName = "";
            }

            try {
                //If selected "No descargar imagen"
                if (SelectedOptions.ALBUM_ART_SIZE == GnImageSize.kImageSizeUnknown) {
                    imageUrl = "";
                }
                //If selected "Cualquier tamaño disponible"
                else if (SelectedOptions.ALBUM_ART_SIZE == null) {
                    GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                    GnImageSize[] values = GnImageSize.values();
                    for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                        String url = gnContent.asset(values[sizes]).url();
                        if (!gnContent.asset(values[sizes]).url().equals("")) {
                            imageUrl = url;
                            break;
                        }
                    }
                }
                //get the selected size
                else {
                    imageUrl = gnResponseAlbums.albums().at(0).next().coverArt().asset(SelectedOptions.ALBUM_ART_SIZE).url();
                }

            } catch (GnException e) {
                e.printStackTrace();
                imageUrl = "";
            }

            try {
                trackNumber = gnResponseAlbums.albums().at(0).next().trackMatchNumber() + "";
            } catch (GnException e) {
                e.printStackTrace();
                trackNumber = "";
            }
            try {
                year = gnResponseAlbums.albums().at(0).next().year();
            } catch (GnException e) {
                e.printStackTrace();
                year = "";
            }
            try {
                genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_3);
            } catch (GnException e) {
                e.printStackTrace();
                genre = "";
            }


            //if intent to FixerTrackService was made from list
            if(!fromDetailsTrackDialog) {


                //lets try to open the file to edit metatags.
                myID3 = new MyID3();
                try {
                    musicMetadataSet = myID3.read(new File(currentAudioItem.getNewAbsolutePath()));
                } catch (IOException e) {
                    e.printStackTrace();
                }


                //For some reason it could not be opened the mp3 file =(
                if (musicMetadataSet == null) {
                    currentAudioItem.setStatus(AudioItem.FILE_STATUS_BAD);
                    currentAudioItem.setChecked(false);
                    contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_BAD);
                    dataTrackDbHelper.setStatus(currentAudioItem.getId(), contentValues);

                }
                //yeah!!! now lets fix this song!!!
                else {
                    iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();


                    //FIRST WE FIX THE TITLE

                    //was found song name? lets change the title and filename! =)
                    boolean dataTitle = !newName.equals("");

                    if (dataTitle) {
                        currentFile = new File(currentAudioItem.getNewAbsolutePath());

                        //yeah,lets change filename if this setting on settings activity was activated
                        if (SelectedOptions.AUTOMATIC_CHANGE_FILENAME) {
                            String newPath = currentFile.getParent();
                            String newFilename = newName + ".mp3";
                            String newCompleteFilename = newPath + "/" + newFilename;
                            renamedFile = new File(newCompleteFilename);
                            if (!renamedFile.exists()) {
                                Log.d("No Existe", "file");
                                currentFile.renameTo(renamedFile);
                            } else {
                                newFilename = newName + "(" + (int) Math.floor((Math.random() * 10) + 1) + ")" + ".mp3";
                                newCompleteFilename = newPath + "/" + newFilename;
                                renamedFile = new File(newCompleteFilename);
                                Log.d("Ya Existe", "file");
                                currentFile.renameTo(renamedFile);
                            }

                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME, newFilename);
                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH, newCompleteFilename);
                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_PATH, newPath);
                            currentAudioItem.setFileName(newFilename);
                            currentAudioItem.setNewAbsolutePath(newCompleteFilename);
                            currentAudioItem.setPath(newPath);
                            Log.d("NEW_PATH", newCompleteFilename);
                        }
                        //Not change automatically file name? No problem
                        else {
                            renamedFile = currentFile;
                        }

                        //update our database
                        contentValues.put(TrackContract.TrackData.COLUMN_NAME_TITLE, newName);

                        //lets inform to system that one file has change
                        MediaScannerConnection.scanFile(context.getApplicationContext(),
                                new String[]{currentFile.getAbsolutePath(), renamedFile.getAbsolutePath()},
                                null,
                                null);

                        //then update the itemlist name
                        currentAudioItem.setTitle(newName);
                        iMusicMetadata.setSongTitle(newName);
                    }
                    //it was not found song name
                    else {
                        renamedFile = new File(currentAudioItem.getNewAbsolutePath());
                    }


                    //NOW FIX ARTIST

                    //was found artist? lets fix it!!!
                    boolean dataArtist = !artistName.equals("");
                    if (dataArtist) {
                        iMusicMetadata.setArtist(artistName);
                        currentAudioItem.setArtist(artistName);
                        contentValues.put(TrackContract.TrackData.COLUMN_NAME_ARTIST, artistName);
                    }


                    //NOW FIX ALBUM

                    //was found album? lets go fix it!
                    boolean dataAlbum = !albumName.equals("");
                    if (dataAlbum) {
                        iMusicMetadata.setAlbum(albumName);
                        currentAudioItem.setAlbum(albumName);
                        contentValues.put(TrackContract.TrackData.COLUMN_NAME_ALBUM, albumName);
                    }

                    //FIX MY FAVORITE PART, THE COVER ART!!!, OOPSS, I MADE A RHYME UNINTENTIONALLY =D

                    boolean dataImage = !imageUrl.equals("");
                    if (dataImage) {
                        try {
                            byte[] imgData = new GnAssetFetch(GnService.gnUser, imageUrl).data();
                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_COVER_ART, imgData);
                            currentAudioItem.setCoverArt(imgData);
                            Vector<ImageData> imageDataVector = new Vector<>();
                            ImageData imageData = new ImageData(imgData, "", "", 3);
                            imageDataVector.add(imageData);
                            iMusicMetadata.setPictureList(imageDataVector);

                            //update filesize because the new embed cover
                            float fileSizeInMb = (float) renamedFile.length() / AudioItem.KILOBYTE;
                            currentAudioItem.setSize(fileSizeInMb);
                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_FILE_SIZE, fileSizeInMb);
                        } catch (GnException e) {
                            e.printStackTrace();
                        }

                    }

                    //FIX TRACK NUMBER
                    //was found track number
                    boolean dataTrackNumber = !trackNumber.equals("");
                    if (dataTrackNumber) {
                        iMusicMetadata.setTrackNumber(Integer.parseInt(trackNumber));
                    }

                    //FIX TRACK YEAR
                    //was found track year
                    boolean dataYear = !year.equals("");
                    if (dataYear) {
                        iMusicMetadata.setYear(year);
                    }

                    //FIX GENRE
                    //was found genre
                    boolean dataGenre = !genre.equals("");
                    if (dataGenre) {
                        iMusicMetadata.setGenre(genre);
                    }

                    //NOW LETS CHECK IF ALL VALUES WERE FOUND TO SET A STATUS TO AUDIO ITEM
                    //missing some info, mark as incomplete this song
                    if (!dataTitle || !dataArtist || !dataAlbum || !dataImage || !dataTrackNumber || !dataYear || !dataGenre) {
                        contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_INCOMPLETE);
                        currentAudioItem.setStatus(AudioItem.FILE_STATUS_INCOMPLETE);
                    }
                    //Wiii!!! all info for this song was found!!!
                    else {
                        contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_OK);
                        currentAudioItem.setStatus(AudioItem.FILE_STATUS_OK);
                    }


                    //almost finally update the mp3 file meta tags!!!
                    try {
                        myID3.update(renamedFile, musicMetadataSet, iMusicMetadata);
                    } catch (IOException | ID3WriteException e) {
                        e.printStackTrace();
                    }
                    //and now almost almost finally update our database
                    contentValues.put(TrackContract.TrackData.COLUMN_NAME_IS_SELECTED, false);
                    dataTrackDbHelper.updateData(currentAudioItem.getId(), contentValues);


                }

            }
            //if intent was made from DetailsTrackDialogActivity
            //don't modify the track, only retrieve data and
            //send back to activity, the user has to decide
            //if the information is correct or not, besides,
            //for UX, is not convenient to interfere with what the user
            //is doing
            else {
                    AudioItem tempAudioItem = new AudioItem();

                    //was found song name? lets change the title and filename! =)
                    boolean dataTitle = !newName.equals("");

                    if (dataTitle) {
                        tempAudioItem.setTitle(newName);
                    }
                    //NOW FIX ARTIST

                    //was found artist? lets fix it!!!
                    boolean dataArtist = !artistName.equals("");
                    if (dataArtist) {
                        tempAudioItem.setArtist(artistName);
                    }
                    //NOW FIX ALBUM

                    //was found album? lets go fix it!
                    boolean dataAlbum = !albumName.equals("");
                    if (dataAlbum) {
                        tempAudioItem.setAlbum(albumName);
                    }

                    //FIX MY FAVORITE PART, THE COVER ART!!!, OOPSS, I MADE A RHYME UNINTENTIONALLY =D

                    boolean dataImage = !imageUrl.equals("");
                    if (dataImage) {
                        try {
                            byte[] imgData = new GnAssetFetch(GnService.gnUser, imageUrl).data();
                            tempAudioItem.setCoverArt(imgData);
                        } catch (GnException e) {
                            e.printStackTrace();
                        }

                    }




                    //FIX TRACK NUMBER
                    //was found track number
                    boolean dataTrackNumber = !trackNumber.equals("");
                    if (dataTrackNumber) {
                        tempAudioItem.setTrackNumber(trackNumber);
                    }

                    //FIX TRACK YEAR
                    //was found track year
                    boolean dataYear = !year.equals("");
                    if (dataYear) {
                        tempAudioItem.setTrackYear(year);
                    }

                    //FIX GENRE
                    //was found genre
                    boolean dataGenre = !genre.equals("");
                    if (dataGenre) {
                        tempAudioItem.setGenre(genre);
                    }

                    //put the info retrieved into intent
                    broadcastResponseIntent.putExtra("audioItem",tempAudioItem);

            }


            //report to receivers that task were done
            broadcastResponseIntent.setAction(ACTION_DONE);
            Log.d("action_Sent",broadcastResponseIntent.getAction());
            broadcastResponseIntent.putExtra("id", currentId);
            broadcastResponseIntent.putExtra("singleTrack",singleTrack);
            if(singleTrack)
                broadcastResponseIntent.putExtra("status",currentAudioItem.getStatus());

            boolean sent = localBroadcastManager.sendBroadcast(broadcastResponseIntent);
            Log.d("sent_match",sent+"");


            //now yes finally, set current audio item as deselected
            contentValues.clear();

            if(gnMusicIdFile != null)
                gnMusicIdFileList.remove(gnMusicIdFile);

            Log.d("TITLE_BG", newName);
            Log.d("ARTIST_BG", artistName);
            Log.d("ALBUM_BG",albumName);
            Log.d("ALBUM_ART_BG", imageUrl);
            Log.d("NUMBER_BG", trackNumber);
            Log.d("YEAR_BG", year);
            Log.d("GENRE_BG", genre);
            clearValues();

        }

        @Override
        public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) {
            //Not used because we selected GnMusicIdFileResponseType.kResponseAlbums as response type
            Log.d("musicIdFileMatchResult", "musicIdFileMatchResult");
        }

        @Override
        public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            try {
                Log.d("ResultNotFound",gnMusicIdFileInfo.status().name());
            } catch (GnException e) {
                e.printStackTrace();
            }
            finally {
                currentAudioItem.setStatus(AudioItem.FILE_STATUS_BAD);
                contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_BAD);
                contentValues.put(TrackContract.TrackData.COLUMN_NAME_IS_SELECTED, false);
                dataTrackDbHelper.updateData(currentId, contentValues);

                broadcastResponseIntent.setAction(ACTION_FAIL);
                broadcastResponseIntent.putExtra("id", currentId);
                boolean sentNoresult = localBroadcastManager.sendBroadcast(broadcastResponseIntent);
                Log.d("sent_no_result",sentNoresult+"");


                if(gnMusicIdFile != null)
                    gnMusicIdFileList.remove(gnMusicIdFile);

                contentValues.clear();
            }
        }

        @Override
        public void musicIdFileComplete(GnError gnError) {
            Log.d("musicIdFileComplete",gnError.errorDescription());
            //currentAudioItem.setStatus(AudioItem.FILE_STATUS_BAD);
            //contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_BAD);
            //dataTrackDbHelper.setStatus(currentAudioItem.getId(), contentValues);
            //if(!singleTrack)
            //    currentAudioItem.setChecked(false);

            //synchronized (this){
            //    SelectFolderActivity.audioItemArrayAdapter.notifyItemChanged(currentAudioItem.getPosition());
            //}

            //broadcastResponseIntent.setAction(ACTION_FAIL);
            //broadcastResponseIntent.putExtra("id", currentId);
            //localBroadcastManager.sendBroadcast(broadcastResponseIntent);
            //clearValues();

        }

        @Override
        public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {
            Log.d("gnStatus",gnStatus.name());
        }

        private void clearValues(){
            newName = "";
            artistName = "";
            albumName = "";
            trackNumber = "";
            year = "";
            genre = "";
            imageUrl = "";
            contentValues.clear();
            musicMetadataSet = null;
            myID3 = null;
            currentId = -1;
            iMusicMetadata = null;
            currentFile = null;
            renamedFile = null;
            currentAudioItem = null;
        }
    }
}


