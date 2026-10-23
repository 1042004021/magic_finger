package magic.finger;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import java.io.DataInputStream;

/** Foreground renderer, independent of settings Activity lifetime. */
public final class ParticleService extends Service {
    private volatile boolean active;
    private LocalServerSocket server;
    private LocalSocket client;
    private WindowManager wm;
    private MagicFinger.Sparks sparks;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (sparks != null && Intent.ACTION_SCREEN_OFF.equals(i.getAction())) {
                sparks.particles.clear(); sparks.invalidate();
            }
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("particles", "触摸特效", NotificationManager.IMPORTANCE_LOW));
        PendingIntent settings = PendingIntent.getActivity(this, 0, new Intent(this, ParticleActivity.class), PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, "particles")
                .setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("魔法手指运行中")
                .setContentText("点此调整效果或关闭").setContentIntent(settings).setOngoing(true).build());
        if (!getSharedPreferences("effects", 0).getBoolean("enabled", false) || !Settings.canDrawOverlays(this)) {
            stopSelf(); return;
        }
        try {
            wm = getSystemService(WindowManager.class);
            sparks = new MagicFinger.Sparks(this);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2038,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
            lp.setFitInsetsTypes(0); lp.alpha = 0.7f; lp.setTitle("MagicFingerParticles");
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            wm.addView(sparks, lp);
            server = new LocalServerSocket("magic-finger-module-v1"); active = true;
            registerReceiver(screen, new IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED);
            new Thread(() -> listen(), "magic-finger-input").start();
        } catch (Exception error) { android.util.Log.e("MagicFinger", "Start failed", error); stopSelf(); }
    }
    private void listen() {
        while (active) {
            try {
                client = server.accept();
                if (client.getPeerCredentials().getUid() != 0) { client.close(); continue; }
                DataInputStream input = new DataInputStream(client.getInputStream());
                while (active) {
                    final int action = input.readInt();
                    final float x = input.readFloat(), y = input.readFloat();
                    if (!Float.isFinite(x) || !Float.isFinite(y) || Math.abs(x) > 20000 || Math.abs(y) > 20000) continue;
                    handler.post(() -> {
                        if (!active || sparks == null || !getSystemService(PowerManager.class).isInteractive()) return;
                        long now = SystemClock.uptimeMillis();
                        MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
                        sparks.touch(event); event.recycle();
                    });
                }
            } catch (Exception error) { if (!active) break; }
            finally { try { if (client != null) client.close(); } catch (Exception ignored) { } }
        }
    }
    @Override public int onStartCommand(Intent i, int flags, int id) { return START_NOT_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() {
        active = false; handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(screen); } catch (Exception ignored) { }
        try { if (client != null) client.close(); } catch (Exception ignored) { }
        try { if (server != null) server.close(); } catch (Exception ignored) { }
        if (sparks != null && sparks.isAttachedToWindow()) wm.removeViewImmediate(sparks);
        sparks = null; super.onDestroy();
    }
}
