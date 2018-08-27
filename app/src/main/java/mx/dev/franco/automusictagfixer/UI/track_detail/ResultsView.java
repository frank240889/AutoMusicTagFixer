package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;

public class ResultsView extends FrameLayout{
    private Bundle mData;
    private GnResponseListener.IdentificationResults mResults;

    public ResultsView(Context context, Bundle bundle){
        super(context);
        mData = bundle;

        LayoutInflater layoutInflater = (LayoutInflater) context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        layoutInflater.inflate(R.layout.layout_results_track_id,this, true);

        TextView title = (TextView) getChildAt(0);
        title.setText(mResults.title);
    }

    public ResultsView(Context context, GnResponseListener.IdentificationResults results){
        super(context);
        mResults = results;
    }

    public ResultsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ResultsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
    }
}
