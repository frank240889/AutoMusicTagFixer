package mx.dev.franco.musicallibraryorganizer;

/**
 * Created by franco on 31/05/17.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * This class is a custom seekbar, useful for
 * represent a value slide selector
 */
public final class SeekBarListPreference extends ListPreference implements SeekBar.OnSeekBarChangeListener, View.OnClickListener{

    // Private attributes :
    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private TextView mValueText;
    private Context mContext;
    private CharSequence mValue;

    private String mDialogMessage;
    //
    public SeekBarListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        // Get string value for dialogMessage
        int mDialogMessageId = attrs.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if (mDialogMessageId == 0)
            mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        else
            mDialogMessage = mContext.getString(mDialogMessageId);

    }


    /**
     * We create the View because don't have
     * a xml layout (is not necessary), set
     * its positions and returning this view,
     * that is, show to user.
     * @return
     */
    @Override
    protected View onCreateDialogView() {

        //We create the root layout first and its params
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);


        //This texfview will have the description for
        //selected value
        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.START);
        mValueText.setTextSize(16);
        mValueText.setPadding(70,50,0,50);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        //Then we create the SeekBar, this element
        //is a slide, like the progress bar of any video/audio player
        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setProgressBarValue();

        return layout;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        // do not call super
    }

    /**
     * This method sets the value, if is store in SharedPreference,
     * retreieves its value and, sets the max value that it
     * can be shown, either
     */
    private void setProgressBarValue() {
        String mValue = null;
        if (shouldPersist()) {
            mValue = getValue();
        }

        final int max = this.getEntries().length - 1;

        mSeekBar.setMax(max);
        mSeekBar.setProgress(this.findIndexOfValue(mValue));
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        setProgressBarValue();
    }

    /**
     * This method changes the value and description of textfield
     * when we slide the progress bar
     * @param seek
     * @param value
     * @param fromTouch
     */

    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {

        final CharSequence textToDisplay = getEntryFromValue(value);
        mValueText.setText(textToDisplay);
        mValue = getValueFromIndexValue(value);


    }

    /**
     * This method get the entry from String.xml, corresponding
     * to current value selected in progress bar
     * @param value
     * @return
     */
    private CharSequence getEntryFromValue(int value) {
        CharSequence[] entries = getEntries();
        return value >= 0 && entries != null ? entries[value] : null;
    }


    /**
     * This method get the value from String.xml corresponding
     * to the current value selected in progress bar
     * @param value
     * @return
     */
    private CharSequence getValueFromIndexValue(int value){
        CharSequence[] entries = getEntryValues();
        return value >= 0 && entries != null ? entries[value] : null;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }


    /**
     * Shows the dialog and sets its positive button.
     * @param state
     */
    @Override
    public void showDialog(Bundle state) {
        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    /**
     * Listener for positive button, this saves the current value
     * in shared preferences and dismiss the dialog,
     * @param v
     */
    @Override
    public void onClick(View v) {

        if (shouldPersist()) {
            final int progressChoice = mSeekBar.getProgress();
            setValueIndex(progressChoice);
            callChangeListener(mValue);
        }

        getDialog().dismiss();
    }

}