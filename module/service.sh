#!/system/bin/sh
MODDIR=${0%/*}
mkdir -p "$MODDIR/runtime"
chmod 700 "$MODDIR/runtime"
BOOT=$(cat /proc/sys/kernel/random/boot_id)
LOCK="$MODDIR/runtime/$BOOT.lock"
mkdir "$LOCK" 2>/dev/null || exit 0
trap 'rmdir "$LOCK" 2>/dev/null' EXIT
until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 3; done
export CLASSPATH="$MODDIR/classes.dex"
while [ -d "$MODDIR" ] && [ ! -f "$MODDIR/disable" ] && [ ! -f "$MODDIR/remove" ]; do
    sleep 5
    # No monitor while screen-off; Awake is NOT an unlocked-state check.
    dumpsys power | grep -q 'mWakefulness=Awake' || continue
    UID_APP=$(cmd package list packages -U magic.finger | sed -n 's/^package:magic.finger uid:\([0-9]*\)$/\1/p')
    [ -n "$UID_APP" ] || continue
    # am is a wrapper of "cmd activity"; its stdout is written by system_server,
    # which SELinux forbids writing into the module dir. Pipe through cat so the
    # root-side shell performs the file write instead.
    am start-foreground-service --user 0 -n magic.finger/.ParticleService 2>&1 \
        | cat > "$MODDIR/runtime/start.log"
    sleep 1
    app_process /system/bin magic.finger.TouchRelay magic-finger-module-v1 "$UID_APP" > "$MODDIR/runtime/relay.log" 2>&1 &
    RELAY=$!
    while kill -0 "$RELAY" 2>/dev/null; do
        sleep 2
        if ! dumpsys power | grep -q 'mWakefulness=Awake'; then
            kill "$RELAY" 2>/dev/null
            break
        fi
        [ -f "$MODDIR/disable" ] || [ -f "$MODDIR/remove" ] && { kill "$RELAY" 2>/dev/null; break; }
    done
    wait "$RELAY"
done
