package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обновление приложения из GitHub Releases.
 * <p>
 * Проверяет последний релиз репозитория, сравнивает версию, выбирает APK под ABI
 * устройства и вариант сборки (full/lite), скачивает его и запускает системный
 * установщик.
 */
public class FreshgramUpdater {

    public static final String REPO_OWNER = "Greznish297477";
    public static final String REPO_NAME = "freshgram-android";

    private static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";

    /** freshGram-13.1.1-7237-full-universal.apk */
    private static final Pattern ASSET_NAME =
            Pattern.compile("^freshGram-([0-9][0-9.]*)-(\\d+)-(full|lite)-(.+)\\.apk$", Pattern.CASE_INSENSITIVE);

    /** Не проверять автоматически чаще одного раза в сутки. */
    private static final long AUTO_CHECK_INTERVAL = 24L * 60L * 60L * 1000L;

    private static boolean checking;

    private FreshgramUpdater() {
    }

    public static void checkForUpdates(Activity activity, boolean force, Browser.Progress progress) {
        if (activity == null || activity.isFinishing()) {
            if (progress != null) {
                progress.end();
            }
            return;
        }
        if (!force && System.currentTimeMillis() - SharedConfig.lastUpdateCheckTime < AUTO_CHECK_INTERVAL) {
            if (progress != null) {
                progress.end();
            }
            return;
        }
        if (checking) {
            if (progress != null) {
                progress.end();
            }
            return;
        }
        checking = true;

        Utilities.globalQueue.postRunnable(() -> {
            Release release = null;
            String error = null;
            try {
                release = fetchLatestRelease();
            } catch (Exception e) {
                FileLog.e(e);
                error = e.getMessage();
            }
            Release result = release;
            final String errorText = error;
            AndroidUtilities.runOnUIThread(() -> {
                checking = false;
                SharedConfig.lastUpdateCheckTime = System.currentTimeMillis();
                SharedConfig.saveConfig();
                if (progress != null) {
                    progress.end();
                }
                if (activity.isFinishing()) {
                    return;
                }
                if (result == null) {
                    if (force) {
                        boolean noRelease = TextUtils.isEmpty(errorText) || errorText.contains("404");
                        Toast.makeText(activity,
                                noRelease
                                        ? LocaleController.getString(R.string.YourVersionIsLatest)
                                        : LocaleController.getString(R.string.FreshgramUpdateError),
                                Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                if (!result.isNewer()) {
                    if (force) {
                        Toast.makeText(activity, LocaleController.getString(R.string.YourVersionIsLatest), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                showUpdateDialog(activity, result);
            });
        });
    }

    private static void showUpdateDialog(Activity activity, Release release) {
        CharSequence changelog = TextUtils.isEmpty(release.changelog)
                ? AndroidUtilities.replaceTags(LocaleController.getString(R.string.AppUpdateChangelogEmpty))
                : release.changelog;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(LocaleController.formatString(R.string.FreshgramUpdateAvailable, release.versionName))
                .setMessage(changelog)
                .setPositiveButton(LocaleController.getString(R.string.AppUpdateDownloadNow),
                        (dialog, which) -> startDownload(activity, release))
                .setNegativeButton(LocaleController.getString(R.string.AppUpdateRemindMeLater), null);
        try {
            builder.show();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void startDownload(Activity activity, Release release) {
        if (TextUtils.isEmpty(release.downloadUrl)) {
            if (!TextUtils.isEmpty(release.htmlUrl)) {
                Browser.openUrl(activity, release.htmlUrl);
            }
            return;
        }

        final AlertDialog progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_LOADING);
        progressDialog.setMessage(LocaleController.formatString(R.string.AppUpdateDownloading, 0));
        progressDialog.setCanCancel(true);
        progressDialog.setCanceledOnTouchOutside(false);
        final boolean[] cancelled = {false};
        progressDialog.setOnCancelListener(dialog -> cancelled[0] = true);
        try {
            progressDialog.show();
        } catch (Exception e) {
            FileLog.e(e);
        }

        Utilities.globalQueue.postRunnable(() -> {
            File file = null;
            String error = null;
            try {
                file = downloadApk(release.downloadUrl, percent -> AndroidUtilities.runOnUIThread(() -> {
                    if (!cancelled[0] && progressDialog.isShowing()) {
                        progressDialog.setProgress(percent);
                        progressDialog.setMessage(LocaleController.formatString(R.string.AppUpdateDownloading, percent));
                    }
                }), cancelled);
            } catch (Exception e) {
                FileLog.e(e);
                error = e.getMessage();
            }
            File result = file;
            final String errorText = error;
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                } catch (Exception ignore) {
                }
                if (cancelled[0] || activity.isFinishing()) {
                    return;
                }
                if (result == null) {
                    Toast.makeText(activity,
                            TextUtils.isEmpty(errorText)
                                    ? LocaleController.getString(R.string.FreshgramUpdateError)
                                    : errorText,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                installApk(activity, result);
            });
        });
    }

    private interface ProgressCallback {
        void onProgress(int percent);
    }

    private static File downloadApk(String urlString, ProgressCallback callback, boolean[] cancelled) throws Exception {
        File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "cache/updates");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("Cannot create update directory");
        }
        File out = new File(dir, "freshGram-update.apk");
        if (out.exists() && !out.delete()) {
            FileLog.e("FreshgramUpdater: cannot delete old apk");
        }

        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        try {
            connection = open(urlString);
            int total = connection.getContentLength();
            input = connection.getInputStream();
            output = new FileOutputStream(out);
            byte[] buffer = new byte[16 * 1024];
            long downloaded = 0;
            int read;
            int lastPercent = -1;
            while ((read = input.read(buffer)) != -1) {
                if (cancelled != null && cancelled[0]) {
                    throw new Exception(LocaleController.getString(R.string.AppUpdateRemindMeLater));
                }
                output.write(buffer, 0, read);
                downloaded += read;
                if (total > 0) {
                    int percent = (int) (downloaded * 100 / total);
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        if (callback != null) {
                            callback.onProgress(percent);
                        }
                    }
                }
            }
            output.flush();
        } finally {
            try {
                if (output != null) output.close();
            } catch (Exception ignore) {
            }
            try {
                if (input != null) input.close();
            } catch (Exception ignore) {
            }
            if (connection != null) connection.disconnect();
        }
        if (!out.exists() || out.length() == 0) {
            throw new Exception(LocaleController.getString(R.string.FreshgramUpdateError));
        }
        return out;
    }

    public static void installApk(Activity activity, File apk) {
        Context ctx = activity != null ? activity : ApplicationLoader.applicationContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !ctx.getPackageManager().canRequestPackageInstalls()) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + ctx.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                Toast.makeText(ctx, LocaleController.getString(R.string.FreshgramUpdateAllowInstall), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                FileLog.e(e);
                Toast.makeText(ctx, LocaleController.getString(R.string.FreshgramUpdateAllowInstall), Toast.LENGTH_LONG).show();
            }
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(ctx, LocaleController.getString(R.string.FreshgramUpdateError), Toast.LENGTH_LONG).show();
        }
    }

    private static HttpURLConnection open(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        configure(connection);
        int code = connection.getResponseCode();
        // GitHub отдаёт ассеты через 302 на objects.githubusercontent.com.
        int redirects = 0;
        while (isRedirect(code) && redirects < 5) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (TextUtils.isEmpty(location)) {
                break;
            }
            connection = (HttpURLConnection) new URL(location).openConnection();
            configure(connection);
            code = connection.getResponseCode();
            redirects++;
        }
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code);
        }
        return connection;
    }

    private static void configure(HttpURLConnection connection) {
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "freshGram-Android/" + BuildConfig.VERSION_NAME);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
    }

    private static boolean isRedirect(int code) {
        return code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_MOVED_PERM
                || code == 307 || code == 308;
    }

    private static Release fetchLatestRelease() throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = open(LATEST_RELEASE_API);
            input = connection.getInputStream();
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            JSONObject root = new JSONObject(buffer.toString("UTF-8"));

            String tag = root.optString("tag_name", "");
            String versionName = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
            String changelog = root.optString("body", null);
            String htmlUrl = root.optString("html_url", null);

            JSONArray assets = root.optJSONArray("assets");
            Release best = null;
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset == null) {
                        continue;
                    }
                    String name = asset.optString("name", "");
                    Matcher m = ASSET_NAME.matcher(name);
                    if (!m.matches()) {
                        continue;
                    }
                    String assetVersion = m.group(1);
                    int versionCode;
                    try {
                        versionCode = Integer.parseInt(m.group(2));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    String variant = m.group(3).toLowerCase(Locale.ROOT);
                    String abi = m.group(4).toLowerCase(Locale.ROOT);
                    if (!variantMatches(variant)) {
                        continue;
                    }
                    int score = abiScore(abi);
                    if (score < 0) {
                        continue;
                    }
                    String url = asset.optString("browser_download_url", null);
                    if (TextUtils.isEmpty(url)) {
                        continue;
                    }
                    Release candidate = new Release(assetVersion, versionCode, changelog, url, htmlUrl, abi);
                    if (best == null || score > best.abiScore) {
                        candidate.abiScore = score;
                        best = candidate;
                    }
                }
            }
            return best;
        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception ignore) {
            }
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean variantMatches(String variant) {
        boolean lite = BuildVars.LITE;
        return lite ? "lite".equals(variant) : "full".equals(variant);
    }

    /** Чем больше — тем лучше подходит; -1 — не подходит. */
    private static int abiScore(String abi) {
        if ("universal".equals(abi)) {
            return 1;
        }
        String[] supported = Build.SUPPORTED_ABIS;
        for (int i = 0; i < supported.length; i++) {
            if (supported[i].equalsIgnoreCase(abi)) {
                return 100 - i;
            }
        }
        // x86/x86_64 устройства обычно поддерживают и universal
        return -1;
    }

    private static class Release {
        final String versionName;
        final int versionCode;
        final String changelog;
        final String downloadUrl;
        final String htmlUrl;
        final String abi;
        int abiScore;

        Release(String versionName, int versionCode, String changelog, String downloadUrl, String htmlUrl, String abi) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.changelog = changelog;
            this.downloadUrl = downloadUrl;
            this.htmlUrl = htmlUrl;
            this.abi = abi;
        }

        boolean isNewer() {
            if (versionCode <= BuildConfig.VERSION_CODE) {
                return false;
            }
            try {
                return SharedConfig.versionBiggerOrEqual(versionName, BuildConfig.VERSION_NAME);
            } catch (Exception e) {
                // нечисловой суффикс версии — ориентируемся на versionCode
                return true;
            }
        }
    }
}
