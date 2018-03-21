package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.list.AudioItem;

/**
 * Created by franco on 19/03/18.
 */

public class GnErrorCodes {
    public static String getMessage(Context context, long errorCode, String... params){
        String msg = null;
        switch ((int) errorCode){
            case 0x90820042:
            case 0x90b30042:
                msg = String.format(context.getString(R.string.file_already_added), AudioItem.getFilename(params[0]));
                break;

        }
        context = null;
        return msg;
    }
}
