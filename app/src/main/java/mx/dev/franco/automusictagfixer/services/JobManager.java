package mx.dev.franco.automusictagfixer.services;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.util.SparseArray;

/**
 * Created by franco on 12/01/18.
 * This simple JobManager helps us to
 * control jobs started and don't recreate it
 * again
 */

public final class JobManager {
    private SparseArray jobs = new SparseArray();
    private static Context sContext;
    private static JobManager sJobManager = new JobManager();

    private JobManager(){

    }

    /**
     * Set conetext to use this JobManager
     * @param context
     * @return
     */
    public static JobManager withContext(Context context){
        if(sContext == null)
            sContext = context;
        return sJobManager;
    }

    /**
     * Adds JobInfo object to current
     * jobs list running
     * @param jobInfo
     */
    public void addJob(JobInfo jobInfo ){
        jobs.append(jobInfo.getId(), jobInfo);
    }

    /**
     * Cancel all jobs running and removes
     * from list
     */
    public void cancelAllJobs(){
        JobScheduler jobScheduler = (JobScheduler) sContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler != null) {
            jobScheduler.cancelAll();
        }

        if(hasJobs())
            jobs.clear();

    }

    /**
     * Check if there are jobs running
     * @return true if there are jobs running, false otherwise
     */
    private boolean hasJobs(){
        return (jobs.size() > 0);
    }

    /**
     * Check if any job with
     * the id provided is running
     * @param id job ID
     * @return JobInfo object, null if doesn't exist
     */
    public JobInfo getJobById(int id) {
        if(!hasJobs())
            return null;

        return (JobInfo) jobs.get(id);
    }
}
