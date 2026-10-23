package magic.finger;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;

/** Local settings only; no network or touch history. */
public final class ParticleActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout layout;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("effects", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        layout = new LinearLayout(this); layout.setOrientation(1); layout.setPadding(40, 60, 40, 40);
        scroll.addView(layout); setContentView(scroll);
        label("魔法手指", 30);
        label("KernelSU 全局触摸粒子 · 实验版\n设置即时生效；模块负责 Root 输入后端。", 16);
        Switch enabled = new Switch(this); enabled.setText("开启全局特效");
        enabled.setChecked(prefs.getBoolean("enabled", false)); layout.addView(enabled);
        enabled.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("enabled", checked).apply();
            if (checked) launch(); else stopService(new Intent(this, ParticleService.class));
        });
        Button permission = new Button(this); permission.setText("授权悬浮窗"); layout.addView(permission);
        permission.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        label("粒子效果", 18);
        Spinner effect = new Spinner(this);
        effect.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"星星", "爱心", "光点", "光环", "随机"}));
        effect.setSelection(prefs.getInt("effect", 0)); layout.addView(effect);
        effect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int position, long id) {
                prefs.edit().putInt("effect", position).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        slider("density", "粒子密度", 1, 8, 3);
        slider("size", "粒子大小（像素）", 6, 40, 18);
        slider("life", "消散时间（毫秒）", 200, 1600, 750);
        label("本页预览（不需要 Root）", 18);
        MagicFinger.Sparks preview = new MagicFinger.Sparks(this);
        preview.setBackgroundColor(0xff182338);
        preview.setOnTouchListener((v, e) -> { preview.touch(e); return true; });
        layout.addView(preview, new LinearLayout.LayoutParams(-1, 450));
        Button start = new Button(this); start.setText("启动 / 重新连接"); layout.addView(start);
        start.setOnClickListener(v -> launch());
        label("首次安装：先授予悬浮窗权限，再开启特效。模块启用后重启生效。\n其他应用可禁止悬浮窗；锁屏及敏感页面不保证显示。\n没有记录触摸轨迹，也不申请联网权限。", 15);
    }
    private void launch() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show(); return;
        }
        if (!prefs.getBoolean("enabled", false)) {
            Toast.makeText(this, "请先打开特效开关", Toast.LENGTH_SHORT).show(); return;
        }
        startForegroundService(new Intent(this, ParticleService.class));
    }
    private void label(String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size);
        view.setPadding(0, 20, 0, 12); layout.addView(view);
    }
    private void slider(final String key, String title, final int min, int max, int fallback) {
        final TextView value = new TextView(this);
        int current = Math.max(min, Math.min(max, prefs.getInt(key, fallback)));
        value.setText(title + "：" + current); layout.addView(value);
        SeekBar bar = new SeekBar(this); bar.setMax(max - min); bar.setProgress(current - min); layout.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int p, boolean user) {
                value.setText(title + "：" + (min + p));
                if (user) prefs.edit().putInt(key, min + p).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar b) { }
            @Override public void onStopTrackingTouch(SeekBar b) { }
        });
    }
}
