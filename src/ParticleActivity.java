package magic.finger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Local settings only; no network or touch history. */
public final class ParticleActivity extends Activity {
    private static final int PAGE_TOP = 0xff0b1020;
    private static final int PAGE_BOTTOM = 0xff182238;
    private static final int CARD = 0xff151e30;
    private static final int CARD_STROKE = 0xff28344c;
    private static final int TEXT = 0xfff3f6fb;
    private static final int MUTED = 0xff8fa0b8;
    private static final int ACCENT = 0xffff7ab6;
    private static final int ACCENT2 = 0xff8e7cff;
    private static final int OK = 0xff5be3a6;
    private static final String[] EFFECTS = {"星星", "爱心", "光点", "光环", "随机"};

    private SharedPreferences prefs;
    private LinearLayout layout;
    private TextView statusText;
    private TextView permissionButton;
    private View statusDot;
    private int effectIndex;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("effects", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackground(pageBackground());
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(26), dp(20), dp(32));
        scroll.addView(layout, new FrameLayout.LayoutParams(-1, -2));
        setContentView(scroll);
        paintSystemBars();
        buildHeader();
        buildStatus();
        buildEffects();
        buildSliders();
        buildPreview();
        buildFooter();
    }

    private void paintSystemBars() {
        getWindow().setStatusBarColor(PAGE_TOP);
        getWindow().setNavigationBarColor(PAGE_TOP);
        WindowInsetsController bars = getWindow().getInsetsController();
        if (bars != null) {
            bars.setSystemBarsAppearance(0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    private Drawable pageBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{PAGE_TOP, PAGE_BOTTOM});
        return drawable;
    }

    private Drawable rounded(int fill, float radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private Drawable accentGradient(float radius) {
        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, new int[]{ACCENT, ACCENT2});
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private Drawable ripple(float radius, Drawable content) {
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xffffffff);
        mask.setCornerRadius(dp(radius));
        return new RippleDrawable(ColorStateList.valueOf(0x40ffffff), content, mask);
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return view;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(18));
        box.setBackground(rounded(CARD, 18, CARD_STROKE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(14);
        layout.addView(box, params);
        return box;
    }

    private void buildHeader() {
        TextView title = text("魔法手指", 27, TEXT, true);
        title.setLetterSpacing(0.06f);
        layout.addView(title);
        TextView subtitle = text("KernelSU · 全局触摸粒子 · 实验版", 12, MUTED, false);
        subtitle.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-2, -2);
        subtitleParams.topMargin = dp(6);
        layout.addView(subtitle, subtitleParams);
        View bar = new View(this);
        bar.setBackground(accentGradient(2));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(46), dp(3));
        barParams.topMargin = dp(16);
        layout.addView(bar, barParams);
    }

    private LinearLayout section(LinearLayout parent, String title) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View mark = new View(this);
        mark.setBackground(accentGradient(2));
        row.addView(mark, new LinearLayout.LayoutParams(dp(3), dp(15)));
        TextView label = text(title, 15, TEXT, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
        labelParams.leftMargin = dp(9);
        row.addView(label, labelParams);
        parent.addView(row);
        return row;
    }

    private void buildStatus() {
        LinearLayout box = card();
        section(box, "运行状态");
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        statusDot = new View(this);
        line.addView(statusDot, new LinearLayout.LayoutParams(dp(8), dp(8)));
        statusText = text("", 13, MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.leftMargin = dp(8);
        line.addView(statusText, statusParams);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, -2);
        lineParams.topMargin = dp(12);
        box.addView(line, lineParams);
        permissionButton = text("授权悬浮窗", 14, 0xffffffff, true);
        permissionButton.setGravity(Gravity.CENTER);
        permissionButton.setBackground(ripple(24, accentGradient(24)));
        permissionButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(48));
        buttonParams.topMargin = dp(16);
        box.addView(permissionButton, buttonParams);
    }

    private void updateStatus() {
        boolean granted = Settings.canDrawOverlays(this);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(granted ? OK : 0xfff0a35e);
        statusDot.setBackground(dot);
        statusText.setText(granted
                ? "悬浮窗已授权 · 刷入模块后重启自动生效"
                : "尚未授权悬浮窗 · 模块无法绘制粒子");
        statusText.setTextColor(granted ? MUTED : 0xfff0a35e);
        permissionButton.setText(granted ? "重新授权悬浮窗" : "去授权悬浮窗");
        permissionButton.setTextColor(granted ? ACCENT : 0xffffffff);
        permissionButton.setBackground(granted
                ? ripple(24, rounded(0xff1b2540, 24, 0xff3a4a68))
                : ripple(24, accentGradient(24)));
    }

    private void buildEffects() {
        LinearLayout box = card();
        section(box, "粒子效果");
        effectIndex = Math.max(0, Math.min(EFFECTS.length - 1, prefs.getInt("effect", 0)));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(12);
        box.addView(row, rowParams);
        for (int i = 0; i < EFFECTS.length; i++) {
            final int index = i;
            TextView chip = text(EFFECTS[i], 13, MUTED, false);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(0, dp(10), 0, dp(10));
            chip.setOnClickListener(v -> {
                effectIndex = index;
                prefs.edit().putInt("effect", index).apply();
                for (int n = 0; n < row.getChildCount(); n++) applyChip((TextView) row.getChildAt(n), n == index);
            });
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) chipParams.leftMargin = dp(6);
            row.addView(chip, chipParams);
            applyChip(chip, i == effectIndex);
        }
    }

    private void applyChip(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackground(ripple(11, accentGradient(11)));
            chip.setTextColor(0xffffffff);
            chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        } else {
            chip.setBackground(ripple(11, rounded(0xff1e2942, 11, CARD_STROKE)));
            chip.setTextColor(MUTED);
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void buildSliders() {
        LinearLayout box = card();
        section(box, "手感参数");
        slider(box, "density", "粒子密度", 1, 8, 3, "");
        slider(box, "size", "粒子大小", 6, 40, 18, " px");
        slider(box, "life", "消散时间", 200, 1600, 750, " ms");
    }

    private void slider(LinearLayout box, final String key, String title,
                        final int min, int max, final int fallback, String unit) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(title, 13, MUTED, false);
        head.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        final TextView value = text("", 13, ACCENT, true);
        head.addView(value);
        LinearLayout.LayoutParams headParams = new LinearLayout.LayoutParams(-1, -2);
        headParams.topMargin = dp(16);
        box.addView(head, headParams);
        final int current = Math.max(min, Math.min(max, prefs.getInt(key, fallback)));
        value.setText(current + unit);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setProgress(current - min);
        bar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        bar.setThumbTintList(ColorStateList.valueOf(0xffffd3e6));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0xff2c3850));
        bar.setSplitTrack(false);
        bar.setPadding(0, dp(8), 0, 0);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int progress, boolean user) {
                value.setText((min + progress) + unit);
                if (user) prefs.edit().putInt(key, min + progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar b) { }
            @Override public void onStopTrackingTouch(SeekBar b) { }
        });
        box.addView(bar, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildPreview() {
        LinearLayout box = card();
        LinearLayout head = section(box, "本页预览");
        TextView hint = text("无需 Root · 直接拖动", 12, MUTED, false);
        hint.setGravity(Gravity.END);
        head.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
        FrameLayout frame = new FrameLayout(this);
        GradientDrawable shade = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xff0d1524, 0xff1b2843});
        shade.setCornerRadius(dp(16));
        shade.setStroke(dp(1), CARD_STROKE);
        frame.setBackground(shade);
        frame.setClipToOutline(true);
        final TextView placeholder = text("轻触此处预览效果", 12, 0xff4a5b78, false);
        placeholder.setGravity(Gravity.CENTER);
        frame.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
        MagicFinger.Sparks preview = new MagicFinger.Sparks(this);
        preview.setBackgroundColor(0x00000000);
        preview.setOnTouchListener((v, e) -> {
            placeholder.setVisibility(View.GONE);
            preview.touch(e);
            return true;
        });
        frame.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(-1, dp(300));
        frameParams.topMargin = dp(14);
        box.addView(frame, frameParams);
        TextView caption = text("上方参数会即时同步到全局效果", 11, 0xff74839b, false);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-1, -2);
        captionParams.topMargin = dp(10);
        box.addView(caption, captionParams);
    }

    private void buildFooter() {
        TextView foot = text("刷入模块即开启 · 停用模块并重启后关闭\n不记录触摸轨迹 · 不申请联网权限", 11, 0xff6f7d95, false);
        foot.setGravity(Gravity.CENTER);
        foot.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(22);
        layout.addView(foot, params);
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
    }
}