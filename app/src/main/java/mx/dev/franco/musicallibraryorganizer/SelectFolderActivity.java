package mx.dev.franco.musicallibraryorganizer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.util.ArrayList;

import static mx.dev.franco.musicallibraryorganizer.SplashActivity.sharedPreferences;

public class SelectFolderActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected static ArrayList<Integer> selectedTracks = new ArrayList<Integer>();
    protected int scanRequestType;
    protected ProgressBar progressBar;
    private ArrayList<File> fileList = new ArrayList<File>();
    private ArrayList<File> files = new ArrayList<File>();
    protected ArrayAdapter<File> filesAdapter;
    private ArrayList<File> arrayListFiles;
    private int requestCode;
    private TypeScanDialogFragment typeScanDialog;
    public LinearLayout view;
    private TrackAdapter adapter;
    protected ListView listSongs;

    protected boolean isPlaying = false;
    protected String currentTrackName = "";
    protected String lastTrackName = "";
    protected ImageButton lastButton, currentButton;
    protected static String activeTrack = "";
    protected MediaPlayer mediaPlayer;
    protected String m_chosenDir = "";
    protected boolean m_newFolderEnabled = true;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_folder);
        view = (LinearLayout) findViewById(R.id.list_of_files);
        /*view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(getApplicationContext(), "TOUCH", Toast.LENGTH_SHORT).show();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    return true;
                }
                return false;
            }
        });*/
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.searching);
        progressBar.setVisibility(View.GONE);


        //Verificamos que ya se han concedido permisos, de los contrario, aparecera el Dialogo que nos pregunta el tipo de busqueda de musica,
        //el cual nos va a pedir el permiso de acceos a los archivos
        SplashActivity.sharedPreferences = getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
        boolean grantedAccessFiles = sharedPreferences.getBoolean("accessFilesPermission", false);
        System.out.println(grantedAccessFiles);

        //Si ya teniamos concedido el permiso de acceso archivos ya no preguntamos al inicio de la app el tipo de escaneo
        //if(!grantedAccessFiles) {
        typeScanDialog = new TypeScanDialogFragment();
        typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");
        typeScanDialog.setCancelable(false);
        //}

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "EN DESARROLLO...", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().findItem(R.id.scan).setChecked(true);

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
        if (id == R.id.preferences) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.donate) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.rate) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.share) {
            Toast.makeText(this, "En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.scan) {

            try{
                filesAdapter.clear();
                ListView listView= (ListView) findViewById(R.id.list_view_songs);
                listView.invalidateViews();
                files.clear();
                fileList.clear();
                selectedTracks.clear();
            }catch (Exception e){

            }
            System.out.println("Items removidos");
            typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");
            typeScanDialog.setCancelable(false);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    protected void onClickPlayImageButton(View view){
        int elId = getResources().getIdentifier("android:drawable/ic_media_pause",null,null);
        Log.d("EL TAG", String.format("%d", elId));
        if(isPlaying){
            currentButton = (ImageButton)view;
            mediaPlayer.stop();
            isPlaying = false;
            if(lastTrackName.equals(currentButton.getTag().toString())){
                activeTrack = "";
                currentButton.setImageResource(getResources().getIdentifier("android:drawable/ic_media_play", null, null));
                View currentParentView = (View)currentButton.getParent().getParent();
                currentParentView.setBackgroundColor(Color.TRANSPARENT);
             }
            else{
                lastButton.setImageResource(getResources().getIdentifier("android:drawable/ic_media_play", null, null));
                View lastParentView = (View)lastButton.getParent().getParent();
                lastParentView.setBackgroundColor(Color.TRANSPARENT);
                currentButton.setImageResource(getResources().getIdentifier("android:drawable/ic_media_pause", null, null));
                View currentParentView = (View)currentButton.getParent().getParent();
                currentParentView.setBackgroundColor(Color.parseColor("#33b5e5"));
                lastButton = currentButton;
                currentTrackName = currentButton.getTag().toString();
                lastTrackName = currentTrackName;
                activeTrack = lastTrackName;
                mediaPlayer = MediaPlayer.create(getApplicationContext(),Uri.parse(lastTrackName));
                mediaPlayer.start();
                isPlaying = true;
            }
        }else{
            currentButton = (ImageButton)view;
            lastButton = currentButton;
            currentTrackName = currentButton.getTag().toString();
            lastTrackName = currentTrackName;
            activeTrack = currentTrackName;
            mediaPlayer = MediaPlayer.create(getApplicationContext(),Uri.parse(currentTrackName));
            mediaPlayer.start();
            isPlaying = true;
            currentButton.setImageResource(getResources().getIdentifier("android:drawable/ic_media_pause", null, null));
            View parentView = (View)currentButton.getParent().getParent();
            parentView.setBackgroundColor(Color.parseColor("#33b5e5"));
        }
    }

    protected void executeScan(View view) {
        if (view.getId() == R.id.autoScanRadio) {
            scanRequestType = 1;
        } else if (view.getId() == R.id.manualScanRadio) {
            scanRequestType = 2;
        }

        typeScanDialog.getDialog().cancel();
        progressBar.setVisibility(View.VISIBLE);
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, scanRequestType);
    }

    protected void askForPermission(String permission, Integer requestCode) {
        //Sino tenemos el permiso lo pedimos
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
        else {
        //Si ya tenemos el permiso, verificamos que tipo de escaneo se hara, en caso que se requiera hacer un escaneo
            if (requestCode == 1) {
                Toast.makeText(this, "Buscando música", Toast.LENGTH_LONG).show();
                //Permiso concedido, obtenemos las carpeta del sistema en busca de arhivos mp3 y el arreglo lo pasamos al adapter.
                final ListView listView = (ListView) findViewById(R.id.list_view_songs);
                filesAdapter = new TrackAdapter(this,new ArrayList<File>());
                listView.setAdapter(filesAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?>adapter, View view, int position, long arg){

                        if(!selectedTracks.contains(position)) {
                            selectedTracks.add(position);
                            //listView.getChildAt(position).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

                        }else {
                            selectedTracks.remove(selectedTracks.indexOf(position));
                            //listView.getChildAt(position).setBackgroundColor(getResources().getColor(R.color.common_google_signin_btn_text_dark_default));
                        }

                    }
                } );
                AsyncReadFile asyncReadFile = new AsyncReadFile(requestCode,new File(Environment.getExternalStorageDirectory().getPath()));
                asyncReadFile.execute();
            } else {
                Toast.makeText(this, "Selecciona las carpetas que contengan música", Toast.LENGTH_LONG).show();
                DirectoryChooserDialog directoryChooserDialog =
                        new DirectoryChooserDialog(getApplicationContext(),new DirectoryChooserDialog.ChosenDirectoryListener()
                                {
                                    @Override
                                    public void onChosenDir(String chosenDir)
                                    {
                                        m_chosenDir = chosenDir;
                                        Toast.makeText(getApplicationContext(), "Chosen directory: " +
                                                        chosenDir, Toast.LENGTH_LONG).show();
                                    }
                                });
                // Toggle new folder button enabling
                directoryChooserDialog.setNewFolderEnabled(m_newFolderEnabled);
                // Load directory chooser dialog for initial 'm_chosenDir' directory.
                // The registered callback will be called upon final directory selection.
                directoryChooserDialog.chooseDirectory(m_chosenDir);
                m_newFolderEnabled = ! m_newFolderEnabled;
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        this.requestCode = requestCode;
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Guardamos el permiso concedido para no volver a solicitarlo
            SplashActivity.editor = sharedPreferences.edit();
            SplashActivity.editor.putBoolean("accessFilesPermission", true);
            SplashActivity.editor.apply();
            if (requestCode == 1) {
                Toast.makeText(this, "Permiso concedido... Buscando musica", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permiso concedido... Selecciona las carpetas que contengan musica", Toast.LENGTH_LONG).show();
            }

            //Permiso concedido, obtenemos las carpeta del sistema en busca de arhivos mp3 y el arreglo lo pasamos al adapter.

            final ListView listView = (ListView) findViewById(R.id.list_view_songs);
            filesAdapter = new TrackAdapter(this,new ArrayList<File>());
            listView.setAdapter(filesAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?>adapter, View view, int position, long arg){
                    final int debug = Log.v("Debug", listView.getSelectedItem() + "");

                    if(!selectedTracks.contains(position)) {
                        //listView.getChildAt(position).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark,null));
                        selectedTracks.add(position);

                    }else {
                        selectedTracks.remove(selectedTracks.indexOf(position));
                        //listView.getChildAt(position).setBackgroundColor(getResources().getColor(R.color.common_google_signin_btn_text_dark_default,null));
                    }
                }
            } );
            AsyncReadFile asyncReadFile = new AsyncReadFile(requestCode,new File(Environment.getExternalStorageDirectory().getPath()));
            asyncReadFile.execute();

        }
        else {

            ListView listView = (ListView) findViewById(R.id.list_view_songs);
            filesAdapter = new TrackAdapter(this,new ArrayList<File>());
            listView.setAdapter(filesAdapter);
            AsyncReadFile asyncReadFile = new AsyncReadFile(requestCode,new File(Environment.getExternalStorageDirectory().getPath()));
            asyncReadFile.execute();
            Toast.makeText(this,"sin permiso",Toast.LENGTH_SHORT).show();
            //Sin permisos
        }

    }


    private class AsyncReadFile extends AsyncTask<Void, File, Void> {
        private int code;
        private File path;

        AsyncReadFile(int code,File path){
            this.code = code;
            this.path = path;
        }

        protected ArrayList<File> getFile(File dir) {
            File listFile[] = dir.listFiles();
            if (listFile != null && listFile.length > 0) {
                for (int i = 0; i < listFile.length; i++) {

                    if (listFile[i].isDirectory()) {
                        fileList.add(listFile[i]);
                        getFile(listFile[i]);

                    } else {
                        if (listFile[i].getName().endsWith(".mp3")){
                            files.add(listFile[i]);
                            //publishProgress(listFile[i]);
                            //System.out.println(fileList.get(i).getName());
                        }
                    }

                }
            }
            return this.code == 1 ? files:fileList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<File> arrayListFiles = getFile(this.path);
            for (int i = 0; i < arrayListFiles.size(); i++) {
                publishProgress(arrayListFiles.get(i));
            }
            System.out.println("Numero de elemntos en arrayListFiles "+arrayListFiles.size());
            return null;
        }


        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void result) {
            progressBar.setVisibility(View.GONE);
            System.out.println("Numero de elementos en adapter "+filesAdapter.getCount());
        }
        @Override
        protected void onProgressUpdate(File... progress) {
            super.onProgressUpdate(progress);
            filesAdapter.add(progress[0]);
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
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

}
