package mx.dev.franco.automusictagfixer.covermanager

interface ICoverRunnable {
    val path: String?

    /**
     * Sets the actions for each state of the PhotoTask instance.
     * @param state The state being handled.
     */
    fun handleExtractionState(state: Int)

    /**
     * Set the cover as byte array to display.
     * @param cover
     */
    fun setCover(cover: ByteArray?)
}