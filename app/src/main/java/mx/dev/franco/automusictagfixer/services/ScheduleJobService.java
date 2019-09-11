package mx.dev.franco.automusictagfixer.services;

import android.app.job.JobParameters;
import android.app.job.JobService;

import mx.dev.franco.automusictagfixer.identifier.GnApiService;

/**
 * Created by franco on 6/07/17.
 */

public class ScheduleJobService extends JobService {
    /**
     * Callback when service start, here is when we execute our
     * task in background, so an intensive task
     * needs to run in another thread because
     * this callback it executes on UI Thread
     * @param params Are the parameters with was built
     *                   the job
     * @return true for execute the code
     */

    @Override
    public boolean onStartJob(final JobParameters params) {
        //We set context and initialize the GNSDK API if it was not before.
        boolean shouldInitialize = (!GnApiService.getInstance().isApiInitialized());
        if(shouldInitialize) {
            //GnApiService.API_INITIALIZED_AFTER_CONNECTED flag indicates
            //that service was not initialized from Splash.
            //is useful to inform to user in MainActivity
            //that API of GNSDK has been initialized
            GnApiService.getInstance().initializeAPI();
        }
        //if already should not initialize it, we finalize this job
        jobFinished(params, !shouldInitialize);
        return true;
    }

    /**
     * If case Job stops abruptly, we can handle here, if we
     * want to restart, we need to return true, false if not.
     * This callback it executes on UI Thread too
     * @param params Are the parameters with was built
     *                   the job
     * @return
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

}
