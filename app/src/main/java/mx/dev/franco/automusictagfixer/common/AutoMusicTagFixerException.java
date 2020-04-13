package mx.dev.franco.automusictagfixer.common;

/**
 * @version Franco Castillo
 * A generic exception containing a code.
 */
public class AutoMusicTagFixerException extends Exception {
    private int mExceptionCode = -1;

    public AutoMusicTagFixerException(String message, int exceptionCode) {
        super(message);
        mExceptionCode = exceptionCode;
    }

    public AutoMusicTagFixerException(String message, Throwable cause) {
        super(message,cause);
    }

    public int getExceptionCode() {
        return mExceptionCode;
    }

    public void setExceptionCode(int exceptionCode) {
        mExceptionCode = exceptionCode;
    }
}
