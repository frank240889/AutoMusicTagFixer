package mx.dev.franco.automusictagfixer.ui.trackdetail;

import androidx.annotation.IntegerRes;

public class ValidationWrapper {

    @IntegerRes
    private int field;
    private int message;

    public ValidationWrapper() {}

    public ValidationWrapper(int field, int message) {
        this();
        this.field = field;
        this.message = message;
    }

    public int getField() {
        return field;
    }

    public void setField(int field) {
        this.field = field;
    }

    public int getMessage() {
        return message;
    }

    public void setMessage(int message) {
        this.message = message;
    }
}
