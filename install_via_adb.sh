#!/system/bin/sh
# =============================================================
#  install_via_adb.sh
#  Manual Magisk module installation script via ADB shell
#  Run with: adb shell su -c "sh /sdcard/install_via_adb.sh"
# =============================================================

MODULE_ID="force_sim_network_mode"
MODULE_DIR="/data/adb/modules/$MODULE_ID"

echo "[*] Installing Force SIM Network Mode Rotator module..."
echo "[*] Module dir: $MODULE_DIR"

mkdir -p "$MODULE_DIR"

cat > "$MODULE_DIR/module.prop" << 'PROP'
id=force_sim_network_mode
name=Force SIM Network Mode Rotator
version=v2.0
versionCode=2
author=Custom
description=Rotates SIM1 4G and SIM2 3G/4G periodically. Samsung A7 2016
PROP

cat > "$MODULE_DIR/service.sh" << 'SCRIPT'
#!/system/bin/sh
LOG_FILE="/data/local/tmp/force_sim_mode.log"
CONF_FILE="/data/local/tmp/sim_switcher.conf"

log_msg() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"; }

if [ ! -f "$CONF_FILE" ]; then
    cat > "$CONF_FILE" << 'CONF'
SIM1_TIME=900
SIM2_TIME=180
SIM1_4G_MODE=11
SIM2_3G_MODE=9
ENABLE_ROTATION=1
CONF
fi

log_msg "=== Force SIM Network Mode Rotator: Started ==="
sleep 30

RETRY=0
while ! settings get global preferred_network_mode1 > /dev/null 2>&1; do
    sleep 5
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge 12 ]; then
        log_msg "ERROR: timeout"
        exit 1
    fi
done

toggle_airplane_mode() {
    log_msg "Toggling Airplane Mode..."
    settings put global airplane_mode_on 1
    am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true 2>/dev/null
    sleep 4
    settings put global airplane_mode_on 0
    am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false 2>/dev/null
    log_msg "Airplane Mode toggled."
}

while true; do
    [ -f "$CONF_FILE" ] && . "$CONF_FILE"
    : ${SIM1_TIME:=900}
    : ${SIM2_TIME:=180}
    : ${SIM1_4G_MODE:=11}
    : ${SIM2_3G_MODE:=9}
    : ${ENABLE_ROTATION:=1}

    # Phase 1: SIM1 4G, SIM2 2G
    log_msg "[Phase 1] SIM1=$SIM1_4G_MODE (4G), SIM2=1 (2G)"
    settings put global preferred_network_mode1 $SIM1_4G_MODE
    settings put global preferred_network_mode2 1
    toggle_airplane_mode
    sleep $SIM1_TIME

    if [ "$ENABLE_ROTATION" != "1" ]; then
        continue
    fi

    # Phase 2: SIM1 2G, SIM2 3G/4G
    log_msg "[Phase 2] SIM1=1 (2G), SIM2=$SIM2_3G_MODE (3G/4G)"
    settings put global preferred_network_mode1 1
    settings put global preferred_network_mode2 $SIM2_3G_MODE
    toggle_airplane_mode
    sleep $SIM2_TIME
done
SCRIPT

chmod 755 "$MODULE_DIR/service.sh"
chmod 644 "$MODULE_DIR/module.prop"

rm -f "$MODULE_DIR/disable"
rm -f "$MODULE_DIR/remove"

echo "[*] Done! Module installed at $MODULE_DIR"
echo "[!] Reboot now to activate: adb reboot"
