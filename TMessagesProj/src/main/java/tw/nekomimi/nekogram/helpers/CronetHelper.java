package tw.nekomimi.nekogram.helpers;

import android.content.Context;

import com.google.android.gms.net.CronetProviderInstaller;

import org.chromium.net.CronetEngine;
import org.telegram.messenger.FileLog;

import java.io.File;

public class CronetHelper {

    // На части прошивок (Huawei EMUI) инициализация Cronet из GMS убивает процесс
    // на старте, поэтому используем OkHttp, пока это не будет исправлено.
    public static final boolean ENABLED = false;

    private static CronetEngine engine = null;

    public static void init(Context context) {
        if (!ENABLED) {
            return;
        }
        CronetProviderInstaller.installProvider(context)
                .continueWith(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    try {
                        engine = createEngine(context);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                    return null;
                })
                .addOnFailureListener(e -> FileLog.e("Failed to create cronet engine", e));
    }

    public static CronetEngine createEngine(Context context) {
        var builder = new CronetEngine.Builder(context);
        builder.enableBrotli(true);
        builder.enableHttp2(true);
        builder.enableQuic(true);
        var cacheDir = new File(context.getCacheDir(), "cronet");
        if (cacheDir.exists() || cacheDir.mkdirs()) {
            builder.setStoragePath(cacheDir.getAbsolutePath());
            builder.enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 10 * 1024 * 1024);
        }
        return builder.build();
    }

    public static boolean isAvailable() {
        return ENABLED && engine != null;
    }

    public static CronetEngine getEngine() {
        return engine;
    }
}
