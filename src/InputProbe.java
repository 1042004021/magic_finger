import java.lang.reflect.Method;

/** Method inventory only: no input subscription or window creation. */
public final class InputProbe {
    public static void main(String[] args) throws Exception {
        for (String name : new String[]{"android.hardware.input.InputManagerGlobal",
                "android.hardware.input.InputManager", "android.view.InputEventReceiver",
                "android.view.InputMonitor", "android.view.WindowManager$LayoutParams"}) {
            System.out.println("CLASS " + name);
            try {
                Class<?> c = Class.forName(name);
                for (Method m : c.getDeclaredMethods()) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("monitor") || n.contains("instance") || n.contains("dispose")
                            || n.contains("inputchannel") || n.contains("finishinput")) System.out.println(m);
                }
                if (name.endsWith("InputEventReceiver")) {
                    for (java.lang.reflect.Constructor<?> constructor : c.getDeclaredConstructors()) {
                        System.out.println(constructor);
                    }
                }
            } catch (Throwable error) { System.out.println(error); }
        }
    }
}
