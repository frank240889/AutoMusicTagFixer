package mx.dev.franco.automusictagfixer.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

@Module
public class ServiceUtilsModule {

  @Provides
  @Singleton
  ServiceUtils provideServiceUtils(Context context) {
    return ServiceUtils.getInstance(context);
  }
}
