package mx.dev.franco.automusictagfixer;

import org.junit.Test;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        /*Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("mx.dev.franco.automusictagfixer", appContext.getPackageName());*/
    }


    @Test
    public void testIdentification() {
        /*File file = new File("");
        String abs = file.getAbsolutePath() + "/6_AM.MP3";
        GnApiService.init(InstrumentationRegistry.getTargetContext().getApplicationContext());
        IdentifierFactory factory = new IdentifierFactory(GnApiService.getInstance(), null);
        Identifier<Track, List<AudioFingerprintIdentifier.IdentificationResults>> identifier = factory.create(IdentifierFactory.FINGERPRINT_IDENTIFIER);
        Track track = new Track("Me faltas tu", "Los temerarios", "", abs);
        identifier.registerCallback(new Identifier.IdentificationListener<List<AudioFingerprintIdentifier.IdentificationResults>, Track>() {
            @Override
            public void onIdentificationStart(Track file) {

            }

            @Override
            public void onIdentificationFinished(List<AudioFingerprintIdentifier.IdentificationResults> result) {

            }

            @Override
            public void onIdentificationError(Track file, String error) {

            }

            @Override
            public void onIdentificationCancelled(Track file) {

            }

            @Override
            public void onIdentificationNotFound(Track file) {

            }
        });
        identifier.identify(track);*/
    }
}
