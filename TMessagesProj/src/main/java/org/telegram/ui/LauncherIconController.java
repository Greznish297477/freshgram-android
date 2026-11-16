package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    private static final String PREF_KEY = "freshgram_launcher_icon";

    public static void tryFixLauncherIconIfNeeded() {
        // freshGram: после обновления APK система может сбросить состояние компонентов.
        // Приоритет — сохранённая пользователем иконка.
        LauncherIcon saved = getSavedIcon();
        if (saved != null) {
            if (!isEnabled(saved)) {
                setIcon(saved);
            }
            return;
        }
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }
        setIcon(LauncherIcon.DEFAULT);
    }

    private static android.content.SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", android.content.Context.MODE_PRIVATE);
    }

    public static void saveIcon(LauncherIcon icon) {
        if (icon == null) {
            return;
        }
        try {
            prefs().edit().putString(PREF_KEY, icon.name()).apply();
        } catch (Throwable ignore) {
        }
    }

    public static LauncherIcon getSavedIcon() {
        try {
            String name = prefs().getString(PREF_KEY, null);
            if (name != null) {
                return LauncherIcon.valueOf(name);
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /** freshGram: возвращает текущую выбранную иконку приложения (для сплэша и т.п.). */
    public static LauncherIcon getSelectedIcon() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return icon;
            }
        }
        return LauncherIcon.DEFAULT;
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        saveIcon(icon);
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconDefault),
        //VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.drawable.ic_launcher_foreground, R.string.AppIconVintage),
        AQUA("AquaIcon", R.color.ic_launcher_background, R.drawable.icon_4_fg, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.color.ic_launcher_background, R.drawable.icon_3_fg, R.string.AppIconPremium),
        TURBO("TurboIcon", R.color.ic_launcher_background, R.drawable.icon_5_fg, R.string.AppIconTurbo),
        NOX("NoxIcon", R.color.ic_launcher_background, R.drawable.icon_2_fg, R.string.AppIconNox),
        MERIO("MerioIcon", R.color.ic_launcher_background, R.drawable.icon_7_fg, R.string.AppIconMerio),
        RAINBOW("RainbowIcon", R.color.ic_launcher_background, R.drawable.icon_8_fg, R.string.AppIconRainbow),
        SCHOOL("OldSchoolIcon", R.color.ic_launcher_background, R.drawable.icon_9_fg, R.string.AppIconOldSchool),
        MUSHEEN("MusheenIcon", R.color.ic_launcher_background, R.drawable.icon_10_fg, R.string.AppIconMusheen),
        SPACE("SpaceIcon", R.color.ic_launcher_background, R.drawable.icon_11_fg, R.string.AppIconSpace),
        CLOUD("CloudIcon", R.color.ic_launcher_background, R.drawable.icon_12_fg, R.string.AppIconCloud),
        NEON("NeonIcon", R.color.ic_launcher_background, R.drawable.icon_13_fg, R.string.AppIconNeon),
        MATERIAL("MaterialIcon", R.color.ic_launcher_background, R.drawable.icon_14_fg, R.string.AppIconMaterial),
        WHEELOLD("WheelOldIcon", R.color.ic_launcher_background, R.drawable.icon_15_fg, R.string.AppIconWheelOld),
        WIN95("Win95Icon", R.color.ic_launcher_background, R.drawable.icon_16_fg, R.string.AppIconWin95),
        MATERIALDESIGN("MaterialDesignIcon", R.color.ic_launcher_background, R.drawable.icon_17_fg, R.string.AppIconMaterialDesign);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
