package mx.dev.franco.automusictagfixer.interfaces;

public interface CorrectionListener {
    void onTaskStarted();
    void onStartProcessingFor(int id);
    void onFinishProcessing(String error);
    void onFinishTask();
}
