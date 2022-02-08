package mx.dev.franco.automusictagfixer.common

/**
 * @version Franco Castillo
 * A generic exception containing a code.
 */
class AutoMusicTagFixerException : Exception {
    var exceptionCode = -1

    constructor(message: String?, exceptionCode: Int) : super(message) {
        this.exceptionCode = exceptionCode
    }

    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}