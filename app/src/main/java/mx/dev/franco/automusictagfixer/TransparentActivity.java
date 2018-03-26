package mx.dev.franco.automusictagfixer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;
import mx.dev.franco.automusictagfixer.utilities.Settings;

public class TransparentActivity extends AppCompatActivity {
    private boolean mSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent);
        Button button = findViewById(R.id.request_permission_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, RequiredPermissions.REQUEST_PERMISSION_SAF);
            }
        });

        ImageButton closeInstructions = findViewById(R.id.close_instructions);
        closeInstructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            if (resultData != null) {
                //Save root Uri of SD card
                Constants.URI_SD_CARD = resultData.getData();

                // Persist access permissions.
                // Persist access permissions.
                final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(Constants.URI_SD_CARD, takeFlags);

                SharedPreferences sharedPreferences = getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(Constants.URI_TREE, Constants.URI_SD_CARD.toString());
                editor.apply();
                mSuccess = true;
                Settings.ENABLE_SD_CARD_ACCESS = true;
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor e  = preferences.edit();
                e.putBoolean("key_enable_sd_card_access",true);
                e.commit();
            }
        }
        else {
            mSuccess = false;
        }
        finish();
    }

    @Override
    public void onDestroy(){
        Toast toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);
        View view = toast.getView();
        TextView text = view.findViewById(android.R.id.message);
        text.setTextColor(ContextCompat.getColor(this.getApplicationContext(), R.color.grey_900));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            text.setTextAppearance(R.style.CustomToast);
        } else {
            text.setTextAppearance(this.getApplicationContext(), R.style.CustomToast);
        }
        view.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_custom_toast));
        toast.setGravity(Gravity.CENTER, 0, 0);

        String msg;
        if(mSuccess){
            msg = getString(R.string.permission_granted);
            toast.setDuration(Toast.LENGTH_SHORT);
        }
        else {
            msg = getString(R.string.permission_granted_fail);
        }

        text.setText(msg);
        toast.show();

        super.onDestroy();
    }
}
