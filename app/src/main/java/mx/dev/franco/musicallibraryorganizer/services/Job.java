package mx.dev.franco.musicallibraryorganizer.services;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

/**
 * Created by franco on 6/07/17.
 */

public class Job {
    /**
     * Here we schedule our task to execute and constraints to execute,
     * in this case certain time period, and pass what job we want to execute
     * in our case internet connection, placed in DetectorInternetConnection class
     * @param context
     */
    public static void scheduleJob(Context context){
        //Component we want to execute
        //we need to pass context and name of our extended JobService class
        //that will execute the task
        ComponentName serviceComponent = new ComponentName(context.getApplicationContext(), DetectorInternetConnection.class);
        //the component is passed to the builder of job,
        //this builder object are the requirements
        //needed to execute our task, in this case, we need
        //that our task executes every 5 seconds
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);

        //Minimum time for being periodic job in Nougat are 15 minutes,
        //so here we use the minimum latency instead
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            builder.setMinimumLatency(5000);
        }
        //Before Nougat, we established a periodic job
        else {
            builder.setPeriodic(5000);
        }

        //now that our builder object has the parameters set
        //get
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
