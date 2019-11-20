package mx.dev.franco.automusictagfixer.covermanager;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.persistence.cache.CoverDataCache;
import mx.dev.franco.automusictagfixer.ui.AudioHolder;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;

/**
 * This manager handles the extraction of covers from audio files and then
 * present them to the user into an {@link AudioHolder} object.
 * @author Franco Castillo
 */
public class CoverManager {
    static final int EXTRACTION_STARTED = 0;
    static final int EXTRACTION_FINISHED = 1;
    static final int EXTRACTION_ERROR = 2;

    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAXIMUM_POOL_SIZE = (NUMBER_OF_CORES * 2) + 1;

    private final BlockingQueue<Runnable> mExtractWorkQueue;
    private final Queue<CoverTask> mCoverTaskQueue;
    private final ThreadPoolExecutor mCoverExtractionThreadPool;
    private static CoverDataCache mCoverDataCache;

    private Handler mUiHandler;

    private static CoverManager sInstance;

    /**
     * A static block initialize the necessary static fields.
     */
    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.MINUTES;
        sInstance = new CoverManager();
        mCoverDataCache = new CoverDataCache();
    }

    /**
     * Get the instance of this manager.
     * @return A instance in a singleton pattern.
     */
    public static CoverManager getInstance() {
        return sInstance;
    }

    /**
     * Private constructor, we don't need to instantiate directly.
     */
    private CoverManager(){
        mExtractWorkQueue = new LinkedBlockingQueue<>();
        mCoverTaskQueue = new LinkedBlockingQueue<>();
        mCoverExtractionThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mExtractWorkQueue);

        mUiHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                CoverTask coverTask = (CoverTask) msg.obj;
                AudioHolder audioItemHolder = coverTask.getAudioHolder();
                byte[] cover = coverTask.getCover();
                String id = coverTask.getId();
                mCoverDataCache.add(id, cover);
                if(audioItemHolder != null) {
                    loadCover(audioItemHolder, cover);
                }

                recycleTask(coverTask);
            }
        };
    }

    private void recycleTask(CoverTask coverTask) {
        coverTask.recycle();
        mCoverTaskQueue.offer(coverTask);
    }

    private static void loadCover(AudioHolder holder, byte[] result) {
        Log.w(CoverManager.class.getName(), "loading cover");
            GlideApp.with(holder.itemView)
                    .load(result)
                    .theme(holder.cover.getContext().getTheme())
                    .thumbnail(0.5f)
                    .error(holder.cover.getContext().getDrawable(R.drawable.ic_album_white_48px))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .fitCenter()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e,
                                                    Object model,
                                                    Target<Drawable> target,
                                                    boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource,
                                                       Object model,
                                                       Target<Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            return false;
                        }
                    })
                    .placeholder(holder.cover.getContext().getDrawable(R.drawable.ic_album_white_48px))
                    .into(holder.cover);
    }

    /**
     * Handles state messages for a particular task object
     * @param coverTask A task object
     * @param state The state of the task
     */
    @SuppressLint("HandlerLeak")
    public void handleState(CoverTask coverTask, int state) {
        switch (state) {

            // The task finished downloading and decoding the image
            case EXTRACTION_FINISHED:

                // Gets a Message object, stores the state in it, and sends it to the Handler
                Message completeMessage = mUiHandler.obtainMessage(state, coverTask);
                completeMessage.sendToTarget();
                break;
            default:
                mUiHandler.obtainMessage(state, coverTask).sendToTarget();
                break;
        }

    }

    public static CoverTask startFetchingCover(
            AudioHolder audioItemHolder, String path, String id) {
        /*
         * Gets a task from the pool of tasks, returning null if the pool is empty
         */
        CoverTask coverTask = null;
        byte[] data = mCoverDataCache.load(id);
        if(data != null) {
            loadCover(audioItemHolder, data);
        }
        else {
            coverTask = sInstance.mCoverTaskQueue.poll();
            // If the queue was empty, create a new task instead.
            if (null == coverTask) {
                coverTask = new CoverTask();
            }

            // Initializes the task
            coverTask.startFetching(CoverManager.sInstance, audioItemHolder, path, id);


            /*
             * "Executes" the tasks' download Runnable in order to download the image. If no
             * Threads are available in the thread pool, the Runnable waits in the queue.
             */
            sInstance.mCoverExtractionThreadPool.execute(coverTask.getExtractionRunnable());
        }


        // Returns a task object, either newly-created or one from the task pool
        return coverTask;
    }

    /**
     * Cancels all Threads in the ThreadPool
     */
    public static void cancelAll() {

        /*
         * Creates an array of tasks that's the same size as the task work queue
         */
        CoverTask[] taskArray = new CoverTask[sInstance.mCoverTaskQueue.size()];

        // Populates the array with the task objects in the queue
        sInstance.mCoverTaskQueue.toArray(taskArray);

        // Stores the array length in order to iterate over the array
        int taskArraylen = taskArray.length;

        /*
         * Locks on the singleton to ensure that other processes aren't mutating Threads, then
         * iterates over the array of tasks and interrupts the task's current Thread.
         */
        synchronized (sInstance) {

            // Iterates over the array of tasks
            for (int taskArrayIndex = 0; taskArrayIndex < taskArraylen; taskArrayIndex++) {

                // Gets the task's current thread
                Thread thread = taskArray[taskArrayIndex].mThreadThis;

                // if the Thread exists, post an interrupt to it
                if (null != thread) {
                    thread.interrupt();
                }
            }
        }
    }

    public static void removeCover(String id) {
        mCoverDataCache.delete(id);
    }
}
