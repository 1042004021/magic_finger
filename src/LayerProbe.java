package magic.finger;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.view.Surface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;

/** Ten-second non-interactive composition-layer probe; no input capture. */
public final class LayerProbe {
    private static InputEventReceiver receiver;
    private static Object monitor;
    private static long events;
    public static void main(String[] args) {
        Object layer = null, tx = null;
        Surface surface = null;
        try {
            System.out.println("LAYER_STAGE start"); System.out.flush();
            Class<?> sc = Class.forName("android.view.SurfaceControl");
            Class<?> builderClass = Class.forName("android.view.SurfaceControl$Builder");
            Object builder = builderClass.getConstructor().newInstance();
            builderClass.getMethod("setName", String.class).invoke(builder, "MagicFingerHighLayerProbe");
            builderClass.getMethod("setBufferSize", int.class, int.class).invoke(builder, 1200, 2670);
            builderClass.getMethod("setFormat", int.class).invoke(builder, PixelFormat.TRANSLUCENT);
            layer = builderClass.getMethod("build").invoke(builder);
            System.out.println("LAYER_STAGE built"); System.out.flush();
            Class<?> tc = Class.forName("android.view.SurfaceControl$Transaction");
            tx = tc.getConstructor().newInstance();
            tc.getMethod("setLayer", sc, int.class).invoke(tx, layer, 2000000000);
            tc.getMethod("setLayerStack", sc, int.class).invoke(tx, layer, 0);
            tc.getMethod("show", sc).invoke(tx, layer);
            tc.getMethod("apply").invoke(tx);
            surface = Surface.class.getConstructor().newInstance();
            Surface.class.getMethod("copyFrom", sc).invoke(surface, layer);
            System.out.println("LAYER_STAGE surface"); System.out.flush();
            Canvas canvas = surface.lockCanvas(null);
            System.out.println("LAYER_STAGE canvas"); System.out.flush();
            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
            Paint paint = new Paint(3); paint.setColor(0xffff66cc);
            System.out.println("LAYER_STAGE paint"); System.out.flush();
            canvas.drawCircle(600, 240, 45, paint);
            // No text: this ROM aborts in default typeface initialization.
            paint.setColor(0xffffffff);
            canvas.drawCircle(600, 240, 18, paint);
            surface.unlockCanvasAndPost(canvas);
            if (args.length == 1 && "touch".equals(args[0])) {
                runTouchProbe(surface);
            } else {
                System.out.println("LAYER_READY duration=10s"); System.out.flush();
                Thread.sleep(10000);
            }
        } catch (Throwable error) { error.printStackTrace(); }
        finally {
            try { if (receiver != null) receiver.dispose(); } catch (Exception error) { error.printStackTrace(); }
            try { if (monitor != null) monitor.getClass().getMethod("dispose").invoke(monitor); }
            catch (Exception error) { error.printStackTrace(); }
            if (surface != null) surface.release();
            try {
                if (layer != null && tx != null) {
                    Class<?> sc = Class.forName("android.view.SurfaceControl");
                    tx.getClass().getMethod("remove", sc).invoke(tx, layer);
                    tx.getClass().getMethod("apply", boolean.class).invoke(tx, true);
                    sc.getMethod("release").invoke(layer);
                }
                if (tx != null) tx.getClass().getMethod("close").invoke(tx);
            } catch (Throwable error) { error.printStackTrace(); }
            System.out.println("LAYER_FINISHED events=" + events); System.out.flush();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    /** Isolated 25-second visual/input test, not a persistent renderer. */
    private static void runTouchProbe(final Surface surface) throws Exception {
        Looper.prepare();
        final Handler handler = new Handler(Looper.myLooper());
        final Paint ring = new Paint(3);
        ring.setStyle(Paint.Style.STROKE); ring.setStrokeWidth(8);
        final float[] position = new float[2];
        final long[] touched = new long[1];
        final boolean[] scheduled = new boolean[1];
        final Runnable frame = new Runnable() {
            @Override public void run() {
                Canvas canvas = null;
                try {
                    long age = SystemClock.uptimeMillis() - touched[0];
                    canvas = surface.lockCanvas(null);
                    canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                    if (age < 600) {
                        ring.setColor(0xffff66cc); ring.setAlpha((int) (255 * (1 - age / 600f)));
                        canvas.drawCircle(position[0], position[1], 35 + age * 0.08f, ring);
                    }
                    scheduled[0] = age < 600;
                    if (scheduled[0]) handler.postDelayed(this, 16);
                } catch (Throwable error) {
                    error.printStackTrace(); Looper.myLooper().quitSafely();
                } finally {
                    if (canvas != null) surface.unlockCanvasAndPost(canvas);
                }
            }
        };
        Class<?> im = Class.forName("android.hardware.input.InputManagerGlobal");
        Object manager = im.getMethod("getInstance").invoke(null);
        monitor = im.getMethod("monitorGestureInput", String.class, int.class)
                .invoke(manager, "MagicFingerHighLayerProbe", 0);
        InputChannel channel = (InputChannel) monitor.getClass().getMethod("getInputChannel").invoke(monitor);
        receiver = new InputEventReceiver(channel, Looper.myLooper()) {
            @Override public void onInputEvent(InputEvent event) {
                try {
                    if (event instanceof MotionEvent) {
                        events++;
                        MotionEvent motion = (MotionEvent) event;
                        int action = motion.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                            position[0] = motion.getX(); position[1] = motion.getY();
                            touched[0] = SystemClock.uptimeMillis();
                            if (!scheduled[0]) { scheduled[0] = true; handler.post(frame); }
                        }
                    }
                } finally { finishInputEvent(event, false); }
            }
        };
        // Clear the static marker before observing real touches.
        handler.post(frame);
        handler.postDelayed(() -> Looper.myLooper().quitSafely(), 25000);
        System.out.println("TOUCH_LAYER_READY duration=25s passive=true"); System.out.flush();
        Looper.loop();
        handler.removeCallbacksAndMessages(null);
    }
}
