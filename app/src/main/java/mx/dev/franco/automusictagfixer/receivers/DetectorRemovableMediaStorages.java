package mx.dev.franco.automusictagfixer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;

/**
 * Created by franco on 12/01/18.
 */

public final class DetectorRemovableMediaStorages extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("media","works");
        String action = intent.getAction();
        Toast toast = AndroidUtils.getToast(context);

        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            toast.setText(context.getString(R.string.media_mounted));
        }
        else if(action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            toast.setText(context.getString(R.string.media_unmounted));
            AndroidUtils.revokePermissionSD(context.getApplicationContext());
        }

        toast.show();

        //Reload number of storage available
        StorageHelper.getInstance(context).getBasePaths().clear();
        StorageHelper.getInstance(context).detectStorages();
    }
}
