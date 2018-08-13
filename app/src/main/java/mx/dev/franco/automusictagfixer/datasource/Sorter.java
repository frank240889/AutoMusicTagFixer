package mx.dev.franco.automusictagfixer.datasource;

import java.util.Comparator;

import mx.dev.franco.automusictagfixer.room.database.TrackContract;
import mx.dev.franco.automusictagfixer.room.Track;

/**
 * The instance of this class handles the sort
 * of the list
 */
public class Sorter implements Comparator<Track> {
    //Comparator for ordering list
    private static Sorter mSorter;
    //constants for indacate the sort order
    public static final int ASC = 0;
    public static final int DESC = 1;
    //default sort if no provided
    private int mSortType = ASC;
    //default sort field if no provided
    private String mSortByField = TrackContract.TrackData.DATA;

    //don't instantiate objects, we only need one
    private Sorter(){}

    public static Sorter getInstance(){
        if(mSorter == null){
            mSorter = new Sorter();
        }
        return mSorter;
    }

    /**
     * Set sort params: order by field and ascendent 0
     * or descendant 1
     * @param sortByField
     * @param sortType
     */
    public void setSortParams(String sortByField, int sortType){
        this.mSortType = sortType;
        this.mSortByField = sortByField;
    }

    @Override
    public int compare(Track track1, Track track2) {
        String str1 = null;
        String str2 = null;
        String str1ToCompare = null;
        String str2ToCompare = null;

        switch (mSortByField) {
            case TrackContract.TrackData.TITLE:
                str1 = track1.getTitle();
                str2 = track2.getTitle();
                break;
            case TrackContract.TrackData.ARTIST:
                str1 = track1.getArtist();
                str2 = track2.getArtist();
                break;
            case TrackContract.TrackData.ALBUM:
                str1 = track1.getAlbum();
                str2 = track2.getAlbum();
                break;
            default:
                str1 = track1.getPath();
                str2 = track2.getPath();
                break;
        }

        if(mSortType == DESC) {
            str1ToCompare = str1;
            str2ToCompare = str2;
        }
        else{
            str1ToCompare = str2;
            str2ToCompare = str1;
        }
        return str2ToCompare.compareToIgnoreCase(str1ToCompare);
    }
}
