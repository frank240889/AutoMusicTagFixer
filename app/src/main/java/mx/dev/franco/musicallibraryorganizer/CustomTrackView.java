package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by franco on 16/03/17.
 */

public class CustomTrackView extends LinearLayout {
    private String absoluteTrackPath;
    private String relativeTrackPath;
    private String trackName;
    private String albumName;
    private String authorName;
    private String titleName;
    private String bitrate;
    private String genre;
    private String duration;
    private int absolutePosition;
    private boolean isSelected;
    private boolean isPlaying;
    private TextView trackListTitleLabel, trackListTitle;
    private TextView trackListArtistLabel, tracklistArtist;
    private TextView trackListAlbumLabel, trackListAlbum;
    private Button contextualMenuButton;
    private ImageButton imageButton;
    private View rootView;


    public CustomTrackView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public CustomTrackView setAbsoluteTrackPath(String absoluteTrackPath){
        this.absoluteTrackPath = absoluteTrackPath;
        return this;
    }

    public CustomTrackView setAbsolutePosiion(int position){
        this.absolutePosition = position;
        return this;
    }

    public int getAbsolutePosition(){
        return this.absolutePosition;
    }

    public String getAbsoluteTrackPath(){
        return this.absoluteTrackPath;
    }

    public CustomTrackView setPlaying(boolean isPlaying){
        this.isPlaying = isPlaying;
        return this;
    }

    public boolean isPlaying(){
        return this.isPlaying;
    }

}
