package mx.dev.franco.automusictagfixer.utilities.resource_manager

import android.content.Context

/**
 * Created by Franco Castillo on 12/04/2018.
 * Class to wrap R class and only pass
 * the required string, and avoid inject the
 * context in every case. This component
 * is managed by dagger.
 */
class AndroidResourceManager(  //The context, needed for access android resources.
    private val mContext: Context
) : ResourceManager() {
    /**
     * Retrieves the corresponding value of the id
     * or string passed.
     * @param obj The resource, this must be a
     * integer, for that reason if is
     * a string is parsed to integer.
     * @return The string corresponding to the resource.
     */
    override fun getString(obj: Any?): String? {
        val code: Int
        return if (obj is Throwable) {
            getStringResource(obj)!!
        } else {
            if (obj is String && obj != "") {
                code = obj.toInt()
            } else {
                code = obj as Int
            }
            getStringResource(code)
        }
    }

    /**
     * Retrieves the string corresponding to the resources.
     * @param resource The required resource.
     * @return The string corresponding to the resource.
     */
    private fun getStringResource(resource: Int): String {
        return mContext.getString(resource)
    }

    private fun getStringResource(t: Throwable): String? {
        return null
    }
}