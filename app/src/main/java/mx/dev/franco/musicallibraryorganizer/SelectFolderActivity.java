package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;


import static mx.dev.franco.musicallibraryorganizer.SplashActivity.sharedPreferences;

public class SelectFolderActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    protected ProgressBar progressBar;
    private ArrayList<File> fileList = new ArrayList<File>();
    private ArrayList<File> files = new ArrayList<File>();
    private ArrayList<File> arrayListFiles;
    private int requestCode;
    private LinearLayout view;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_folder);
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
            TypeScanDialogFragment typeScanDialog = new TypeScanDialogFragment();
            typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");
            typeScanDialog.setCancelable(false);
        //}

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Aun en construcción", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        view = (LinearLayout) findViewById(R.id.list_of_files);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        System.out.println("click aqui "+id);
        if (id == R.id.preferences) {
            // Handle the camera action
        } else if (id == R.id.donate) {

        } else if (id == R.id.rate) {

        } else if (id == R.id.share) {

        } else if (id == R.id.scan){

            TypeScanDialogFragment typeScanDialog = new TypeScanDialogFragment();
            typeScanDialog.show(getFragmentManager(), "TypeScanDialogFragment");
            typeScanDialog.setCancelable(false);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    public void showFoldersFromSystem(int scanType){
//getting SDcard root path

        File root = new File(Environment.getExternalStorageDirectory().getPath());
        arrayListFiles = getFile(root,scanType);

        for (int i = 0; i < arrayListFiles.size(); i++) {
            TextView textView = new TextView(this);
            textView.setText(arrayListFiles.get(i).getName());
            textView.setPadding(5, 5, 5, 5);



            //if (files.get(i).isDirectory()) {
                //files.remove(i);
                //textView.setTextColor(Color.parseColor("#FF0000"));
           // }
            view.addView(textView);
        }

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
}
