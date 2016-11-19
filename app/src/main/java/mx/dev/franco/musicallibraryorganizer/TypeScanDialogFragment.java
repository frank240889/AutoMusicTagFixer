package mx.dev.franco.musicallibraryorganizer;

import android.Manifest;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;

import static mx.dev.franco.musicallibraryorganizer.SplashActivity.sharedPreferences;

/**
 * Created by franco on 12/11/16.
 */

public class TypeScanDialogFragment extends DialogFragment {
    protected int scanRequestType;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SplashActivity.sharedPreferences = getActivity().getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
        // Se usa la clase Builder para la construccion del dialogo
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View formElementsView = inflater.inflate(R.layout.scan_dialog_elements, null, false);
        final RadioGroup scanTypeRadioGroup = (RadioGroup) formElementsView.findViewById(R.id.scanTypeRadioGroup);


        builder.setView(formElementsView).setMessage(R.string.title_scantype_dialog);
        scanTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id){

                if(id == R.id.autoScanRadio) {
                    scanRequestType = 1;
                } else if(id == R.id.manualScanRadio) {
                    scanRequestType = 2;
                }
                new CountDownTimer(1000, 1000) {
                    public void onTick(long millisUntilFinished) {}

                    public void onFinish() {
                        getDialog().cancel();
                        ((SelectFolderActivity)getActivity()).progressBar.setVisibility(View.VISIBLE);
                        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,scanRequestType);

                    }
                }.start();

            }
        });

        // Se crea el Dialog y se retorna
        return builder.create();
    }


    protected void askForPermission(String permission, Integer requestCode) {

        if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);
        } else {

            if(requestCode == 1){
                Toast.makeText(getActivity(), "Permiso ya habia sido concedido... Buscando musica", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Permiso ya habia sido concedido... Selecciona las carpetas que contengan musica", Toast.LENGTH_LONG).show();
                //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                //startActivityForResult(intent, 1);
            }
            ((SelectFolderActivity)getActivity()).showFoldersFromSystem(requestCode);
        }
    }


}
