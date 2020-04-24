package mx.dev.franco.automusictagfixer.utilities;

import android.util.Log;

import javax.inject.Inject;

public class InjectedClass {
    @Inject
    public InjectedClass() {}

    public void showInjected() {
        Log.e(getClass().getName(), "Injected");
    }
}
