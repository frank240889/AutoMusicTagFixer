package mx.dev.franco.musicallibraryorganizer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Created by franco on 2/04/17.
 */

public class DetailsTrackDialog extends DialogFragment{
    static String FRAGMENT_NAME = DetailsTrackDialog.class.getName();
    private int p;
    private boolean editMode = false;
    private ArrayAdapter<AudioItem> filesAdapter;
    private FFmpegMediaMetadataRetriever mediaMetadataRetriever2 = null;
    private MediaMetadataRetriever mediaMetadataRetriever = null;
    private DataTrackDbHelper dbHelper;
    private String trackPath;
    private EditText nameField;
    private String oldNameField;
    private EditText artistField;
    private String oldArtistField;
    private EditText albumField;
    private String oldAlbumField;
    private EditText numberField;
    private String oldNumberField;
    private EditText yearField;
    private String oldYearField;
    private EditText genreField;
    private String oldGenreField;
    private ImageButton imageButtonField;
    private Bitmap oldImageBitmap;
    private ContentValues newData;
    private View viewDetailsTrack;
    private Button editOrSaveButton;
    private Button cancelButton;
    static byte[] newAlbumArt = null;
    static int INTENT_OPEN_GALLERY = 1;

    public DetailsTrackDialog(){
    }

    public void setData(String path, ArrayAdapter<AudioItem> filesAdapter, int p, DataTrackDbHelper dbHelper){
        this.trackPath = path;
        this.filesAdapter = filesAdapter;
        this.p = p;
        this.dbHelper = dbHelper;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        viewDetailsTrack = inflater.inflate(R.layout.details_track_layout,null,false);
        setupFields();
        builder.setView(viewDetailsTrack);
        return builder.create();
    }

    @Override
    public void onStart(){
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface){
        if(mediaMetadataRetriever2 != null){
            mediaMetadataRetriever2.release();
            mediaMetadataRetriever2 = null;
        }

        if(mediaMetadataRetriever != null){
            mediaMetadataRetriever.release();
            mediaMetadataRetriever = null;
        }
    }

    private void setupFields(){
        mediaMetadataRetriever2 = new FFmpegMediaMetadataRetriever();
        mediaMetadataRetriever2.setDataSource(trackPath);
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(trackPath);
        if(mediaMetadataRetriever.getEmbeddedPicture() != null) {
            ((ImageButton) viewDetailsTrack.findViewById(R.id.albumArt)).setImageBitmap(BitmapFactory.decodeByteArray(mediaMetadataRetriever.getEmbeddedPicture(), 0, mediaMetadataRetriever.getEmbeddedPicture().length));
        }

        oldImageBitmap = ((BitmapDrawable)((ImageButton)viewDetailsTrack.findViewById(R.id.albumArt)).getDrawable()).getBitmap();

        nameField = (EditText)viewDetailsTrack.findViewById(R.id.track_name_details);
        oldNameField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE):"";
        nameField.setText(oldNameField);

        artistField = (EditText)viewDetailsTrack.findViewById(R.id.artist_name_details);
        oldArtistField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST):"";
        artistField.setText(oldArtistField);

        albumField = (EditText)viewDetailsTrack.findViewById(R.id.album_name_details);
        oldAlbumField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM):"";
        albumField.setText(oldAlbumField);

        numberField = (EditText)viewDetailsTrack.findViewById(R.id.track_number);
        oldNumberField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK):"";
        numberField.setText(oldNumberField);

        yearField = (EditText)viewDetailsTrack.findViewById(R.id.track_year);
        oldYearField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE):"";
        yearField.setText(oldYearField);

        genreField = (EditText)viewDetailsTrack.findViewById(R.id.track_genre);
        oldGenreField = mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE):"";
        genreField.setText(oldGenreField);

        imageButtonField = (ImageButton) viewDetailsTrack.findViewById(R.id.albumArt);

        cancelButton = (Button) viewDetailsTrack.findViewById(R.id.cancel);
        editOrSaveButton = (Button) viewDetailsTrack.findViewById(R.id.editTrackInfo);
        editOrSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editInfoTrack();
            }
        });

        MediaExtractor mex = new MediaExtractor();
        try {
            mex.setDataSource(trackPath);// the adresss location of the sound on sdcard.
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MediaFormat mf = mex.getTrackFormat(0);

        int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        ((TextView)viewDetailsTrack.findViewById(R.id.track_type)).setText(mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC) != null ? mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC):"");
        ((TextView)viewDetailsTrack.findViewById(R.id.track_duration_details)).setText(AudioItem.getHumanReadableDuration(Integer.parseInt(mediaMetadataRetriever2.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION))));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_sampling_rate)).setText(String.valueOf(sampleRate));
        //((TextView)viewDetailsTrack.findViewById(R.id.track_resolution)).setText(trackInfo.getString(trackInfo.getColumnIndexOrThrow(TrackContract.TracKData.COLUMN_NAME_RESOLUTION)));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_channels)).setText(AudioItem.getChannelsMode(channelCount+""));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_speed)).setText(AudioItem.convertSpeed(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_size)).setText(AudioItem.convertSpeed(String.valueOf(new File(trackPath).length())));
        ((TextView)viewDetailsTrack.findViewById(R.id.track_path)).setText(trackPath);
        ((TextView)viewDetailsTrack.findViewById(R.id.track_path)).setSelected(true);
        cancelButton.setEnabled(false);
    }


    private void editInfoTrack(){
        if(!editMode) {
            enableFieldsToEdit();
            editMode = true;
        }
        else {
            disableFields();
        }
    }

    private void cachingPreviousValues(){
        oldImageBitmap = ((BitmapDrawable)((ImageButton)viewDetailsTrack.findViewById(R.id.albumArt)).getDrawable()).getBitmap();
        oldNameField = nameField.getText().toString().trim();
        oldArtistField = artistField.getText().toString().trim();
        oldAlbumField = albumField.getText().toString().trim();
        oldNumberField = numberField.getText().toString().trim();
        oldYearField = yearField.getText().toString().trim();
        oldGenreField = genreField.getText().toString().trim();
    }

    private void updateData() throws IOException, ID3WriteException {
        filesAdapter.getItem(this.p).setTitle(nameField.getText().toString().trim()).setArtist(artistField.getText().toString().trim()).setAlbum(albumField.getText().toString().trim()).setStatus(AudioItem.FILE_STATUS_EDIT_BY_USER);
        new Thread(new Runnable() {
            @Override
            public void run() {

                    DetailsTrackDialog.this.dbHelper.updateData(filesAdapter.getItem(DetailsTrackDialog.this.p).getId(),newData);
                    MyID3 myID3 = new MyID3();
                    File tempFile = new File(trackPath);
                    MusicMetadataSet musicMetadataSet = null;
                    try {
                        musicMetadataSet = myID3.read(tempFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    MusicMetadata iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();
                    iMusicMetadata.setSongTitle(nameField.getText().toString().trim());
                    iMusicMetadata.setArtist(artistField.getText().toString().trim());
                    iMusicMetadata.setAlbum(albumField.getText().toString().trim());
                    iMusicMetadata.setTrackNumber(Integer.parseInt(numberField.getText().toString().trim()));
                    iMusicMetadata.setYear(yearField.getText().toString().trim());
                    iMusicMetadata.setGenre(genreField.getText().toString().trim());

                    if(newAlbumArt != null){
                        Vector<ImageData> imageDataVector = new Vector<>();
                        ImageData imageData = new ImageData(newAlbumArt, "", "", 3);
                        imageDataVector.add(imageData);
                        iMusicMetadata.setPictureList(imageDataVector);
                    }

                    try {
                        myID3.update(tempFile, musicMetadataSet, iMusicMetadata);
                    } catch (IOException | ID3WriteException e) {
                        e.printStackTrace();
                    }

                    DetailsTrackDialog.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DetailsTrackDialog.this.getDialog().findViewById(R.id.progressSavingData).setVisibility(View.INVISIBLE);
                            filesAdapter.notifyDataSetChanged();
                            Toast.makeText(getActivity().getApplicationContext(),"Información guardada correctamente",Toast.LENGTH_SHORT).show();
                        }
                    });
                newAlbumArt = null;
            }
        }).start();

    }

    private void setPreviousValues(){
        nameField.setText(oldNameField);
        artistField.setText(oldArtistField);
        albumField.setText(oldAlbumField);
        numberField.setText(oldNumberField);
        yearField.setText(oldYearField);
        genreField.setText(oldGenreField);
        imageButtonField.setImageBitmap(oldImageBitmap);
    }

    private void putValuesToRecord(){
        newData = new ContentValues();
        newData.put(TrackContract.TrackData.COLUMN_NAME_TITLE,nameField.getText().toString().trim());
        newData.put(TrackContract.TrackData.COLUMN_NAME_ARTIST,artistField.getText().toString().trim());
        newData.put(TrackContract.TrackData.COLUMN_NAME_ALBUM,albumField.getText().toString().trim());
        newData.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_EDIT_BY_USER);
    }

    private void enableFieldsToEdit(){

        nameField.setEnabled(true);
        artistField.setEnabled(true);
        albumField.setEnabled(true);
        numberField.setEnabled(true);
        yearField.setEnabled(true);
        genreField.setEnabled(true);
        imageButtonField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("CLICK","ALBUMART");
                Intent selectorImageIntent = new Intent(Intent.ACTION_PICK);
                selectorImageIntent.setType("image/*");
                getActivity().startActivityForResult(selectorImageIntent,INTENT_OPEN_GALLERY);
            }
        });
        cancelButton.setEnabled(true);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPreviousValues();
                cancelButton.setOnClickListener(null);
                disableFields();
            }
        });
        editOrSaveButton.setOnClickListener(null);
        editOrSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableFields();
                cachingPreviousValues();
                putValuesToRecord();
                try {
                    DetailsTrackDialog.this.getDialog().findViewById(R.id.progressSavingData).setVisibility(View.VISIBLE);
                    updateData();
                } catch (IOException | ID3WriteException e) {
                    e.printStackTrace();
                }


            }
        });
        editOrSaveButton.setText(getText(R.string.save_info_track));

        InputMethodManager imm =(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(nameField,InputMethodManager.SHOW_IMPLICIT);
    }

    private void disableFields(){
        nameField.setEnabled(false);
        artistField.setEnabled(false);
        albumField.setEnabled(false);
        numberField.setEnabled(false);
        yearField.setEnabled(false);
        genreField.setEnabled(false);
        imageButtonField.setOnClickListener(null);
        editOrSaveButton.setOnClickListener(null);
        editOrSaveButton.setText(getText(R.string.edit_info_track));
        editOrSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editInfoTrack();
            }
        });
        cancelButton.setEnabled(false);
        editMode = false;
    }

}
