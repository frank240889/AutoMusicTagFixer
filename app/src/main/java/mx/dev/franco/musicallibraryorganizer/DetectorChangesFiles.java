package mx.dev.franco.musicallibraryorganizer;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.File;

/**
 * Created by franco on 17/04/17.
 */

final class DetectorChangesFiles extends FileObserver {
    private static final String TAG_FILE_OBSERVER = DetectorChangesFiles.class.getName();
    private static final int mask = FileObserver.ALL_EVENTS;
    private NewFilesScannerService newFilesScannerService;
    private NewFilesScannerService.BinderService binderService;
    private Context fileObserverContext;
    private ServiceConnection serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    binderService = (NewFilesScannerService.BinderService) service;
                    newFilesScannerService = binderService.getService();
                    Log.d(TAG_FILE_OBSERVER,"CONNECTED");
                    bounded = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.d(TAG_FILE_OBSERVER,"DISCONNECTED");
                    bounded = false;
                }
            };
    private boolean bounded = false;
    private String rootPath;
    private ArrayAdapter<AudioItem> mFilesAdapter;
    static int UPDATE_FROM_FILE_OBSERVER_CHANGES = 1;
    static int UPDATE_FROM_SERVICE_SCANNER_ON_APP_START = 2;

    DetectorChangesFiles(String path, Context context, ArrayAdapter<AudioItem> filesAdapter) {
        super(path,mask);
        fileObserverContext = context;
        mFilesAdapter = filesAdapter;
        if (!path.endsWith(File.separator)){
            path += File.separator;
        }
        rootPath = path;
        Intent intent = new Intent(fileObserverContext, NewFilesScannerService.class);
        fileObserverContext.bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onEvent(int event, String path) {
        if(SelectFolderActivity.isProcessingTask){
            return;
        }
        Log.d(TAG_FILE_OBSERVER, event + " PATH");
        switch(event){
            case FileObserver.CREATE:
                if(bounded) {
                    newFilesScannerService.scanForChangedFiles(UPDATE_FROM_FILE_OBSERVER_CHANGES);
                }
                    Log.d(TAG_FILE_OBSERVER, "CREATE:" + rootPath + path);
                break;
            case FileObserver.DELETE:
                Log.d(TAG_FILE_OBSERVER, "DELETE:" + rootPath + path);
                break;
            case FileObserver.DELETE_SELF:
                Log.d(TAG_FILE_OBSERVER, "DELETE_SELF:" + rootPath + path);
                break;
            case FileObserver.MODIFY:
                Log.d(TAG_FILE_OBSERVER, "MODIFY:" + rootPath + path);
                break;
            case FileObserver.MOVED_FROM:
                Log.d(TAG_FILE_OBSERVER, "MOVED_FROM:" + rootPath + path);
                break;
            case FileObserver.MOVED_TO:
                Log.d(TAG_FILE_OBSERVER, "MOVED_TO:" + path);
                break;
            case FileObserver.MOVE_SELF:
                Log.d(TAG_FILE_OBSERVER, "MOVE_SELF:" + path);
                break;
            default:
                // just ignore
                break;
        }
    }

    @Override
    public void finalize(){
        super.finalize();
    }
}
