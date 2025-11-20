#
# Copyright (C) 2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common mica stuff.
$(call inherit-product, vendor/mica/config/common_full_phone.mk)

# Inherit from alioth device
$(call inherit-product, device/xiaomi/alioth/device.mk)

TARGET_INCLUDE_REVANCED := false

# Device identifier
PRODUCT_NAME := mica_alioth
PRODUCT_DEVICE := alioth
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := POCO
PRODUCT_MODEL := POCO F3

PRODUCT_SYSTEM_NAME := alioth_global
PRODUCT_SYSTEM_DEVICE := alioth

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=POCO/alioth_global/alioth:13/TKQ1.221114.001/V816.0.3.0.TKHMIXM:user/release-keys \
    DeviceName=$(PRODUCT_SYSTEM_DEVICE) \
    DeviceProduct=$(PRODUCT_SYSTEM_NAME)
