package mx.dev.franco.automusictagfixer.services;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

/**
 * Created by franco on 6/07/17.
 */

public class Job {
    public static final int ID_JOB = 1;
    /**
     * Here we schedule our task and constraints to execute,
     * in this case a periodic task, that will try to initialize the API
     * Gracenote
     * @param context
     */
    public static void scheduleJob(Context context){
        //Component we want to execute
        //we need to pass context and name of our extended JobService class
        //that will execute the task
        ComponentName serviceComponent = new ComponentName(context.getApplicationContext(), ScheduleJobService.class);
        //this builder object are the requirements
        //needed to execute our task, in this case, we need
        //that our task executes every 5 seconds
        JobInfo.Builder builder = new JobInfo.Builder(ID_JOB, serviceComponent);
        //Using setMinimumLatency with setOverrideDeadline together,
        //it makes something like minimum and maximum limit to execute the code, is useful
        //because minimum time for setPeriodic in Nougat are 15 minutes, so if you require
        //to execute your task in lower range of time is not going to work.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //minimum latency means the min limit in milliseconds JobScheduler has to wait to execute the code
            builder.setMinimumLatency(5000);
            //setOverrideDeadline means the max limit in milliseconds in which your code is
            //going to execute.
            builder.setOverrideDeadline(5000);
        }
        //For android <= M there is no problem in use setPeriodic (instead of two before), with less than 15 minutes
        else {
            builder.setPeriodic(5000);
        }

        //time to schedule task
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo job = builder.build();
        jobScheduler.schedule(job);
    }
}
