package mx.dev.franco.musicallibraryorganizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.gracenote.gnsdk.GnAlbumIterable;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnAsset;
import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnAudioFile;
import com.gracenote.gnsdk.GnConfigOptionEnable;
import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileCallbackStatus;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnResponseDataMatches;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnMusicIdFileEvents;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static mx.dev.franco.musicallibraryorganizer.SplashActivity.sharedPreferences;

public class SelectFolderActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected static ArrayList<Integer> selectedTracks = new ArrayList<Integer>();
    protected static HashMap<Integer,String> selectedTracksList;
    protected int scanRequestType;
    protected ProgressBar progressBar;
    protected SearchView searchView;
    protected FloatingActionButton fab;
    protected static Snackbar snackbar;
    private ArrayList<CustomAudioFile> folderList = new ArrayList<CustomAudioFile>();
    private ArrayList<CustomAudioFile> files = new ArrayList<CustomAudioFile>();
    protected ArrayAdapter<File> filesAdapter;
    private ArrayList<File> arrayListFiles;
    private int requestCode;
    private TypeScanDialogFragment typeScanDialog;
    public LinearLayout view;
    private RelativeLayout loadingMetadataLayout;
    private TrackAdapter adapter;
    protected ListView listSongs;

    protected static boolean isPlaying = false;
    protected String currentTrackName = "";
    protected String lastTrackName = "";
    protected ImageButton lastButton, currentButton;
    protected static String activeTrack = "";
    protected Bitmap currentAlbumart,lastAlbumart;
    protected static MediaPlayer mediaPlayer;

    protected String m_chosenDir = "";
    protected boolean m_newFolderEnabled = true;
    protected DataTrackDbHelper dbHelper;
    protected int position;

    protected GnManager gnManager;
    protected GnUser gnUser;
    protected MusicIdFileEvents musicIdFileEvents;
    protected String gnsdkLicenseString = "-- BEGIN LICENSE v1.0 A75228BC --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 843162123\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE A75228BC --\\r\\nlAADAgAe/WEZPZ5IaetmxgKEpZm7EjG1SLm/yLvyhTwzlr8cAB4R2GcEuN/6PovFycqgCmnnmr3ioB/KXt3EDTz8yYk=\\r\\n-- END LICENSE A75228BC --\\r\\n";
    protected GnLocale gnLocale;
    static final String gnsdkClientId 			= "843162123";
    static final String gnsdkClientTag 			= "4E937B773F03BA431014169770593072";
    private static final String appString = "MusicTagFixer";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_folder);
        view = (LinearLayout) findViewById(R.id.list_of_files);
        loadingMetadataLayout = (RelativeLayout) findViewById(R.id.loadingMetadataLayout);

        selectedTracksList = new HashMap<Integer, String>();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.searching);
        progressBar.setVisibility(View.GONE);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#ff0099cc"), PorterDuff.Mode.MULTIPLY);
        searchView = (SearchView) findViewById(R.id.searchView);
        snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG).setAction("Action", null);
        snackbar.getView().setBackgroundColor(Color.parseColor("#ff0099cc"));


        //Verificamos que ya se han concedido permisos, de los contrario, aparecera el Dialogo que nos pregunta el tipo de busqueda de musica,
        //el cual nos va a pedir el permiso de acceos a los archivos
        SplashActivity.sharedPreferences = getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
        boolean grantedAccessFiles = sharedPreferences.getBoolean("accessFilesPermission", false);
        Log.d("ACCESS_FILE_PERMISSION", String.valueOf(grantedAccessFiles));
        try {
            gnManager =  new GnManager(getApplicationContext(),this.gnsdkLicenseString,GnLicenseInputMode.kLicenseInputModeString);
            gnUser = new GnUser(new GnUserStore(getApplicationContext()),gnsdkClientId,gnsdkClientTag,appString);
            musicIdFileEvents = new MusicIdFileEvents();
            gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic,GnLanguage.kLanguageInvalid,GnRegion.kRegionGlobal, GnDescriptor.kDescriptorDefault,gnUser);


        } catch (GnException e) {
            e.printStackTrace();
        }
        //Si ya teniamos concedido el permiso de acceso archivos ya no preguntamos al inicio de la app el tipo de escaneo
        if(!grantedAccessFiles) {
            typeScanDialog = new TypeScanDialogFragment();
            typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");
            typeScanDialog.setCancelable(false);
        }
        else{
            int scanType = getSharedPreferences("ShaPreferences",Context.MODE_PRIVATE).getInt("scanType",RequiredPermissions.READ_INTERNAL_STORAGE_PERMISSION);
            Log.d("SCANTYPE", String.valueOf(scanType));
            askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        this.fab = (FloatingActionButton) findViewById(R.id.fab);
        this.fab.setVisibility(View.GONE);
        this.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(ContextCompat.checkSelfPermission(SelectFolderActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(SelectFolderActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                }
                else {
                    snackbar.setText("Procesando... espere por favor.");
                    snackbar.show();
                    AsyncProcessFile asyncProcessFile = new AsyncProcessFile();
                    asyncProcessFile.execute();
                }

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().findItem(R.id.scan).setChecked(true);

        //dbHelper = new DataTrackDbHelper(getApplicationContext());


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.select_folder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        item.setChecked(true);
        int id = item.getItemId();
        System.out.println("click aqui " + id);
        if (id == R.id.donate) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.rate) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.share) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.scan) {


            if(typeScanDialog == null){
                typeScanDialog = new TypeScanDialogFragment();
                typeScanDialog.setCancelable(false);
            }
            else{
                typeScanDialog.setCancelable(true);
            }
            typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");


        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void onClickReloadList(View view){

        typeScanDialog = new TypeScanDialogFragment();
        typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");
        typeScanDialog.setCancelable(false);
        findViewById(R.id.reloadButtons).setVisibility(View.GONE);
    }


    protected void executeScan(View view) {
        if (view.getId() == R.id.autoScanRadio) {
            this.scanRequestType = 1;
        } else if (view.getId() == R.id.manualScanRadio) {
            this.scanRequestType = 2;
        }

        typeScanDialog.getDialog().cancel();
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    protected void onClickPlayImageButton(View view){

        if(isPlaying){
            currentButton = (ImageButton)view;
            mediaPlayer.stop();
            isPlaying = false;
            if(lastTrackName.equals(currentButton.getTag().toString())){
                activeTrack = "";
                currentButton.setImageBitmap(currentAlbumart);
                View currentParentView = (View)currentButton.getParent().getParent();
            }
            else{
                lastButton.setImageBitmap(lastAlbumart);
                View lastParentView = (View)lastButton.getParent().getParent();
                currentAlbumart = ((BitmapDrawable) currentButton.getDrawable()).getBitmap();
                currentButton.setImageResource(R.drawable.circled_pause);
                View currentParentView = (View)currentButton.getParent().getParent();
                lastButton = currentButton;
                currentTrackName = currentButton.getTag().toString();
                lastTrackName = currentTrackName;
                activeTrack = lastTrackName;
                mediaPlayer = MediaPlayer.create(this, Uri.parse(lastTrackName));
                mediaPlayer.start();
                isPlaying = true;
                SelectFolderActivity.snackbar.setText(getResources().getString(R.string.snackbar_message_track_preview)+ ": " + lastTrackName);
                SelectFolderActivity.snackbar.show();
            }
        }else{
            currentButton = (ImageButton)view;
            currentAlbumart = ((BitmapDrawable) currentButton.getDrawable()).getBitmap();
            lastButton = currentButton;
            lastAlbumart = currentAlbumart;
            currentTrackName = currentButton.getTag().toString();
            lastTrackName = currentTrackName;
            activeTrack = currentTrackName;
            mediaPlayer = MediaPlayer.create(this,Uri.parse(currentTrackName));
            mediaPlayer.start();
            isPlaying = true;
            currentButton.setImageResource(R.drawable.circled_pause);
            View parentView = (View)currentButton.getParent().getParent();
            SelectFolderActivity.snackbar.setText(getResources().getString(R.string.snackbar_message_track_preview)+": " + lastTrackName);
            SelectFolderActivity.snackbar.show();
        }

    }

    protected void runScantypeSelected(){
        //Si ya tenemos el permiso, verificamos que tipo de escaneo se hara, en caso que se requiera hacer un escaneo
        if(this.scanRequestType!=1){
            this.scanRequestType = 1;
        }
        if (this.scanRequestType == 1) {
            Toast.makeText(this, "Buscando música", Toast.LENGTH_LONG).show();
            //Permiso concedido, obtenemos las carpeta del sistema en busca de arhivos mp3 y el arreglo lo pasamos al adapter.
            final ListView listView = (ListView) findViewById(R.id.list_view_songs);
            filesAdapter = new TrackAdapter(this,new ArrayList<File>());
            filesAdapter.setNotifyOnChange(true);
            //filesAdapter.notifyDataSetChanged();
            listView.setAdapter(filesAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?>adapter, View view, int position, long arg){

                    ImageButton ib = (ImageButton) view.findViewById(R.id.playTrack);
                    onClickPlayImageButton(ib);

                }
            } );
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
                    String message = "";
                    if(!selectedTracksList.containsKey((Integer) position)) {
                        selectedTracksList.put((Integer)position,((CustomTrackView)view).getAbsoluteTrackPath());
                        view.setBackgroundColor(Color.parseColor("#ff0099cc"));
                        SelectFolderActivity.selectedTracks.add((Integer)position);
                        message = getResources().getString(R.string.toast_track_added_to_fix);
                    }else {
                        selectedTracksList.remove((Integer)position);
                        view.setBackgroundColor(Color.TRANSPARENT);
                        message = getResources().getString(R.string.toast_track_remove_from_fixlist);
                    }
                    Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                    if(selectedTracksList.isEmpty()){
                        message = getResources().getString(R.string.toast_empty_list);
                        SelectFolderActivity.this.fab.setEnabled(false);
                        SelectFolderActivity.this.fab.setAlpha(0.8f);
                        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                    }
                    else{
                        SelectFolderActivity.this.fab.setEnabled(true);
                        SelectFolderActivity.this.fab.setAlpha(1f);
                    }

                    return true;
                }
            });

            SearchView searchView = (SearchView) this.findViewById(R.id.searchView);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filesAdapter.getFilter().filter(newText);
                    return false;
                }
            });

            AsyncReadFile asyncReadFile = new AsyncReadFile(this.scanRequestType,new CustomAudioFile(Environment.getExternalStorageDirectory().getPath()));
            asyncReadFile.execute();
        } else {
            snackbar.setText(R.string.snackbar_message_in_development);
            snackbar.show();
        }
    }

    protected void askForPermission(String permission) {

        switch (permission){
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                //Sino tenemos el permiso lo pedimos
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{permission}, this.scanRequestType);
                    //ActivityCompat.requestPermissions(this, new String[]{INTERNET}, this.scanRequestType);
                }

                else {
                    this.runScantypeSelected();
                }
            break;

            case Manifest.permission.INTERNET:

            break;
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        this.requestCode = requestCode;
        Log.d("REQUEST_CODE", String.valueOf(requestCode));
        // If request is cancelled, the result arrays are empty.

        switch (requestCode){
            case 2:
                break;
            case RequiredPermissions.READ_INTERNAL_STORAGE_PERMISSION:
                Log.d("PERMISSION GRANTED","READ_INTERNAL_STORAGE");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runScantypeSelected();
                    SplashActivity.sharedPreferences = getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
                    SplashActivity.editor = sharedPreferences.edit();
                    SplashActivity.editor.putBoolean("accessFilesPermission", true);
                    SplashActivity.editor.apply();

                }
                else{
                    findViewById(R.id.reloadButtons).setVisibility(View.VISIBLE);
                    Toast.makeText(this,"No se obtuvo el permiso",Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.explanation_permission_access_files).setTitle(R.string.important_tittle).setIcon(R.drawable.error);
                    AlertDialog dialog =  builder.create();
                    dialog.setCancelable(true);
                    dialog.show();
                }
                break;
        }



    }


    private class AsyncReadFile extends AsyncTask<Void, File, Void> {
        private int code;
        private CustomAudioFile path;
        private MediaMetadataRetriever mediaMetadataRetriever;

        AsyncReadFile(int code,CustomAudioFile path){
            this.code = code;
            this.path = path;
        }

        protected ArrayList<CustomAudioFile> getFile(File dir) {
            CustomAudioFile listFile[] = (CustomAudioFile[])dir.listFiles();
            if (listFile != null && listFile.length > 0) {
                for (int i = 0; i < listFile.length; i++) {

                    if (listFile[i].isDirectory()) {
                        folderList.add(listFile[i]);
                        getFile(listFile[i]);

                    } else {
                        if (listFile[i].getName().endsWith(".mp3")) {
                            files.add(listFile[i]);
                        }
                    }

                }
            }
            return this.code == 1 ? files:folderList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<CustomAudioFile> arrayListFiles = getFile(this.path);
            for (int i = 0; i < arrayListFiles.size(); i++) {
                publishProgress(arrayListFiles.get(i));
            }
            System.out.println("Numero de elemntos en arrayListFiles "+arrayListFiles.size());
            return null;
        }


        @Override
        protected void onPreExecute() {
            try{
                filesAdapter.clear();
                ListView listView= (ListView) findViewById(R.id.list_view_songs);
                listView.invalidateViews();
                files.clear();
                folderList.clear();
                selectedTracks.clear();
            }catch (Exception e){

            }
            findViewById(R.id.reloadButtons).setVisibility(View.GONE);
            searchView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            findViewById(R.id.fab).setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(Void result) {
            progressBar.setVisibility(View.GONE);
            SelectFolderActivity.this.fab.setVisibility(View.VISIBLE);
            SelectFolderActivity.this.fab.setAlpha(0.8f);
            SelectFolderActivity.this.fab.setEnabled(false);
            searchView.setVisibility(View.INVISIBLE);
            loadingMetadataLayout.setVisibility(View.VISIBLE);
            new AsyncSetMetadata().execute();
        }
        @Override
        protected void onProgressUpdate(File... progress) {
            super.onProgressUpdate(progress);
            filesAdapter.add(progress[0]);
        }
    }

    private class AsyncSetMetadata extends AsyncTask<Void, Void, Void> {
        private MediaMetadataRetriever mediaMetadataRetriever;
        private ListView listView = (ListView) findViewById(R.id.list_view_songs);

        AsyncSetMetadata(){
            mediaMetadataRetriever = new MediaMetadataRetriever();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            for (int i = 0; i < SelectFolderActivity.this.filesAdapter.getCount(); i++) {
                mediaMetadataRetriever = new MediaMetadataRetriever();
                Log.d("EL PATH DEL FILE", SelectFolderActivity.this.filesAdapter.getItem(i).getPath());
                mediaMetadataRetriever.setDataSource(SelectFolderActivity.this.filesAdapter.getItem(i).getPath());
                ((CustomAudioFile) SelectFolderActivity.this.filesAdapter.getItem(i)).setTitle(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                .setArtist(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                .setAlbum(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                .setAuthor(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR))
                .setGenre(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE))
                .setComposer(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER))
                .setTrackNumber(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));

                /*if (mediaMetadataRetriever.getEmbeddedPicture() != null){
                    assert ((CustomAudioFile) SelectFolderActivity.this.filesAdapter.getItem(i)) != null;
                    ((CustomAudioFile) SelectFolderActivity.this.filesAdapter.getItem(i)).setAlbumArt(mediaMetadataRetriever.getEmbeddedPicture()).
                                    setDecodedAlbumArt(BitmapFactory.decodeByteArray(
                                            ((CustomAudioFile) SelectFolderActivity.this.filesAdapter.getItem(i)).getAlbumArt(),
                                            0,
                                            ((CustomAudioFile) SelectFolderActivity.this.filesAdapter.getItem(i)).getByteLengthAlbum())
                                    );
                }*/

                mediaMetadataRetriever.release();
                mediaMetadataRetriever = null;
                publishProgress();

            }
            return null;
        }


        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Void result) {
            searchView.setVisibility(View.VISIBLE);
            loadingMetadataLayout.setVisibility(View.GONE);
        }
        @Override
        protected void onProgressUpdate(Void... progress) {
            super.onProgressUpdate();
           this.listView.invalidateViews();
        }
    }

    private class AsyncProcessFile extends AsyncTask<Void, CustomAudioFile, Void> {
        private Set<Map.Entry<Integer,String>> entrySet;

        AsyncProcessFile(){
        }

        @Override
        protected Void doInBackground(Void... voids) {
            entrySet = selectedTracksList.entrySet();
            for (Map.Entry<Integer,String> entry:entrySet) {
                boolean completeData = false;
                String name = entry.getValue();
                int position = entry.getKey();
                String newName = "";
                String artistName = "";
                String albumName = "";
                String imageUrl = "";
                File file = new File(name);
                GnMusicIdFile gnMusicIdFile = null;
                GnMusicIdFileInfoManager gnMusicIdFileInfoManager = null;
                GnMusicIdFileInfo gnMusicIdFileInfo = null;

                try {
                    gnMusicIdFile = new GnMusicIdFile(gnUser, musicIdFileEvents);
                    gnMusicIdFileInfoManager = gnMusicIdFile.fileInfos();
                    gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(file.getName());
                    gnMusicIdFileInfo.fileName(name);
                    gnMusicIdFileInfo.fingerprintFromSource(new GnAudioFile(new File(name )));
                    gnMusicIdFile.doTrackId(GnMusicIdFileProcessType.kQueryReturnAll ,GnMusicIdFileResponseType.kResponseMatches);
                    newName = gnMusicIdFileInfo.dataMatchResponse().dataMatches().at(0).next().getAsAlbum().tracksMatched().at(0).next().title().display();
                    artistName = gnMusicIdFileInfo.dataMatchResponse().dataMatches().at(0).next().getAsAlbum().tracksMatched().at(0).next().artist().name().display();
                    albumName = gnMusicIdFileInfo.dataMatchResponse().dataMatches().at(0).next().getAsAlbum().title().display();
                    imageUrl = gnMusicIdFileInfo.dataMatchResponse().dataMatches().at(0).next().getAsAlbum().coverArt().asset(GnImageSize.kImageSizeSmall).url();




                    Log.d("MATCHES_TITLE_BG", newName);
                    Log.d("MATCHES_ARTIST_BG", artistName);
                    Log.d("MATCHES_ALBUM_BG", albumName);
                    Log.d("MATCHES_ALBUM_ART_BG", imageUrl);
                } catch (GnException e) {
                    e.printStackTrace();
                }
                CustomAudioFile currentFile = null;
                CustomAudioFile newFile = null;

                if(newName.equals("") && artistName.equals("") && albumName.equals("") && imageUrl.equals("")) {
                    currentFile = (CustomAudioFile) filesAdapter.getItem(position);//new CustomAudioFile(filesAdapter.getItem(position).getAbsolutePath());
                    currentFile.setStatus(CustomAudioFile.FILE_STATUS_BAD);
                    currentFile.setPosition(position);
                }
                else {
                    MusicMetadataSet musicMetadataSet;
                    MyID3 myID3 = null;
                    MusicMetadata iMusicMetadata = null;
                    String newFullName = "";

                    if(!newName.equals("")) {
                        currentFile = (CustomAudioFile) filesAdapter.getItem(position);
                        boolean result = currentFile.renameTo(new CustomAudioFile(currentFile.getParent() + "/" + newName + ".mp3"));
                        boolean allData = !artistName.equals("") && !albumName.equals("") && !imageUrl.equals("");
                        currentFile.setTitle(newName);
                        myID3 = new MyID3();
                        try {
                            musicMetadataSet = myID3.read((File) currentFile);
                            //assert musicMetadataSet != null;
                            if (musicMetadataSet == null) {
                                //publishProgress((CustomAudioFile) null);
                                continue;
                            } else {
                                iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();

                                iMusicMetadata.setSongTitle(newName);
                                if (!artistName.equals("")) {
                                    iMusicMetadata.setArtist(artistName);
                                    ((CustomAudioFile) currentFile).setArtist(artistName);
                                    Log.d("GET_ARTIST", ((CustomAudioFile) currentFile).getArtist());
                                }

                                if (!albumName.equals("")) {
                                    iMusicMetadata.setAlbum(albumName);
                                    ((CustomAudioFile) currentFile).setAlbum(albumName);
                                    Log.d("GET_ALBUM", ((CustomAudioFile) currentFile).getAlbum());
                                }
                                    /*if (!imageUrl.equals("")) {
                                        Vector<Byte> vector = new Vector<>();
                                        byte[] imgData = new GnAssetFetch(gnUser, imageUrl).data();
                                        int i = 0;
                                        for (byte b : imgData) {
                                            vector.add(i++,b);
                                        }
                                        iMusicMetadata.setPictureList(vector);
                                        currentFile.setAlbumArt(imgData);
                                        ((CustomAudioFile)currentFile).setDecodedAlbumArt(BitmapFactory.decodeByteArray(((CustomAudioFile)currentFile).getAlbumArt(),0,((CustomAudioFile)currentFile).getByteLengthAlbum()));
                                    }*/
                                myID3.update(currentFile, musicMetadataSet, iMusicMetadata);
                                currentFile.setStatus(CustomAudioFile.FILE_STATUS_OK);
                                currentFile.setPosition(position);
                            }
                        }
                        catch (IOException | ID3WriteException e) {
                            e.printStackTrace();
                        }
                    }


                }
                publishProgress(currentFile);

            }
            return null;
        }


        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.bringToFront();
            SelectFolderActivity.this.fab.setVisibility(View.GONE);
            SelectFolderActivity.this.fab.setAlpha(0.8f);
            SelectFolderActivity.this.fab.setEnabled(false);
            ListView listView= (ListView) findViewById(R.id.list_view_songs);
            listView.setEnabled(false);

        }

        @Override
        protected void onPostExecute(Void result) {
            progressBar.setVisibility(View.GONE);
            SelectFolderActivity.this.fab.setVisibility(View.VISIBLE);
            SelectFolderActivity.this.fab.setAlpha(1f);
            SelectFolderActivity.this.fab.setEnabled(true);
            ListView listView= (ListView) findViewById(R.id.list_view_songs);
            listView.setEnabled(true);
            filesAdapter.notifyDataSetChanged();
            listView.invalidateViews();
            Toast.makeText(getApplicationContext(),"Tarea terminada exitosamente",Toast.LENGTH_SHORT).show();

        }
        @Override
        protected void onProgressUpdate(CustomAudioFile... customAudioFile) {
            super.onProgressUpdate(customAudioFile);
            if(customAudioFile != null) {
                if (customAudioFile[0].getStatus().equals(CustomAudioFile.FILE_STATUS_BAD)) {
                    Toast.makeText(getApplicationContext(), getText(R.string.no_all_data_found) + customAudioFile[0].getName() + ".", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), getText(R.string.audiofile_succesfully_processed), Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(getApplicationContext(), "No se pudo procesar el archivo, intentelo de nuevo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("SelectFolder Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        Log.d("onSaveInstanceState","onSaved");

        savedInstanceState.putBoolean("KEEP_LIST",true);

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("onStop","onStop");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    class InfoTrack{
        public Integer position;
        public String absolutePath;
    }

    class MusicIdFileEvents implements IGnMusicIdFileEvents {
        protected String trackTitle;
        protected String artist;
        protected HashMap<String,String> gnStatusToDisplay;
        public MusicIdFileEvents(){
            gnStatusToDisplay = new HashMap<String, String>();
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingBegin","Begin processing file");
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusInfoQuery","Querying file info");
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingComplete","IdentificationComplete");
        }

        @Override
        public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long l, long l1, IGnCancellable iGnCancellable) {
            //SelectFolderActivity.this.progressBar.setVisibility(View.VISIBLE);
            Log.d("FILE STATUS EVENT",gnMusicIdFileCallbackStatus.toString());
        }

        @Override
        public void gatherFingerprint(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {

            try {
                //gnMusicIdFileInfo.fingerprintFromSource( new GnAudioFile( new File(gnMusicIdFileInfo.fileName())) );
                Log.e("GATHER FINGERPRINT", gnMusicIdFileInfo.fingerprint());
                //gnMusicIdFileInfo.

            } catch (GnException e) {
                e.printStackTrace();

            }

        }

        @Override
        public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            try {
                Log.d("GATHER METADATA",gnMusicIdFileInfo.trackTitle());
                //Log.d("GATHER METADATA",gnMusicIdFileInfo.albumTitle());
                //Log.d("GATHER METADATA",gnMusicIdFileInfo.albumArtist());
                //Log.d("GATHER METADATA", String.valueOf(gnMusicIdFileInfo.discNumber()));
                //Log.d("GATHER METADATA", String.valueOf(gnMusicIdFileInfo.trackNumber()));
            } catch (GnException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {
            GnAlbumIterable gnAlbumIterable = gnResponseAlbums.albums();
            GnAlbumIterator gnAlbumIterator =  gnAlbumIterable.getIterator();
            while(gnAlbumIterator.hasNext()){

                try {
                    gnAlbumIterator.next().title().display();
                    Log.d("ALBUM RESULT", gnAlbumIterator.next().title().display());
                } catch (GnException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) {

            try {
                Log.d("MATCHES_TITLE", gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().tracksMatched().at(0).next().title().display());
                Log.d("MATCHES_ARTIST", gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().tracksMatched().at(0).next().artist().name().display());
                Log.d("MATCHES_ALBUM", gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().title().display());

            } catch (GnException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            try {
                Log.d("RESULT NOT FOUND", gnMusicIdFileInfo.trackTitle());
            } catch (GnException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void musicIdFileComplete(GnError gnError) {
            //SelectFolderActivity.this.progressBar.setVisibility(View.GONE);
            //SelectFolderActivity.snackbar.setText("ARTISTA: "+ this.artist).show();

        }

        @Override
        public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {
            Log.d("STATUS EVENT HOLA", "HOla");
        }

        public String getTrackTitle(){
            return this.trackTitle;
        }
    }



}
