package mx.dev.franco.automusictagfixer.ui.sdcardinstructions;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;

public class SdCardInstructionsActivity extends AppCompatActivity {
    private boolean mSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sd_card_instructions);
        Button button = findViewById(R.id.request_permission_button);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, RequiredPermissions.REQUEST_PERMISSION_SAF);
        });

        ImageButton closeInstructions = findViewById(R.id.close_instructions);
        closeInstructions.setOnClickListener(v -> finish());

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            boolean res = AndroidUtils.grantPermissionSD(getApplicationContext(), resultData);
            if (res) {
                mSuccess = true;
            }
            else {
                mSuccess = false;
            }
        }
        else {
            mSuccess = false;
        }

        finish();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Toast toast = AndroidUtils.getToast(this);
        toast.setDuration(Toast.LENGTH_LONG);

        String msg;
        if(mSuccess){
            msg = getString(R.string.permission_granted);
        }
        else {
            msg = getString(R.string.permission_granted_fail);
        }
        toast.setText(msg);
        toast.show();
    }
}
