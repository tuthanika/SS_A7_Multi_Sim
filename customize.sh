#!/system/bin/sh
# customize.sh
# Called by Magisk after it extracts the ZIP to MODPATH.
# MODPATH, MAGISK_VER_CODE, ARCH, etc. are available.

ui_print "************************************"
ui_print " Force SIM Network Mode - v1.0      "
ui_print "************************************"
ui_print " SIM1 -> 4G Only (LTE Only)"
ui_print " SIM2 -> 3G Only (WCDMA Only)"
ui_print ""
ui_print " Reboot to activate!"
ui_print " Log: /data/local/tmp/force_sim_mode.log"
ui_print "************************************"

# Set executable permission on service.sh
chmod 0755 "$MODPATH/service.sh"
