package mx.dev.franco.musicallibraryorganizer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by franco on 10/11/16.
 */

public class ScreenSlidePageFragment3 extends Fragment{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_screen_slide_page3, container, false);
        return rootView;
    }
}
