#!/bin/sh
set -eu
cd "$(dirname "$0")"
SDK=/home/elder/Android/Sdk
JDK=/usr/lib/jvm/java-8-temurin
mkdir -p build/stubs build/classes dist
"$JDK/bin/javac" -cp "$SDK/platforms/android-34/android.jar" -d build/stubs stubs/android/view/*.java
"$JDK/bin/javac" -encoding UTF-8 -cp "build/stubs:$SDK/platforms/android-34/android.jar" -d build/classes src/*.java
# Compile-only hidden API stubs are deliberately NOT packaged in the dex.
find build/classes -name '*.class' > build/classes.list
"$SDK/build-tools/34.0.0/d8" --min-api 34 --lib "$SDK/platforms/android-34/android.jar" --classpath build/stubs --output dist @build/classes.list
"$SDK/build-tools/34.0.0/aapt" package -f -M AndroidManifest.xml -I "$SDK/platforms/android-34/android.jar" -F build/unsigned.apk
cp dist/classes.dex build/classes.dex
(cd build && "$SDK/build-tools/34.0.0/aapt" add unsigned.apk classes.dex)
if [ ! -f build/debug.keystore ]; then
    /usr/lib/jvm/java-17-temurin/bin/keytool -genkeypair -keystore build/debug.keystore -storepass android -keypass android -alias debug -dname 'CN=Magic Finger Experiment' -keyalg RSA -validity 3650
fi
"$SDK/build-tools/34.0.0/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$SDK/build-tools/34.0.0/apksigner" sign --ks build/debug.keystore --ks-pass pass:android --key-pass pass:android --out dist/magic-finger-experimental.apk build/aligned.apk
"$SDK/build-tools/34.0.0/apksigner" verify dist/magic-finger-experimental.apk
