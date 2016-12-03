package mx.dev.franco.musicallibraryorganizer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import static mx.dev.franco.musicallibraryorganizer.SplashActivity.sharedPreferences;

public class SelectFolderActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    protected int scanRequestType;
    protected ProgressBar progressBar;
    private ArrayList<File> fileList = new ArrayList<File>();
    private ArrayList<File> files = new ArrayList<File>();
    private ArrayList<File> arrayListFiles;
    private int requestCode;
    private TypeScanDialogFragment typeScanDialog;
    public LinearLayout view;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_folder);
        view = (LinearLayout) findViewById(R.id.list_of_files);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(getApplicationContext(),"TOUCH",Toast.LENGTH_SHORT).show();
                if(event.getAction() == MotionEvent.ACTION_DOWN) {

                    return true;
                }
                return false;
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar)findViewById(R.id.searching);
        progressBar.setVisibility(View.GONE);


        //Verificamos que ya se han concedido permisos, de los contrario, aparecera el Dialogo que nos pregunta el tipo de busqueda de musica,
        //el cual nos va a pedir el permiso de acceos a los archivos
        SplashActivity.sharedPreferences = getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
        boolean grantedAccessFiles = sharedPreferences.getBoolean("accessFilesPermission",false);
        System.out.println(grantedAccessFiles);
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
        System.out.println("click aqui "+id);
        if (id == R.id.preferences) {
            Toast.makeText(this,"En desarrollo",Toast.LENGTH_SHORT).show();
        } else if (id == R.id.donate) {
            Toast.makeText(this,"En desarrollo",Toast.LENGTH_SHORT).show();
        } else if (id == R.id.rate) {
            Toast.makeText(this,"En desarrollo",Toast.LENGTH_SHORT).show();
        } else if (id == R.id.share) {
            Toast.makeText(this,"En desarrollo",Toast.LENGTH_SHORT).show();
        } else if (id == R.id.scan){
            view.removeAllViews();
            System.out.println("WIdgets removidos");
            typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");
            typeScanDialog.setCancelable(false);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    public void showFoldersFromSystem(int scanType){
//Obtenemos el path de la SD card
    AsyncSearch asyncSearch = new AsyncSearch(scanType, this, view);
        asyncSearch.execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        this.requestCode = requestCode;
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SplashActivity.editor = sharedPreferences.edit();
                    SplashActivity.editor.putBoolean("accessFilesPermission",true);
                    SplashActivity.editor.apply();
                    if(requestCode == 1){
                        Toast.makeText(this, "Permiso concedido... Buscando musica", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Permiso concedido... Selecciona las carpetas que contengan musica", Toast.LENGTH_LONG).show();
                        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        //startActivityForResult(intent, 1);

                    }

                    showFoldersFromSystem(requestCode);

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {


                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;


    }

    protected void executeScan(View view){
        if(view.getId() == R.id.autoScanRadio) {
            scanRequestType = 1;
        } else if(view.getId() == R.id.manualScanRadio) {
            scanRequestType = 2;
        }

        typeScanDialog.getDialog().cancel();
        progressBar.setVisibility(View.VISIBLE);
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,scanRequestType);



    }

    protected void askForPermission(String permission, Integer requestCode) {

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        } else {

            if(requestCode == 1){
                Toast.makeText(this, "Buscando música", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Selecciona las carpetas que contengan música", Toast.LENGTH_LONG).show();
                //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                //startActivityForResult(intent, 1);
            }
            showFoldersFromSystem(requestCode);
        }
    }

    private class AsyncSearch extends AsyncTask <Void, Integer, Void>{
        private AppCompatActivity activity;
        private int scanCode;
        private LinearLayout view;

        public AsyncSearch(int scanCode, AppCompatActivity activity, LinearLayout view){
            this.scanCode = scanCode;
            this.activity = activity;
            this.view = view;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File root = new File(Environment.getExternalStorageDirectory().getPath());
            arrayListFiles = getFile(root,this.scanCode);
            for (int i = 0; i < arrayListFiles.size(); i++) {
                publishProgress(i);


                //if (files.get(i).isDirectory()) {
                //files.remove(i);
                //textView.setTextColor(Color.parseColor("#FF0000"));
                // }

            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        public ArrayList<File> getFile(File dir, int scanType) {
            File listFile[] = dir.listFiles();
            if (listFile != null && listFile.length > 0) {
                for (int i = 0; i < listFile.length; i++) {

                    if (listFile[i].isDirectory()) {
                        fileList.add(listFile[i]);
                        getFile(listFile[i], scanType);

                    } else {
                        if (listFile[i].getName().endsWith(".mp3")){
                            files.add(listFile[i]);
                            //System.out.println(fileList.get(i).getName());
                        }
                    }

                }
            }
            return (scanType == 1) ? files:fileList;
        }
        @Override
        protected void onPostExecute(Void result) {
            progressBar.setVisibility(View.GONE);
        }
        @Override
        protected void onProgressUpdate(Integer... progress) {
            System.out.println(progress[0] + "--- " + arrayListFiles.get(progress[0]).getName() + "---" + arrayListFiles.get(progress[0]).lastModified());
            super.onProgressUpdate(progress);
            CheckBox checkbox = new CheckBox(activity);
            checkbox.setId(progress[0]);
            checkbox.setText(arrayListFiles.get(progress[0]).getName());
            checkbox.setPadding(5,5,5,5);
            checkbox.setTextSize(20);

            view.addView(checkbox);
            progressBar.setProgress(progress[0]);
        }
    }
}
