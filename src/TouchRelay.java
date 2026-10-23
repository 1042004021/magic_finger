package magic.finger;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.Looper;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import java.io.DataOutputStream;

/** Passive root input monitor. Never pilfers, injects, or writes touch traces. */
public final class TouchRelay {
    private static InputEventReceiver receiver;
    private static Object monitor;
    private static LocalSocket socket;
    private static long events;
    private static void finish() {
        try { if (receiver != null) receiver.dispose(); } catch (Exception ignored) { }
        try { if (monitor != null) monitor.getClass().getMethod("dispose").invoke(monitor); } catch (Exception ignored) { }
        try { if (socket != null) socket.close(); } catch (Exception ignored) { }
        System.out.println("RELAY_FINISHED events=" + events);
        System.out.flush();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    public static void main(String[] args) {
        try {
            boolean persistent = args.length == 2 && args[0].equals("magic-finger-module-v1");
            if (!persistent && (args.length != 1 || !args[0].matches("magic-finger-[a-f0-9]{32}"))) throw new IllegalArgumentException("socket");
            Looper.prepareMainLooper();
            socket = new LocalSocket();
            socket.connect(new LocalSocketAddress(args[0], LocalSocketAddress.Namespace.ABSTRACT));
            if (persistent && socket.getPeerCredentials().getUid() != Integer.parseInt(args[1])) {
                throw new SecurityException("Renderer UID mismatch");
            }
            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Class<?> c = Class.forName("android.hardware.input.InputManagerGlobal");
            Object manager = c.getMethod("getInstance").invoke(null);
            monitor = c.getMethod("monitorGestureInput", String.class, int.class).invoke(manager, "MagicFingerPassive", 0);
            InputChannel channel = (InputChannel) monitor.getClass().getMethod("getInputChannel").invoke(monitor);
            receiver = new InputEventReceiver(channel, Looper.getMainLooper()) {
                @Override public void onInputEvent(InputEvent event) {
                    try {
                        if (event instanceof MotionEvent) {
                            MotionEvent e = (MotionEvent) event;
                            int a = e.getActionMasked();
                            for (int i = 0; i < e.getPointerCount(); i++) {
                                out.writeInt(a); out.writeFloat(e.getX(i)); out.writeFloat(e.getY(i));
                            }
                            out.flush(); events++;
                        }
                    } catch (Exception error) { finish(); }
                    finally { if (receiver != null) finishInputEvent(event, false); }
                }
            };
            if (!persistent) new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() { finish(); }
            }, 25000);
            System.out.println("RELAY_READY passive=true persistent=" + persistent); System.out.flush();
            Looper.loop();
        } catch (Throwable error) { error.printStackTrace(); finish(); }
    }
}
