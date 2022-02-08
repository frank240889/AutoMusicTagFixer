package mx.dev.franco.automusictagfixer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.fixer.StorageHelper
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils

/**
 * Created by franco on 12/01/18.
 */
class DetectorRemovableMediaStorage : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val toast = AndroidUtils.getToast(context)

        if (Intent.ACTION_MEDIA_MOUNTED == action) {
            toast.setText(context.getString(R.string.media_mounted))
        }
        else if (Intent.ACTION_MEDIA_UNMOUNTED == action) {
            AndroidUtils.revokePermissionSD(context)
            toast.setText(context.getString(R.string.media_unmounted))
            AndroidUtils.revokePermissionSD(context.applicationContext)
        }

        toast.show()

        StorageHelper.getInstance(context).basePaths.clear()
        StorageHelper.getInstance(context).detectStorage()
    }
}