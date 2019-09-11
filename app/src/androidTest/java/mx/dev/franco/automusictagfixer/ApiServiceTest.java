package mx.dev.franco.automusictagfixer;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ServiceTestRule;

import org.junit.Test;
import org.junit.runner.RunWith;

import mx.dev.franco.automusictagfixer.identifier.ApiInitializerService;

@RunWith(AndroidJUnit4.class)
public class ApiServiceTest {

    public final ServiceTestRule serviceTestRule = new ServiceTestRule();

    @Test
    public void testInitApi() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ApiInitializerService.class);
        ApplicationProvider.getApplicationContext().startService(intent);
    }
}
