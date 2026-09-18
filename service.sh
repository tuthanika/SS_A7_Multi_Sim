#!/system/bin/sh
# =============================================================
#  Force & Rotate SIM Network Mode - Magisk Module
#  Target: Samsung Galaxy A7 2016 (SM-A710) / Android 7.1
# =============================================================

LOG_FILE="/data/local/tmp/force_sim_mode.log"
CONF_FILE="/data/local/tmp/sim_switcher.conf"

log_msg() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# Create default config if not existing
if [ ! -f "$CONF_FILE" ]; then
    cat > "$CONF_FILE" << 'CONF'
# Configuration for SIM Network Mode Rotator
# SIM1_TIME: Seconds SIM1 is on 4G LTE (Default: 900 = 15 mins)
SIM1_TIME=900
# SIM2_TIME: Seconds SIM2 is on 3G/4G (Default: 180 = 3 mins)
SIM2_TIME=180
# SIM1 4G mode (11 = LTE Only, 9 = LTE/3G/2G auto)
SIM1_4G_MODE=11
# SIM2 3G/4G mode (9 = LTE/3G/2G auto, 2 = 3G Only)
SIM2_3G_MODE=9
# Enable rotation (1 = Yes, 0 = No - lock SIM1 to 4G)
ENABLE_ROTATION=1
CONF
fi

log_msg "=== Force SIM Network Mode Rotator: Started ==="

# Wait for settings service
sleep 30
RETRY=0
while ! settings get global preferred_network_mode1 > /dev/null 2>&1; do
    sleep 5
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge 12 ]; then
        log_msg "ERROR: settings service not available after 90s, aborting."
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

# Main Rotation Daemon Loop
while true; do
    # Reload config if updated
    [ -f "$CONF_FILE" ] && . "$CONF_FILE"
    
    : ${SIM1_TIME:=900}
    : ${SIM2_TIME:=180}
    : ${SIM1_4G_MODE:=11}
    : ${SIM2_3G_MODE:=9}
    : ${ENABLE_ROTATION:=1}

    # --- Phase 1: SIM 1 = 4G Only, SIM 2 = 2G Only ---
    log_msg "[Phase 1] Setting SIM1=$SIM1_4G_MODE (4G), SIM2=1 (2G)"
    settings put global preferred_network_mode1 $SIM1_4G_MODE
    settings put global preferred_network_mode2 1
    toggle_airplane_mode
    
    log_msg "[Phase 1] Sleeping for ${SIM1_TIME}s..."
    sleep $SIM1_TIME

    # If rotation is disabled, continue loop Phase 1
    if [ "$ENABLE_ROTATION" != "1" ]; then
        continue
    fi

    # --- Phase 2: SIM 1 = 2G Only, SIM 2 = 3G/4G Auto ---
    log_msg "[Phase 2] Setting SIM1=1 (2G), SIM2=$SIM2_3G_MODE (3G/4G)"
    settings put global preferred_network_mode1 1
    settings put global preferred_network_mode2 $SIM2_3G_MODE
    toggle_airplane_mode

    log_msg "[Phase 2] Sleeping for ${SIM2_TIME}s..."
    sleep $SIM2_TIME
done
