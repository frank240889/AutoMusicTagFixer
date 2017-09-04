package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Created by franco on 4/12/16.
 */

@GlideModule
public final class MyGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        super.applyOptions(context, builder);
        setMemoryCache(context, builder);
    }

    public MyGlideModule() {
        super();
    }

    @Override
    public void registerComponents(Context context, Registry registry) {
        super.registerComponents(context, registry);
    }

    public void setMemoryCache(Context context, GlideBuilder builder) {
        int memoryCacheSizeBytes = 1024 * 1024 * 20; // 20mb
        builder.setMemoryCache(new LruResourceCache(memoryCacheSizeBytes));
    }

    public void setDiskCache() {
        int diskCacheSizeBytes = 1024 * 1024 * 100; // 100 MB

    }
}
