#!/system/bin/sh
[ "$API" -ge 34 ] || abort "本实验版本要求 Android 14+；仅实测 Android16"
ui_print "请安装配套 APK，授予悬浮窗权限并打开总开关"
ui_print "模块不会自动安装 APK，不修改系统框架或 SELinux"
set_perm_recursive "$MODPATH" 0 0 0700 0600
set_perm "$MODPATH/service.sh" 0 0 0700
