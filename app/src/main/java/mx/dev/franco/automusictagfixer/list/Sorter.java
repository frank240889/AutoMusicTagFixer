package mx.dev.franco.automusictagfixer.list;

import java.util.Comparator;

import mx.dev.franco.automusictagfixer.room.database.TrackContract;

public class Sorter implements Comparator<AudioItem> {
    //constants for indacate the sort order
    public static final int ASC = 0;
    public static final int DESC = 1;
    //Comparator for ordering list
    private static Sorter mSorter;
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
     * Set sort params: order by field and ascendant
     * or descendant order.
     * @param sortByField
     * @param sortType
     */
    public void setSortParams(String sortByField, int sortType){
        mSortType = sortType;
        mSortByField = sortByField;
    }

    @Override
    public int compare(AudioItem audioItem1, AudioItem audioItem2) {
        String str1 = null;
        String str2 = null;
        String str1ToCompare = null;
        String str2ToCompare = null;

        switch (mSortByField) {
            case TrackContract.TrackData.TITLE:
                str1 = audioItem1.getTitle();
                str2 = audioItem2.getTitle();
                break;
            case TrackContract.TrackData.ARTIST:
                str1 = audioItem1.getArtist();
                str2 = audioItem2.getArtist();
                break;
            case TrackContract.TrackData.ALBUM:
                str1 = audioItem1.getAlbum();
                str2 = audioItem2.getAlbum();
                break;
            default:
                str1 = audioItem1.getAbsolutePath();
                str2 = audioItem2.getAbsolutePath();
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
