package mx.dev.franco.automusictagfixer.UI.sd_card_instructions;

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

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
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
                AndroidUtils.grantPermissionSD(getApplicationContext(), resultData);
                mSuccess = true;
            }
        }
        else {
            mSuccess = false;
        }
        finish();
    }

    @Override
    public void onDestroy(){
        Toast toast = AndroidUtils.getToast(this);
        toast.setDuration(Toast.LENGTH_SHORT);

        String msg;
        if(mSuccess){
            msg = getString(R.string.permission_granted);
        }
        else {
            msg = getString(R.string.permission_granted_fail);
        }
        toast.setText(msg);
        toast.show();

        super.onDestroy();
    }
}
