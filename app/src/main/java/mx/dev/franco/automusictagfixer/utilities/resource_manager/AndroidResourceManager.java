package mx.dev.franco.automusictagfixer.utilities.resource_manager;

import android.content.Context;

/**
 * Created by Franco Castillo on 12/04/2018.
 * Class to wrap R class and only pass
 * the required string, and avoid inject the
 * context in every case. This component
 * is managed by dagger.
 */

public class AndroidResourceManager extends ResourceManager{
    //The context, needed for access android resources.
    private Context mContext;
    public AndroidResourceManager(Context context) {
        mContext = context;
    }

    /**
     * Retrieves the corresponding value of the id
     * or string passed.
     * @param resources The resource, this must be a
     *                  integer, for that reason if is
     *                  a string is parsed to integer.
     * @return The string corresponding to the resource.
     */
    @Override
    public String getString(Object resources) {
        int code;
        if (resources instanceof Throwable) {
            return getStringResource((Throwable) resources);
        }
        else {
            if (resources instanceof String && !resources.equals("")) {
                code = Integer.parseInt((String) resources);
            } else {
                code = (int) resources;
            }

            return getStringResource(code);

        }
    }

    /**
     * Retrieves the string corresponding to the resources.
     * @param resource The required resource.
     * @return The string corresponding to the resource.
     */
    private String getStringResource(int resource){
            return mContext.getString(resource);
    }

    private String getStringResource(Throwable t){
        return null;
    }
}
