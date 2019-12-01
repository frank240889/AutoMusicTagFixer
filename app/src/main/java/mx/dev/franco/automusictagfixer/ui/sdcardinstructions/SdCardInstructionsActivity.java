package mx.dev.franco.automusictagfixer.ui.sdcardinstructions;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions;

public class SdCardInstructionsActivity extends AppCompatActivity {
    private boolean mSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sd_card_instructions);
        MaterialButton button = findViewById(R.id.request_permission_button);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, RequiredPermissions.REQUEST_PERMISSION_SAF);
        });
        MaterialToolbar toolbar = findViewById(R.id.sd_card_instructions_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.sd_card_detected);
        actionBar.setDefaultDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            boolean res = AndroidUtils.grantPermissionSD(getApplicationContext(), resultData);
            mSuccess = res;
            setResult(Activity.RESULT_OK);
        } else {
            mSuccess = false;
            setResult(Activity.RESULT_CANCELED);
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
