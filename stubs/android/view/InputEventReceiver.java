package android.view;
public abstract class InputEventReceiver {
    public InputEventReceiver(InputChannel channel, android.os.Looper looper) { }
    public void onInputEvent(InputEvent event) { }
    public final void finishInputEvent(InputEvent event, boolean handled) { }
    public void dispose() { }
}
