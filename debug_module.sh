#!/system/bin/sh
# debug_module.sh - Run with root to diagnose module install issue
# adb shell su -c "sh /sdcard/debug_module.sh"

echo "=== Magisk Module Debug ==="
echo ""

echo "[1] /data/adb/ contents:"
ls -la /data/adb/ 2>&1
echo ""

echo "[2] /data/adb/modules/ contents:"
ls -la /data/adb/modules/ 2>&1
echo ""

echo "[3] /data/adb/modules_update/ contents:"
ls -la /data/adb/modules_update/ 2>&1
echo ""

echo "[4] Target module dir (if exists):"
ls -la /data/adb/modules/force_sim_network_mode/ 2>&1
echo ""

echo "[5] Magisk version:"
magisk -v 2>&1
magisk --version 2>&1
echo ""

echo "[6] Magisk log (last 30 lines):"
cat /cache/magisk.log 2>/dev/null | tail -30
logcat -d -t 30 | grep -i magisk 2>/dev/null | tail -20

echo ""
echo "=== End Debug ==="
