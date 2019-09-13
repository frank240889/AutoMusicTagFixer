package mx.dev.franco.automusictagfixer.services;

import android.app.job.JobParameters;
import android.app.job.JobService;

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
