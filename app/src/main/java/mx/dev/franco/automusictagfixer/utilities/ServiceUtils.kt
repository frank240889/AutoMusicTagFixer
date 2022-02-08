package mx.dev.franco.automusictagfixer.utilities

import android.app.ActivityManager
import android.content.Context

/**
 * Created by franco on 30/10/17.
 */
class ServiceUtils private constructor(context: Context) {
    private val mContext: Context
    fun checkIfServiceIsRunning(serviceName: String): Boolean {
        val manager = mContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceName == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        private var sServiceUtils: ServiceUtils? = null

        /**
         * Creates (if is not created) and return a single instance
         * of this helper
         * @param context
         * @return ServicerHelper singleton
         */
        fun getInstance(context: Context): ServiceUtils? {
            if (sServiceUtils == null) sServiceUtils = ServiceUtils(context)
            return sServiceUtils
        }
    }

    init {
        mContext = context.applicationContext
    }
}