#!/usr/bin/env bash
set -u
PKG=com.dordeorz.kronometre
mkdir -p smoke
status=0
for apk in app/build/outputs/apk/release/*.apk app/build/outputs/apk/debug/*.apk; do
  name=$(basename "$apk" .apk)
  adb uninstall "$PKG" >/dev/null 2>&1
  adb install -r "$apk" || { status=1; continue; }
  adb logcat -c
  adb shell am start -W -n "$PKG/.MainActivity"
  sleep 15
  adb logcat -d > "smoke/$name.txt"
  adb logcat -d -b crash > "smoke/$name-crash.txt"
  if grep -q "FATAL EXCEPTION" "smoke/$name.txt" "smoke/$name-crash.txt" || ! adb shell pidof "$PKG" >/dev/null; then
    echo "::error::$name acilista coktu"
    grep -A 40 "FATAL EXCEPTION" "smoke/$name.txt" | head -80
    status=1
  else
    echo "$name acildi"
  fi
done
exit $status
