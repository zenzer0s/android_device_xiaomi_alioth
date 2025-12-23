package org.lineageos.xiaomiparts.data

// HBM
const val PREF_HBM_KEY = "hbm"
const val PREF_AUTO_HBM_KEY = "auto_hbm"
const val PREF_AUTO_HBM_THRESHOLD_KEY = "auto_hbm_threshold"
const val PREF_HBM_DISABLE_TIME_KEY = "hbm_disable_time"
const val PREF_HBM_SAVED_BRIGHTNESS_MODE = "hbm_saved_mode"
const val PREF_HBM_SAVED_BRIGHTNESS_VALUE = "hbm_saved_value"
const val PREF_HBM_OWNER_KEY = "hbm_owner"

const val HBM_SYSFS_PATH = "/sys/class/drm/card0/card0-DSI-1/disp_param"
const val BACKLIGHT_SYSFS_PATH = "/sys/class/backlight/panel0-backlight/brightness"

// DC dimming
const val PREF_DC_DIMMING_KEY = "dc_dimming_enable"
const val DC_DIMMING_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/dimlayer_exposure"

// Thermal
const val PREF_THERMAL_CONTROL = "thermal_control_v2"
const val PREF_THERMAL_ENABLED = "thermal_enabled"
const val THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig"

// Charge
const val PREF_BYPASS_CHARGE = "bypass_charge"
const val BYPASS_CHARGE_NODE = "/sys/class/power_supply/battery/input_suspend"

// ReVanced
const val PROPERTY_REVANCED_ENABLED = "persist.sys.revan.mod"
const val PROPERTY_REVANCED_AVAILABLE = "ro.revanced.available"