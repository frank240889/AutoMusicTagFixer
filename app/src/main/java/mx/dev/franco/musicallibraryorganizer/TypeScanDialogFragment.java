package mx.dev.franco.musicallibraryorganizer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by franco on 12/11/16.
 */

public class TypeScanDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SplashActivity.sharedPreferences = getActivity().getSharedPreferences("ShaPreferences", Context.MODE_PRIVATE);
        // Se usa la clase Builder para la construccion del dialogo
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View formElementsView = inflater.inflate(R.layout.scan_dialog_elements, null, false);
        //final RadioGroup scanTypeRadioGroup = (RadioGroup) formElementsView.findViewById(R.id.scanTypeRadioGroup);
        final Button autoButton = (Button)formElementsView.findViewById(R.id.autoScanRadio);
        final Button manualButton = (Button)formElementsView.findViewById(R.id.manualScanRadio);
        builder.setTitle("Exploración de carpetas.");
        builder.setMessage("Selecciona automatica si quieres que la aplicación busque los archivos de audio por ti o manual para que tu elijas las carpetas. Se requiere el permiso Espacio de almacenamiento.");
        builder.setView(formElementsView);

        // Se crea el Dialog y se retorna
        return builder.create();
    }



}
