package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.LauncherIconController;

/**
 * freshGram: сплэш-заставка при запуске приложения.
 * Показывает ту иконку приложения, которая выбрана в LauncherIconController,
 * с анимацией появления (AyuGram-style) и плавно растворяется.
 */
public class SplashOverlay extends FrameLayout {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final IconView iconView;
    private final TextView titleView;
    private boolean finished;

    public SplashOverlay(Context context, boolean isDark) {
        super(context);
        setWillNotDraw(false);

        final int backgroundColor = isDark ? 0xFF1F2732 : 0xFFFFFFFF;
        final int titleColor = isDark ? 0xFFFFFFFF : 0xFF1E1E1E;
        backgroundPaint.setColor(backgroundColor);

        iconView = new IconView(context);
        addView(iconView, LayoutHelper.createFrame(96, 96, Gravity.CENTER, 0, -60, 0, 0));

        titleView = new TextView(context);
        titleView.setText("freshGram");
        titleView.setTextSize(28);
        titleView.setTypeface(org.telegram.messenger.AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setTextColor(titleColor);
        titleView.setGravity(Gravity.CENTER);
        titleView.setAlpha(0f);
        addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 84, 0, 0));
    }

    public void start() {
        iconView.setScaleX(0.7f);
        iconView.setScaleY(0.7f);
        iconView.setAlpha(0f);
        iconView.setRotation(-12f);

        iconView.animate().scaleX(1f).scaleY(1f).alpha(1f).rotation(0f)
                .setInterpolator(new DecelerateInterpolator(2f))
                .setDuration(420).start();

        titleView.animate().alpha(1f).setStartDelay(160).setDuration(320).start();

        postDelayed(() -> {
            if (finished) {
                return;
            }
            animate().alpha(0f).setStartDelay(150).setDuration(300)
                    .withEndAction(this::removeSelf).start();
        }, 900);
    }

    private void removeSelf() {
        if (finished) {
            return;
        }
        finished = true;
        if (getParent() instanceof FrameLayout) {
            ((FrameLayout) getParent()).removeView(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
    }

    /** Рисует круг с заливкой и выбранной пользователем иконкой поверх. */
    private static class IconView extends View {
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final Drawable background;
        private final Drawable foreground;

        IconView(Context context) {
            super(context);
            LauncherIconController.LauncherIcon icon = LauncherIconController.getSelectedIcon();
            if (icon == null) {
                icon = LauncherIconController.LauncherIcon.DEFAULT;
            }
            background = ContextCompat.getDrawable(context, icon.background);
            foreground = ContextCompat.getDrawable(context, icon.foreground);
            circlePaint.setColor(Color.WHITE);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            path.rewind();
            path.addCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, Path.Direction.CW);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (background == null) {
                return;
            }
            // фон иконки (заливка круга)
            canvas.save();
            canvas.clipPath(path);
            background.setBounds(0, 0, getWidth(), getHeight());
            background.draw(canvas);
            canvas.restore();

            // передний план иконки, чуть больше круга, чтобы заполнить его
            if (foreground != null) {
                int over = AndroidUtilities.dp(3);
                canvas.save();
                canvas.clipPath(path);
                foreground.setBounds(-over, -over, getWidth() + over, getHeight() + over);
                foreground.draw(canvas);
                canvas.restore();
            }
        }
    }
}
