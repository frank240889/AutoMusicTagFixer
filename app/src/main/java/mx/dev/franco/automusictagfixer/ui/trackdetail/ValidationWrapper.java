package mx.dev.franco.automusictagfixer.ui.trackdetail;

import androidx.annotation.IntegerRes;

import mx.dev.franco.automusictagfixer.utilities.Message;

public class ValidationWrapper {

    @IntegerRes
    private int field;
    private Message message;

    public ValidationWrapper() {}

    public ValidationWrapper(int field, Message message) {
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

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
