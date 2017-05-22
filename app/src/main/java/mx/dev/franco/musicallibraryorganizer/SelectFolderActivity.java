package mx.dev.franco.musicallibraryorganizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnAudioFile;
import com.gracenote.gnsdk.GnDataLevel;
import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnLookupData;
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
import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static mx.dev.franco.musicallibraryorganizer.SplashActivity.sharedPreferences;

public class SelectFolderActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    static String TAG_SELECT_FOLDER = SelectFolderActivity.class.getName();
    static boolean apiInitialized = false;
    static boolean isProcessingTask = false;
    CustomMediaPlayer mediaPlayer;
    static ConcurrentHashMap<Integer,String> selectedTracksList;
    int scanRequestType;
    ProgressBar progressBar;
    SearchView searchView;
    FloatingActionButton fab;
    static Snackbar snackbar;
    static ArrayAdapter<AudioItem> audioItemArrayAdapterAdapter; //Adapter with AudioItem objects for display in listview
    int requestCode;
    TypeScanDialogFragment typeScanDialog;
    LinearLayout view;
    RelativeLayout loadingMetadataLayout;
    DetailsTrackDialog detailsTrackDialog;
    ArrayList<String> modifiedFiles;
    boolean isExecutingTask = false;

    DataTrackDbHelper dbHelper;
    ServiceConnection serviceConnection;
    NewFilesScannerService newFilesScannerService;
    DetectorChangesFiles detectorChangesFiles;

    volatile GnManager gnManager;
    volatile GnUser gnUser;
    String gnsdkLicenseString = "-- BEGIN LICENSE v1.0 A75228BC --\\r\\nname: \\r\\nnotes: Gracenote Open Developer Program\\r\\nstart_date: 0000-00-00\\r\\nclient_id: 843162123\\r\\nmusicid_file: enabled\\r\\nmusicid_text: enabled\\r\\nmusicid_stream: enabled\\r\\nmusicid_cd: enabled\\r\\nplaylist: enabled\\r\\nvideoid: enabled\\r\\nvideo_explore: enabled\\r\\nlocal_images: enabled\\r\\nlocal_mood: enabled\\r\\nvideoid_explore: enabled\\r\\nacr: enabled\\r\\nepg: enabled\\r\\n-- SIGNATURE A75228BC --\\r\\nlAADAgAe/WEZPZ5IaetmxgKEpZm7EjG1SLm/yLvyhTwzlr8cAB4R2GcEuN/6PovFycqgCmnnmr3ioB/KXt3EDTz8yYk=\\r\\n-- END LICENSE A75228BC --\\r\\n";
    volatile GnLocale gnLocale;
    static final String gnsdkClientId = "843162123";
    static final String gnsdkClientTag = "4E937B773F03BA431014169770593072";
    static final String appString = "AutoMusicTagFixer";
    static boolean rescan = false;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        //Intent intent = new Intent(this, NewFilesScannerService.class);
        //bindService(intent,serviceConnection, Context.BIND_NOT_FOREGROUND);
    }

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        /*serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                NewFilesScannerService.BinderService binderService = (NewFilesScannerService.BinderService) service;
                newFilesScannerService = binderService.getService();
                newFilesScannerService.setParameters(audioItemArrayAdapterAdapter, SelectFolderActivity.this);
                modifiedFiles = newFilesScannerService.getChangedFiles();
                Log.d(TAG_SELECT_FOLDER,"CONNECTED");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG_SELECT_FOLDER,"DISCONNECTED");
            }
        };*/

        //detectorChangesFiles = new DetectorChangesFiles("/storage/emulated/0/Music/TestMusic/", this, audioItemArrayAdapterAdapter);
        //detectorChangesFiles.startWatching();

        setContentView(R.layout.activity_select_folder);
        view = (LinearLayout) findViewById(R.id.list_of_files);
        loadingMetadataLayout = (RelativeLayout) findViewById(R.id.loadingMetadataLayout);

        selectedTracksList = new ConcurrentHashMap<Integer, String>();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.searching);
        progressBar.setVisibility(View.GONE);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#ff0099cc"), PorterDuff.Mode.MULTIPLY);
        searchView = (SearchView) findViewById(R.id.searchView);
        snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG).setAction("Action", null);
        snackbar.getView().setBackgroundColor(Color.parseColor("#ff0099cc"));
        audioItemArrayAdapterAdapter = new TrackAdapter(SelectFolderActivity.this,new ArrayList<AudioItem>());

        //Verificamos que ya se han concedido permisos, de los contrario, aparecera el Dialogo que nos pregunta el tipo de busqueda de musica,
        //el cual nos va a pedir el permiso de acceso a los archivos
        SplashActivity.sharedPreferences = getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
        //Default value is false, otherwise will be the obtained value
        boolean grantedAccessFiles = sharedPreferences.getBoolean("accessFilesPermission", false);
        initializeAPIGnsdk();

        //If we already had the permission granted, lets go to read data from database and pass it to audioItemArrayAdapterAdapter variable, to show in the ListView,
        //otherwise we ask the scan type
        if(!grantedAccessFiles) {
            typeScanDialog = new TypeScanDialogFragment();
            typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");
            typeScanDialog.setCancelable(false);
        }
        else{
            AsyncReadFile asyncReadFile = new AsyncReadFile(this.scanRequestType);
            asyncReadFile.execute();
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
                    if(mediaPlayer != null && mediaPlayer.isPlaying()){
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }

                    if(!selectedTracksList.isEmpty()) {
                        if(!apiInitialized){
                            snackbar.setText("La API de reconocimiento se esta inicializando, espere unos instantes.");
                            snackbar.show();
                            return;
                        }
                        snackbar.setText("Procesando... espere por favor.");
                        snackbar.show();

                        AsyncProcessTrack asyncProcessTrack = new AsyncProcessTrack();
                        asyncProcessTrack.execute();
                    }
                    else {
                        snackbar.setText("No hay ninguna canción seleccionada para corregir");
                        snackbar.show();
                    }
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

    /*static void hideFiles(final boolean showShortFiles, boolean showLittleFiles){
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < audioItemArrayAdapterAdapter.getCount(); i++) {
                    if(!showShortFiles) {
                        audioItemArrayAdapterAdapter.getItem(i).setVisible(audioItemArrayAdapterAdapter.getItem(i).getDuration() > 180000); //show files only grater than 3 minutes
                        }
                    else {
                        audioItemArrayAdapterAdapter.getItem(i).setVisible(audioItemArrayAdapterAdapter.getItem(i).getDuration() < 180000); //show all files
                    }
                    Log.d("THREAD", audioItemArrayAdapterAdapter.getItem(i).isVisible()+" " + audioItemArrayAdapterAdapter.getItem(i).getDuration());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            audioItemArrayAdapterAdapter.notifyDataSetChanged();
                        }
                    });

                }

            }
        }).start();
    }*/

    void initializeAPIGnsdk(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //We initialize the necessary objects for using the GNSDK API in a different thread for not blocking the UI
                try {
                    gnManager =  new GnManager(SelectFolderActivity.this,gnsdkLicenseString,GnLicenseInputMode.kLicenseInputModeString);
                    gnUser = new GnUser(new GnUserStore(getApplicationContext()),gnsdkClientId,gnsdkClientTag,appString);
                    gnLocale = new GnLocale(GnLocaleGroup.kLocaleGroupMusic,GnLanguage.kLanguageSpanish,GnRegion.kRegionDefault, GnDescriptor.kDescriptorDetailed,gnUser);
                    gnLocale.setGroupDefault();
                    apiInitialized = true;
                    Log.d("GNSDK","API Initialized");
                } catch (GnException e) {
                    e.printStackTrace();
                }

            }
        }).start();
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

        //getMenuInflater().inflate(R.menu.select_folder, menu);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        //item.setChecked(true);
        int id = item.getItemId();

        if (id == R.id.rate) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.share) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        }
        else if(id == R.id.settings){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.scan) {

            rescan = true;
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
    protected boolean checkIfExist(final String path){
        Log.d("PATH EXIST", path);
        if(!new File(path).exists()){
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                builder.setTitle("Archivo no encontrado").setMessage("El archivo sobre el que se esta ejecutando la " +
                        "acción no existe, fue movido, eliminado o renombrado, ¿Deseas quitarlo de la lista?")
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            removeItemFromLists(path);
                            }
                        });
            final AlertDialog dialog =  builder.create();
            dialog.setCancelable(false);
            dialog.show();
            return false;
        }
        return true;
    }

    private void removeItemFromLists(String path){
        for(int t = 0 ; t < audioItemArrayAdapterAdapter.getCount() ; t++){
            if(audioItemArrayAdapterAdapter.getItem(t).getNewAbsolutePath().equals(path)){
                dbHelper.removeItem(audioItemArrayAdapterAdapter.getItem(t).getId(), TrackContract.TrackData.TABLE_NAME);
                audioItemArrayAdapterAdapter.remove(audioItemArrayAdapterAdapter.getItem(t));
                break;
            }
        }

        try {
            for(int g = 0 ; g < selectedTracksList.size() ; g++){
                if(selectedTracksList.get(g).equals(path)){
                    selectedTracksList.remove(g);
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    protected AudioItem selectItemByIdOrPath(long id, String path){
        AudioItem audioItem = null;
        if(id != -1){
            for(int t = 0 ; t < audioItemArrayAdapterAdapter.getCount() ; t++){
                if(audioItemArrayAdapterAdapter.getItem(t).getId() == id ){
                        audioItem = audioItemArrayAdapterAdapter.getItem(t);
                    break;
                }
            }
            return audioItem;
        }

        if(!path.equals("")){
            for(int t = 0 ; t < audioItemArrayAdapterAdapter.getCount() ; t++){
                if(audioItemArrayAdapterAdapter.getItem(t).getNewAbsolutePath().equals(path)){
                        audioItem = audioItemArrayAdapterAdapter.getItem(t);
                    break;
                }
            }
        }

        return audioItem;
    }

    protected AudioItem selectItemByAbsolutePosition(int pos){
        AudioItem audioItem = null;
        for(int t = 0 ; t < audioItemArrayAdapterAdapter.getCount() ; t++){
            if(audioItemArrayAdapterAdapter.getItem(t).getPosition() == pos){
                audioItem = audioItemArrayAdapterAdapter.getItem(t);
                break;
            }
        }
        return audioItem;
    }

    protected void onClickPlayImageButton(View view,int position) throws IOException, InterruptedException {
        assert ((AudioItem)audioItemArrayAdapterAdapter.getItem(position)) != null;
        int absolutePosition = ((AudioItem)audioItemArrayAdapterAdapter.getItem(position)).getPosition();
        AudioItem audioItem = selectItemByAbsolutePosition(absolutePosition);
        Log.d("VISIBLE",audioItem.isVisible()+"");
        String trackPath = audioItemArrayAdapterAdapter.getItem(position).getNewAbsolutePath();//audioItem.getNewAbsolutePath();
        boolean existFile = checkIfExist(trackPath);
        if(!existFile){
            return;
        }

        if(mediaPlayer == null){
            mediaPlayer = CustomMediaPlayer.getInstance();
            mediaPlayer.setParameters(audioItemArrayAdapterAdapter, this);
        }
        /*if(mediaPlayer.getCurrentPositionAudioSource() != position || !mediaPlayer.isPlaying()) {
            snackbar.setText(getResources().getString(R.string.snackbar_message_track_preview) + ": " + trackPath).show();
        }*/
        mediaPlayer.playPreview(view);

    }

    protected void onClickCheckedItem(View v){
        int pos = (int) v.getTag();
        v.setBackground(getResources().getDrawable(R.drawable.checked2, null));
        selectItem(pos, view);
    }

    protected void onClickContextualMenu(final View view){
        final int p = (int) view.getTag();

        PopupMenu trackContextualMenu = new PopupMenu(this,view);
        MenuInflater menuInflater = trackContextualMenu.getMenuInflater();
        menuInflater.inflate(R.menu.track_contextual_menu, trackContextualMenu.getMenu());
        trackContextualMenu.show();
        trackContextualMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String pathTrack = dbHelper.getPath(audioItemArrayAdapterAdapter.getItem(p).getId(), TrackContract.TrackData.TABLE_NAME);

                switch (item.getItemId()){
                    case R.id.action_details:

                        if(!checkIfExist(pathTrack)){
                            break;
                        }

                        detailsTrackDialog = new DetailsTrackDialog();
                        detailsTrackDialog.setData(pathTrack, audioItemArrayAdapterAdapter, p, dbHelper);
                        detailsTrackDialog.show(getFragmentManager(), DetailsTrackDialog.FRAGMENT_NAME);
                        detailsTrackDialog.setCancelable(true);
                        break;

                    case R.id.action_delete:
                        if(mediaPlayer != null && mediaPlayer.isPlaying()){
                            mediaPlayer.stop();
                        }
                        removeItemFromLists(pathTrack);
                        break;

                    default:
                        break;
                }

                return false;
            }
        });
    }

    protected void onClickStatusIcon(View v){
        int p = (int) v.getTag();
        Log.d("POSITION",p+"");
        int status = ((AudioItem)audioItemArrayAdapterAdapter.getItem(p)).getStatus();
        Toast toast = null;
        switch (status){
            case AudioItem.FILE_STATUS_OK:
                toast =  Toast.makeText(this,getText(R.string.file_status_ok),Toast.LENGTH_LONG);
                break;
            case AudioItem.FILE_STATUS_INCOMPLETE:
                toast =  Toast.makeText(this,getText(R.string.file_status_incomplete),Toast.LENGTH_LONG);
                break;
            case AudioItem.FILE_STATUS_BAD:
                toast =  Toast.makeText(this,getText(R.string.file_status_bad),Toast.LENGTH_LONG);
                break;
            case AudioItem.FILE_STATUS_EDIT_BY_USER:
                toast =  Toast.makeText(this,getText(R.string.file_status_edit_by_user),Toast.LENGTH_LONG);
                break;
            case AudioItem.FILE_STATUS_DOES_NOT_EXIST:
                toast =  Toast.makeText(this,getText(R.string.file_status_does_not_exist),Toast.LENGTH_LONG);
                break;
            default:
                toast =  Toast.makeText(this,getText(R.string.file_status_no_processed),Toast.LENGTH_LONG);
                break;
        }

        toast.show();
    }

    protected void selectItem(int position, View view){
        String message = "";

        int absolutePosition = audioItemArrayAdapterAdapter.getItem(position).getPosition();
        AudioItem audioItem = selectItemByAbsolutePosition(absolutePosition);

        if( (audioItem.getStatus() != AudioItem.FILE_STATUS_NO_PROCESSED)) {
            audioItem.setStatus(AudioItem.FILE_STATUS_NO_PROCESSED);
        }

        if(!selectedTracksList.containsKey((int)(audioItem.getId())))  {
            selectedTracksList.put((int)audioItem.getId(), audioItem.getNewAbsolutePath());
            ContentValues data = new ContentValues();
            data.put(TrackContract.TrackSelected.COLUMN_NAME_ID_TRACK, (int)audioItem.getId());
            data.put(TrackContract.TrackSelected.COLUMN_NAME_CURRENT_FULL_PATH, audioItem.getNewAbsolutePath());
            dbHelper.saveFileData(data, TrackContract.TrackSelected.TABLE_NAME);
            message = getResources().getString(R.string.toast_track_added_to_fix);
            audioItem.setSelected(true);
        }else {
            selectedTracksList.remove((int)audioItem.getId());
            dbHelper.removeItem(audioItem.getId(), TrackContract.TrackSelected.TABLE_NAME);
            message = getResources().getString(R.string.toast_track_remove_from_fixlist);
            audioItem.setSelected(false);
        }
        //Para que se redibujen los items en pantalla y se vea el cambio de añadir una cancion.
        audioItemArrayAdapterAdapter.notifyDataSetChanged();
    }

    protected void runScantypeSelected(){
        //Si ya tenemos el permiso, verificamos que tipo de escaneo se hara, en caso que se requiera hacer un escaneo
        if(this.scanRequestType!=1){
            this.scanRequestType = 1;
        }
        if (this.scanRequestType == 1) {
            Toast.makeText(this, "Buscando música", Toast.LENGTH_LONG).show();
            //Permiso concedido, obtenemos las carpeta del sistema en busca de arhivos mp3 y el arreglo lo pasamos al adapter.
            //audioItemArrayAdapterAdapter = new TrackAdapter(this,new ArrayList<AudioItem>());

            setupAdapter();
            if(dbHelper == null){
                dbHelper = new DataTrackDbHelper(this);
            }

            AsyncReadFile asyncReadFile = new AsyncReadFile(this.scanRequestType);
            asyncReadFile.execute();
        } else {
            snackbar.setText(R.string.snackbar_message_in_development);
            snackbar.show();
        }
    }

    protected void setupAdapter(){
        Log.d("ADAPTER","SETUP_ADAPTER");
        audioItemArrayAdapterAdapter.setNotifyOnChange(true);
        final ListView listView = (ListView) findViewById(R.id.list_view_songs);
        listView.setAdapter(audioItemArrayAdapterAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?>adapter, View view, int position, long arg){
                //Log.d("Click aqui",position+"");
                try {

                    onClickPlayImageButton(view,position);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        } );
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
                selectItem(position,view);
                return true;
            }
        });

        SearchView searchView = (SearchView) this.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                audioItemArrayAdapterAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    private void detachAdapter(){
        Log.d("ADAPTER","DETACH_ADAPTER");
        final ListView listView = (ListView) findViewById(R.id.list_view_songs);
        listView.setAdapter(null);
        listView.setOnItemClickListener(null);
        listView.setOnItemLongClickListener(null);

        SearchView searchView = (SearchView) this.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(null);
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
        //Log.d("REQUEST_CODE", String.valueOf(requestCode));
        // If request is cancelled, the result arrays are empty.

        switch (requestCode){
            case 2:
                break;
            case RequiredPermissions.READ_INTERNAL_STORAGE_PERMISSION:
                //Log.d("PERMISSION GRANTED","READ_INTERNAL_STORAGE");
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
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    View view = inflater.inflate(R.layout.warning_permission,null,false);
                    Button buttonOk = (Button) view.findViewById(R.id.ok_button);
                    builder.setView(view);
                    final AlertDialog dialog =  builder.create();
                    buttonOk.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.cancel();
                        }
                    });
                    dialog.setCancelable(false);
                    dialog.show();
                }
                break;
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("RESULT_ACTIVITY", requestCode + "");
        if (requestCode == DetailsTrackDialog.INTENT_OPEN_GALLERY && data != null){
            Uri imageData = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageData);
                if(bitmap.getHeight() > 1080 || bitmap.getWidth() > 1080){
                    Toast.makeText(this, "Las dimensiones de la imagen son mayores a 1080x1080 pixeles, selecciona otra o reduce sus dimensiones",Toast.LENGTH_LONG).show();
                }
                else {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,75,byteArrayOutputStream);
                    DetailsTrackDialog.newAlbumArt = byteArrayOutputStream.toByteArray();
                    ((ImageButton) detailsTrackDialog.getDialog().findViewById(R.id.albumArt)).setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class AsyncReadFile extends AsyncTask<Void, Integer, Void> {
        private int code;

        AsyncReadFile(int code){
            this.code = code;
            if(SelectFolderActivity.rescan){
                dbHelper.clearDb();
                audioItemArrayAdapterAdapter.clear();
                detachAdapter();
            }
        }

        Cursor getDataFromDevice() {

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3");

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0" + " and "
                    + MediaStore.Audio.Media.MIME_TYPE + " = " + " \'" +mimeType + "\'"; //ONly select music mp3 files

            String[] projection = { //Columns to retrieve
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA, // filepath of the audio file
                    MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                    MediaStore.Audio.Media.SIZE
            };

            Cursor cursor = getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    MediaStore.Audio.Media.DISPLAY_NAME);

            // the last parameter sorts the data alphanumerically

            return cursor;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Cursor cursor;
            if (SelectFolderActivity.this.dbHelper == null) {
                SelectFolderActivity.this.dbHelper = new DataTrackDbHelper(getApplicationContext());
            }

            if (!SplashActivity.existDatabase || rescan){

                cursor = getDataFromDevice();
                int count = 0, count2 = cursor.getCount();
                if (cursor != null && count2 > 0) {

                    while (cursor.moveToNext()) {
                        Log.d("NEW AUDIO_1",count+"");
                        String title = cursor.getString(0) != null ? cursor.getString(0) : "";
                        String artist = cursor.getString(1) != null ? cursor.getString(1) : "";
                        String album = cursor.getString(2) != null ? cursor.getString(2) : "";
                        int duration = cursor.getInt(3);
                        String humanReadableDuration = AudioItem.getHumanReadableDuration(duration);
                        String fullPath = Uri.parse(cursor.getString(4)).toString();
                        String filename = cursor.getString(5);
                        String fileSize = cursor.getString(6);
                        float fileSizeInMb = Integer.parseInt(fileSize)/1048576;

                        final AudioItem audioItem = new AudioItem();
                        audioItem.setTitle(title).setArtist(artist).setAlbum(album).setDuration(duration).setHumanReadableDuration(humanReadableDuration).setNewAbsolutePath(fullPath).setFileName(filename).setPosition(count).setSize(fileSizeInMb);

                        ContentValues values = new ContentValues();
                        values.put(TrackContract.TrackData.COLUMN_NAME_TITLE, title);
                        values.put(TrackContract.TrackData.COLUMN_NAME_ARTIST, artist);
                        values.put(TrackContract.TrackData.COLUMN_NAME_ALBUM, album);
                        values.put(TrackContract.TrackData.COLUMN_NAME_DURATION,duration);
                        values.put(TrackContract.TrackData.COLUMN_NAME_FILE_SIZE, fileSizeInMb);
                        values.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME,filename);
                        values.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH, fullPath);
                        values.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_NO_PROCESSED);
                        long _id = SelectFolderActivity.this.dbHelper.saveFileData(values, TrackContract.TrackData.TABLE_NAME);
                        audioItem.setId(_id);
                        count++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                audioItemArrayAdapterAdapter.add(audioItem);
                                audioItemArrayAdapterAdapter.notifyDataSetChanged();

                            }
                        });
                        count++;
                        publishProgress((int) ((count / (float) count2) * 100)/2);

                        values.clear();
                    }
                    cursor.close();
                }
            }
            else {

                Log.d("READ FROM DB", "leyendo de la BD");
                cursor = SelectFolderActivity.this.dbHelper.getDataFromDB();
                int dataLength = cursor.getCount(), i = 0;
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        Log.d("NEW AUDIO", i + "");
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_TITLE)).equals("") ?
                                "No disponible" : cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_TITLE));
                        String artist = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_ARTIST)).equals("") ?
                                "No disponible" : cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_ARTIST));
                        String album = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_ALBUM)).equals("") ?
                                "No disponible" : cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_ALBUM));
                        String filename = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME)).equals("") ?
                                "No disponible" : cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME));
                        String id = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData._ID));
                        String fullPath = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH));
                        int totalSeconds = Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_DURATION)));
                        String sFilesizeInMb = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_FILE_SIZE));
                        float fFileSizeInMb = Float.parseFloat(sFilesizeInMb);
                        String status = cursor.getString(cursor.getColumnIndexOrThrow(TrackContract.TrackData.COLUMN_NAME_STATUS));

                        final AudioItem audioItem = new AudioItem();
                        audioItem.setTitle(title).setArtist(artist).setAlbum(album).setDuration(totalSeconds).setHumanReadableDuration(AudioItem.getHumanReadableDuration(totalSeconds)).setId(Long.parseLong(id)).setNewAbsolutePath(fullPath).setPosition(i).setStatus(Integer.parseInt(status)).setFileName(filename).setSize(fFileSizeInMb);
                        totalSeconds = 0;
                        i++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                audioItemArrayAdapterAdapter.add(audioItem);
                                audioItemArrayAdapterAdapter.notifyDataSetChanged();
                            }
                        });
                        publishProgress((int) ((i / (float) dataLength) * 100));
                    }
                    cursor.close();
                }
            }

            return null;
        }


        @Override
        protected void onPreExecute() {
            try{
                audioItemArrayAdapterAdapter.clear();
                ListView listView = (ListView) findViewById(R.id.list_view_songs);
                listView.invalidateViews();
                selectedTracksList.clear();
                detachAdapter();
            }catch (Exception e){
                e.printStackTrace();
            }
            setupAdapter();
            findViewById(R.id.reloadButtons).setVisibility(View.GONE);
            searchView.setVisibility(View.GONE);
            loadingMetadataLayout.setVisibility(View.VISIBLE);

            //progressBar.setVisibility(View.VISIBLE);
            findViewById(R.id.fab).setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(Void result) {
            SelectFolderActivity.this.fab.setVisibility(View.VISIBLE);
            searchView.setVisibility(View.VISIBLE);
            loadingMetadataLayout.setVisibility(View.INVISIBLE);
            Log.d("SIZE ADAPTER",audioItemArrayAdapterAdapter.getCount()+"");
            System.gc();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            ((TextView) findViewById(R.id.retrievingMetadataInfo)).setText(getText(R.string.retrieving_metadata).toString() + progress[0] + "%");
        }

    }

    private class AsyncProcessTrack extends AsyncTask<Void, ResultSet, Void> implements IGnMusicIdFileEvents{
        AudioItem currentAudioItem;
        Set<Map.Entry<Integer,String>> entrySet;
        ResultSet resultSet;
        HashMap<String,String> gnStatusToDisplay;
        AsyncProcessTrack(){
            gnStatusToDisplay = new HashMap<>();
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingBegin",getString(R.string.begin_processing));
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusInfoQuery",getString(R.string.querying_info_));
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingComplete",getString(R.string.complete_identification));
        }

        @Override
        protected Void doInBackground(Void... voids) {
            entrySet = selectedTracksList.entrySet();
            for (Map.Entry<Integer,String> entry:entrySet) {
                if(this.isCancelled()){
                    break;
                }

                final AudioItem audioItem = selectItemByIdOrPath(entry.getKey(), "");
                currentAudioItem = audioItem;

                final int pos = audioItem.getPosition();
                Log.d("AUDIO ITEM NAME", audioItem.getNewAbsolutePath() + " - position: "+ pos);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        audioItem.setProcessing(true);
                        audioItemArrayAdapterAdapter.notifyDataSetChanged();
                        ((ListView)findViewById(R.id.list_view_songs)).smoothScrollToPosition(pos);
                    }
                });
                Log.d(AsyncProcessTrack.class.getName(), "DO IN BACKGROUND");
                resultSet = new ResultSet();
                String name = entry.getValue();
                File file = new File(name);


                String newName = "";
                String artistName = "";
                String albumName = "";
                String trackNumber = "";
                String year = "";
                String genre = "";
                String imageUrl = "";

                GnMusicIdFile gnMusicIdFile = null;
                GnMusicIdFileInfoManager gnMusicIdFileInfoManager = null;
                GnMusicIdFileInfo gnMusicIdFileInfo = null;

                try {
                    gnMusicIdFile = new GnMusicIdFile(gnUser, this);
                    gnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
                    gnMusicIdFileInfoManager = gnMusicIdFile.fileInfos();
                    gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(file.getName());
                    gnMusicIdFileInfo.fileName(name);
                    gnMusicIdFileInfo.fingerprintFromSource(new GnAudioFile(file));
                    gnMusicIdFile.doTrackId(GnMusicIdFileProcessType.kQueryReturnAll, GnMusicIdFileResponseType.kResponseMatches);
                } catch (GnException e) {
                    e.printStackTrace();
                }

                try {
                    newName = gnMusicIdFileInfo.dataMatchResponse().dataMatches().at(0).next().getAsAlbum().tracksMatched().at(0).next().title().display();
                } catch (GnException e) {
                    e.printStackTrace();
                    newName = "";
                }

                try {
                    artistName = gnMusicIdFileInfo.albumResponse().albums().at(0).next().artist().name().display();
                } catch (GnException e) {
                    e.printStackTrace();
                    artistName = "";
                }
                try {
                    albumName = gnMusicIdFileInfo.dataMatchResponse().dataMatches().at(0).next().getAsAlbum().title().display();
                } catch (GnException e) {
                    e.printStackTrace();
                    albumName = "";
                }
                try {
                    imageUrl = gnMusicIdFileInfo.albumResponse().albums().getIterator().next().coverArt().asset(GnImageSize.kImageSizeMedium).url();
                } catch (GnException e) {
                    e.printStackTrace();
                    imageUrl = "";
                }
                try {
                    trackNumber = gnMusicIdFileInfo.albumResponse().albums().getIterator().next().trackMatched().trackNumber();
                } catch (GnException e) {
                    e.printStackTrace();
                    trackNumber = "";
                }
                try {
                    year = gnMusicIdFileInfo.albumResponse().albums().getIterator().next().year();
                } catch (GnException e) {
                    e.printStackTrace();
                    year = "";
                }
                try {
                    genre = gnMusicIdFileInfo.albumResponse().albums().getIterator().next().trackMatched().genre(GnDataLevel.kDataLevel_1);
                } catch (GnException e) {
                    e.printStackTrace();
                    genre = "";
                }

                Log.d("MATCHES_TITLE_BG", newName);
                Log.d("MATCHES_ARTIST_BG", artistName);
                Log.d("MATCHES_ALBUM_BG", albumName);
                Log.d("MATCHES_ALBUM_ART_BG", imageUrl);
                Log.d("MATCHES_NUMBER_BG", trackNumber);
                Log.d("MATCHES_YEAR_BG", year);
                Log.d("MATCHES_genre_BG", genre);

                resultSet.setName(newName);
                resultSet.setArtist(artistName);
                resultSet.setAlbum(albumName);
                resultSet.setImageUrl(imageUrl);
                resultSet.setTrack(trackNumber);
                resultSet.setYear(year);
                resultSet.setGenre(genre);
                resultSet.setAudioItem(audioItem);

                publishProgress(resultSet);
            }
            Log.d(AsyncProcessTrack.class.getName(), "DOINBACKGROUND");
            return null;
        }


        @Override
        protected void onPreExecute() {
            startTask();
            Log.d(AsyncProcessTrack.class.getName(), "PREEXECUTE");
        }

        @Override
        protected void onPostExecute(Void voids){
            finishTaskByUser(false);
            Log.d(AsyncProcessTrack.class.getName(), "POSTEXECUTE");

        }

        @Override
        protected void onProgressUpdate(ResultSet... resultSet) {
            super.onProgressUpdate(resultSet);
            if(this.isCancelled()){
                return;
            }

            ResultSet data = resultSet[0];

            ContentValues contentValues = new ContentValues();
            AudioItem resultSetAudioItem = data.getAudioItem();
            String newName = data.getName();
            String artistName = data.getArtist();
            String albumName = data.getAlbum();
            String trackNumber = data.getTrack();
            String year = data.getYear();
            String genre = data.getGenre();
            String imageUrl = data.getImageUrl();

            boolean dataTitle = !newName.equals(""), dataArtist = !artistName.equals(""), dataAlbum = !albumName.equals(""), dataImage = !imageUrl.equals(""),
                    dataTrackNumber = !trackNumber.equals(""), dataYear = !year.equals(""), dataGenre = !genre.equals("");

            if(!dataTitle && !dataArtist && !dataAlbum && !dataImage && !dataTrackNumber && !dataYear && !dataGenre) {
                resultSetAudioItem.setStatus(AudioItem.FILE_STATUS_BAD);
                contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_BAD);
                SelectFolderActivity.this.dbHelper.setStatus(resultSetAudioItem.getId(), contentValues);
                resultSetAudioItem.setSelected(false);
            }
            else {

                MusicMetadataSet musicMetadataSet;
                MyID3 myID3;
                MusicMetadata iMusicMetadata;
                File currentFile = null;
                File renamedFile = null;
                boolean existFile =  false;

                myID3 = new MyID3();
                try {
                    musicMetadataSet = myID3.read(new File(resultSetAudioItem.getNewAbsolutePath()));
                    if (musicMetadataSet == null) {
                        resultSetAudioItem.setStatus(AudioItem.FILE_STATUS_INCOMPLETE);
                        resultSetAudioItem.setSelected(false);
                        contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_BAD);
                        SelectFolderActivity.this.dbHelper.setStatus(resultSetAudioItem.getId(), contentValues);
                    } else {
                        iMusicMetadata = (MusicMetadata) musicMetadataSet.getSimplified();

                        if(dataTitle) {
                            currentFile = new File(resultSetAudioItem.getNewAbsolutePath());

                            if(SelectedOptions.AUTOMATIC_CHANGE_FILENAME) {
                                String newPath = currentFile.getParent();
                                String newFilename = newName + ".mp3";
                                String newCompleteFilename= newPath + "/" + newFilename;
                                renamedFile = new File(newCompleteFilename);
                                if(!renamedFile.exists()) {
                                    Log.d("No Existe","file");
                                    currentFile.renameTo(renamedFile);
                                }else {
                                    newFilename = newName +"("+ (int)Math.floor((Math.random()*10)+ 1) +")"+".mp3";
                                    newCompleteFilename = newPath + "/" + newFilename;
                                    renamedFile = new File(newCompleteFilename);
                                    Log.d("Ya Existe","file");
                                    currentFile.renameTo(renamedFile);
                                }

                                contentValues.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME,newFilename);
                                contentValues.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH,newCompleteFilename);
                                resultSetAudioItem.setFileName(newFilename);
                                resultSetAudioItem.setNewAbsolutePath(newCompleteFilename);
                                Log.d("NEW_PATH",newCompleteFilename);
                            }
                            else {
                                renamedFile = currentFile;
                            }

                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_TITLE,newName);


                            resultSetAudioItem.setTitle(newName);
                            iMusicMetadata.setSongTitle(newName);
                        }

                        if (dataArtist) {
                            iMusicMetadata.setArtist(artistName);
                            resultSetAudioItem.setArtist(artistName);
                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_ARTIST,artistName);
                        }

                        if (dataAlbum) {
                            iMusicMetadata.setAlbum(albumName);
                            resultSetAudioItem.setAlbum(albumName);
                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_ALBUM,albumName);
                        }

                        if (dataImage) {
                            byte[] imgData = new GnAssetFetch(gnUser, imageUrl).data();
                            Vector<ImageData> imageDataVector = new Vector<>();
                            ImageData imageData = new ImageData(imgData,"","",3);
                            imageDataVector.add(imageData);
                            iMusicMetadata.setPictureList(imageDataVector);
                        }

                        if(dataTrackNumber){
                            iMusicMetadata.setTrackNumber(Integer.parseInt(trackNumber));
                        }

                        if(dataYear){
                            iMusicMetadata.setYear(year);
                        }

                        if(dataGenre){
                            iMusicMetadata.setGenre(genre);
                        }


                        if(!dataTitle || !dataArtist || !dataAlbum || !dataImage || !dataTrackNumber || !dataYear || !dataGenre){
                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_INCOMPLETE);
                            resultSetAudioItem.setStatus(AudioItem.FILE_STATUS_INCOMPLETE);
                        }
                        else{
                            contentValues.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_OK);
                            resultSetAudioItem.setStatus(AudioItem.FILE_STATUS_OK);
                        }

                        myID3.update(renamedFile, musicMetadataSet, iMusicMetadata);
                        SelectFolderActivity.this.dbHelper.updateData(resultSetAudioItem.getId(), contentValues);
                    }
                }
                catch (IOException | GnException | ID3WriteException e) {
                    e.printStackTrace();
                }

            }

            contentValues.clear();
            contentValues = null;
            resultSetAudioItem.setProcessing(false);
            resultSetAudioItem.setSelected(false);
            audioItemArrayAdapterAdapter.notifyDataSetChanged();
            selectedTracksList.remove((int)resultSetAudioItem.getId());

        }

        @Override
        protected void onCancelled(Void voids){
            super.onCancelled(voids);
            currentAudioItem.setProcessing(false);
            audioItemArrayAdapterAdapter.notifyDataSetChanged();
            finishTaskByUser(true);
            Log.d(AsyncProcessTrack.class.getName(), "ONCANCELLED");
        }

        private void startTask(){
            SelectFolderActivity.this.fab.setOnClickListener(null);
            SelectFolderActivity.this.fab.setImageDrawable(getDrawable(R.drawable.ic_stop_white_24dp));
            SelectFolderActivity.this.fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(SelectFolderActivity.this);
                    builder.setTitle("Cancelando...").setMessage("¿Desea cancelar la corrección en curso?")
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AsyncProcessTrack.this.cancel(true);
                                }
                            });
                    final AlertDialog dialog =  builder.create();
                    dialog.setCancelable(false);
                    dialog.show();
                }
            });

            if(mediaPlayer != null && mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        }

        private void finishTaskByUser(boolean userCanceled){
            String msg = "";

            if(userCanceled){
                msg = "La tarea fue cancelada por el usuario";
            }
            else {
                msg = "Tarea terminada exitosamente";
            }
            SelectFolderActivity.this.searchView.setEnabled(true);
            SelectFolderActivity.this.fab.setOnClickListener(null);
            SelectFolderActivity.this.fab.setImageDrawable(getDrawable(R.drawable.ic_check_white_24dp));
            SelectFolderActivity.this.fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(mediaPlayer != null && mediaPlayer.isPlaying()){
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }

                    if(!selectedTracksList.isEmpty()) {
                        if(!apiInitialized){
                            snackbar.setText("La API de reconocimiento se esta inicializando, espere unos instantes.");
                            snackbar.show();
                            return;
                        }
                        snackbar.setText("Procesando... espere por favor.");
                        snackbar.show();

                        AsyncProcessTrack asyncProcessTrack = new AsyncProcessTrack();
                        asyncProcessTrack.execute();
                    }
                    else {
                        snackbar.setText("No hay ninguna canción seleccionada para corregir");
                        snackbar.show();
                    }
                }
            });

            ListView listView= (ListView) findViewById(R.id.list_view_songs);
            listView.invalidateViews();
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            System.gc();
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
                    Log.d("FILE STATUS EVENT",status);
                    final String finalStatus = status;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SelectFolderActivity.snackbar.setText(finalStatus);
                            SelectFolderActivity.snackbar.show();
                        }
                    });


                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(appString, "error in retrieving musidIdFileStatus");
            }
        }

        @Override
        public void gatherFingerprint(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            //Callback to inform that fingerprint was retrieved from audiotgrack.

        }

        /**
         * Retreieve metadata from file, not invoking if is provided by the developer
         * @param gnMusicIdFileInfo
         * @param l
         * @param l1
         * @param iGnCancellable
         */
        @Override
        public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            // Skipping this here as metadata has been previously loaded for all files
            // You could provide metadata "just in time" instead of before invoking Track/Album/Library ID, which
            // means you would add it in this delegate method for the file represented by fileInfo
        }

        @Override
        public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {

        }

        @Override
        public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) {

            /*try {
                //Log.d("MATCHES_TITLE", gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().tracksMatched().at(0).next().title().display());
                String title = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().tracksMatched().at(0).next().title().display();
                String artistName = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().artist().name().display();// .albums().at(0).next().artist().name().display();
                String albumName = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().title().display(); //dataMatchResponse().dataMatches().at(0).next().getAsAlbum().title().display();
                String imageUrl = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().coverArt().asset(GnImageSize.kImageSizeMedium).url(); //albumResponse().albums().getIterator().next().coverArt().asset(GnImageSize.kImageSizeMedium).url();
                String trackNumber = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().trackMatched().trackNumber(); //albumResponse().albums().getIterator().next().trackMatched().trackNumber();
                String year = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().year(); //albumResponse().albums().getIterator().next().year();
                String genre = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().trackMatched().genre(GnDataLevel.kDataLevel_1); //albumResponse().albums().getIterator().next().trackMatched().genre(GnDataLevel.kDataLevel_1);

                Log.d("TITLE_BG", title);
                Log.d("ARTIST_BG", artistName);
                Log.d("ALBUM_BG",albumName);
                Log.d("ALBUM_ART_BG", imageUrl);
                Log.d("NUMBER_BG", trackNumber);
                Log.d("YEAR_BG", year);
                Log.d("GENRE_BG", genre);
            } catch (GnException e) {
                e.printStackTrace();
            }*/

        }

        @Override
        public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            // no match found for the audio file represented by fileInfo
            try {
                Log.i(appString,"GnMusicIdFile no match found for " + gnMusicIdFileInfo.identifier());
            } catch (GnException e) {
                this.cancel(true);
            }
        }

        @Override
        public void musicIdFileComplete(GnError gnError) {
            if ( gnError.errorCode() == 0 ){
                Log.d("musicIdFileComplete", "SUCCESS");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SelectFolderActivity.snackbar.setText("Completado");
                        SelectFolderActivity.snackbar.show();
                    }
                });

            }
            else {

                if ( gnError.isCancelled() ) {
                    Log.d("musicIdFileComplete", "CANCELLED");
                    this.cancel(true);
                }
                else {
                    //setStatus(gnError.errorDescription(), true);
                    //this.cancel(true);
                    Log.e(appString, gnError.errorAPI() + ": " + gnError.errorDescription() );
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SelectFolderActivity.snackbar.setText("Cancelado o error");
                        SelectFolderActivity.snackbar.show();
                    }
                });



            }
            Log.d("musicIdFileComplete", "READY");
        }

        @Override
        public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {

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
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        //Log.d("onSaveInstanceState","onSaved");
        savedInstanceState.putBoolean("KEEP_LIST",true);


    }

    @Override
    public void onStop() {
        super.onStop();
        //Log.d("onStop","onStop");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();

        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

}
