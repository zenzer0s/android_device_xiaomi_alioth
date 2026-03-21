#
# Copyright (C) 2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common lineage stuff.
TARGET_DISABLE_EPPE := true
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Inherit from alioth device
$(call inherit-product, device/xiaomi/alioth/device.mk)

# Maintainer
AXION_MAINTAINER := zenzer0s

# Blur effect
TARGET_ENABLE_BLUR := true

# Camera
TARGET_USES_MIUI_CAMERA := true
TARGET_INCLUDES_MIUI_CAMERA := true
AXION_CAMERA_REAR_INFO := 48,8,5
AXION_CAMERA_FRONT_INFO := 20

# CPU
AXION_PROCESSOR := Snapdragon_870_5G
PERF_GOV_SUPPORTED := true
PERF_DEFAULT_GOV := schedutil
PERF_ANIM_OVERRIDE := false

# GPU
GPU_FREQS_PATH := /sys/class/kgsl/kgsl-3d0/freq_table_mhz
GPU_MIN_FREQ_PATH := /sys/class/kgsl/kgsl-3d0/min_clock_mhz

# Display
TARGET_SUPPORTED_REFRESH_RATES := 60,120
TARGET_TOUCH_BOOST_SUPPORTED := true

# High Brightness Mode (HBM)
HBM_SUPPORTED := true
HBM_NODE := /sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm

# Flashlight strength
TORCH_STR_SUPPORTED := true

# doze fix
# for devices with doze/sensor related issues
TARGET_NEEDS_DOZE_FIX := false

# Charging
BYPASS_CHARGE_SUPPORTED := true

# LineageOS apps
ifneq ($(WITH_GMS),true)
TARGET_INCLUDES_LOS_PREBUILTS := true
endif

# BCR
TARGET_PREBUILT_BCR := true

PRODUCT_NAME := lineage_alioth
PRODUCT_DEVICE := alioth
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := POCO
PRODUCT_MODEL := POCO F3

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=google/husky_beta/husky:16/BP41.250822.010/14082742:user/release-keys