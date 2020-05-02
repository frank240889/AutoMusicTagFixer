package mx.dev.franco.automusictagfixer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

/**
 * Created by franco on 12/01/18.
 */

public final class DetectorRemovableMediaStorage extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Toast toast = AndroidUtils.getToast(context);

        if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            toast.setText(context.getString(R.string.media_mounted));
        }
        else if(Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
            AndroidUtils.revokePermissionSD(context);
            toast.setText(context.getString(R.string.media_unmounted));
            AndroidUtils.revokePermissionSD(context.getApplicationContext());
        }

        if (ServiceUtils.getInstance(context).checkIfServiceIsRunning(FixerTrackService.CLASS_NAME)) {
            Intent stopIntent = new Intent(context, FixerTrackService.class);
            stopIntent.setAction(action);
            context.startService(stopIntent);
        }
        toast.show();
        AudioTagger.StorageHelper.getInstance(context).getBasePaths().clear();
        AudioTagger.StorageHelper.getInstance(context).detectStorage();
    }
}
