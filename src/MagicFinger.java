package magic.finger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Random;

/** Standalone root visual prototype. Observes input, never pilfers or injects. */
public final class MagicFinger {
    private static Object monitor;
    private static InputEventReceiver receiver;
    private static WindowManager wm;
    private static Sparks view;
    private static long events;
    private static boolean attached;

    static final class Particle {
        float x, y, vx, vy, angle, spin, size;
        long born;
        int color;
        int kind;
        float life;
    }

    static final class Sparks extends View {
        final ArrayList<Particle> particles = new ArrayList<>();
        final Random random = new Random();
        final Paint paint = new Paint(3);
        final Path star = new Path();
        long lastFrame;
        long lastEmit;
        boolean drawing;
        final android.content.SharedPreferences prefs;
        final Path heart = new Path();
        Sparks(Context context) {
            super(context);
            prefs = "android".equals(context.getPackageName()) ? null
                    : context.getSharedPreferences("effects", Context.MODE_PRIVATE);
            heart.moveTo(0, 0.9f);
            heart.cubicTo(-1.8f, -0.2f, -0.8f, -1.5f, 0, -0.55f);
            heart.cubicTo(0.8f, -1.5f, 1.8f, -0.2f, 0, 0.9f);
            heart.close();
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5 - Math.PI / 2;
                float radius = i % 2 == 0 ? 1 : 0.43f;
                float x = (float) Math.cos(a) * radius, y = (float) Math.sin(a) * radius;
                if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
            }
            star.close();
        }
        void touch(MotionEvent event) {
            int action = event.getActionMasked();
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE
                    && action != MotionEvent.ACTION_POINTER_DOWN) return;
            long now = android.os.SystemClock.uptimeMillis();
            if (action == MotionEvent.ACTION_MOVE && now - lastEmit < 14) return;
            lastEmit = now;
            for (int finger = 0; finger < event.getPointerCount(); finger++) {
                int density = Math.max(1, Math.min(8, setting("density", 3)));
                for (int i = 0; i < (action == MotionEvent.ACTION_MOVE ? density : density * 3); i++) {
                    if (particles.size() >= 220) particles.remove(0);
                    Particle p = new Particle();
                    p.x = event.getX(finger); p.y = event.getY(finger);
                    double angle = random.nextDouble() * Math.PI * 2;
                    float speed = 65 + random.nextFloat() * 170;
                    p.vx = (float) Math.cos(angle) * speed;
                    p.vy = (float) Math.sin(angle) * speed - 60;
                    p.angle = random.nextFloat() * 360;
                    p.spin = random.nextFloat() * 240 - 120;
                    p.size = Math.max(6, Math.min(40, setting("size", 18))) * (0.6f + random.nextFloat() * 0.8f);
                    p.life = Math.max(200, Math.min(1600, setting("life", 750)));
                    p.kind = setting("effect", 0);
                    if (p.kind == 4) p.kind = random.nextInt(4);
                    p.born = now;
                    p.color = Color.HSVToColor(new float[]{280 + random.nextFloat() * 100, 0.55f, 1});
                    particles.add(p);
                }
            }
            if (!drawing) { drawing = true; lastFrame = now; postInvalidateOnAnimation(); }
        }
        private int setting(String key, int fallback) { return prefs == null ? fallback : prefs.getInt(key, fallback); }
        @Override protected void onDraw(Canvas canvas) {
            long now = android.os.SystemClock.uptimeMillis();
            float dt = Math.min(0.04f, (now - lastFrame) / 1000f);
            lastFrame = now;
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                float age = (now - p.born) / p.life;
                if (age >= 1) { particles.remove(i); continue; }
                p.x += p.vx * dt; p.y += p.vy * dt; p.vy += 130 * dt;
                p.angle += p.spin * dt;
                paint.setColor(p.color); paint.setAlpha((int) (255 * (1 - age)));
                canvas.save(); canvas.translate(p.x, p.y); canvas.rotate(p.angle);
                float scale = p.size * (1 - age * 0.6f);
                canvas.scale(scale, scale);
                if (p.kind == 1) canvas.drawPath(heart, paint);
                else if (p.kind == 2) canvas.drawCircle(0, 0, 0.65f, paint);
                else if (p.kind == 3) {
                    paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(0.12f);
                    canvas.drawCircle(0, 0, 0.9f + age, paint); paint.setStyle(Paint.Style.FILL);
                } else canvas.drawPath(star, paint);
                canvas.restore();
            }
            drawing = !particles.isEmpty();
            if (drawing) postInvalidateOnAnimation();
        }
    }

    public static void main(String[] args) {
        try {
            int seconds = args.length == 0 ? 20 : Integer.parseInt(args[0]);
            if (seconds < 5 || seconds > 120) throw new IllegalArgumentException("5..120 seconds only");
            Looper.prepareMainLooper();
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("systemMain").invoke(null);
            Context context = (Context) at.getMethod("getSystemContext").invoke(thread);
            wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            view = new Sparks(context);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2015,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.setTitle("MagicFingerPrototype");
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            wm.addView(view, lp);
            attached = true;
            Class<?> im = Class.forName("android.hardware.input.InputManagerGlobal");
            Object manager = im.getMethod("getInstance").invoke(null);
            monitor = im.getMethod("monitorGestureInput", String.class, int.class).invoke(manager, "MagicFingerPrototype", 0);
            InputChannel channel = (InputChannel) monitor.getClass().getMethod("getInputChannel").invoke(monitor);
            receiver = new InputEventReceiver(channel, Looper.getMainLooper()) {
                @Override public void onInputEvent(InputEvent event) {
                    try {
                        if (event instanceof MotionEvent) { events++; view.touch((MotionEvent) event); }
                    } finally { finishInputEvent(event, false); }
                }
            };
            System.out.println("READY duration=" + seconds + "s non-touchable overlay, passive input monitor");
            System.out.flush();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() {
                    cleanup();
                    System.out.println("FINISHED events=" + events);
                    System.out.flush();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, seconds * 1000L);
            Looper.loop();
        } catch (Throwable error) {
            error.printStackTrace();
            cleanup();
            System.err.flush();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
    private static void cleanup() {
        if (receiver != null) { receiver.dispose(); receiver = null; }
        if (monitor != null) {
            try { monitor.getClass().getMethod("dispose").invoke(monitor); }
            catch (Throwable error) { error.printStackTrace(); }
            monitor = null;
        }
        if (wm != null && view != null && attached) {
            try { wm.removeViewImmediate(view); } catch (Throwable error) { error.printStackTrace(); }
            view = null;
            attached = false;
        }
    }
}
