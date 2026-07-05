#!/bin/bash

base64 -d device/xiaomi/alioth/configs/camera/secret > device/xiaomi/alioth/configs/camera/st_license.lic 2>/dev/null || true

# Clone kernel_xiaomi_sm8250
if [ ! -d "kernel/xiaomi/sm8250" ]; then
    git clone https://github.com/TIMISONG-dev/kernel_xiaomi_sm8250.git kernel/xiaomi/sm8250  --depth=1 && cd kernel/xiaomi/sm8250 && sed -i '/kernelsu/d' drivers/Kconfig && cd - > /dev/null
fi

# Clone hardware_xiaomi
if [ ! -d "hardware/xiaomi" ]; then
    git clone https://github.com/Sanjis-Android-Playground/hardware_xiaomi hardware/xiaomi -b aosp-16
fi

# Clone hardware_dolby
if [ ! -d "hardware/dolby" ]; then
    git clone https://github.com/Meow-prjkt/android_hardware_dolby.git hardware/dolby --depth 1
fi

# Clone vendor_xiaomi_alioth
if [ ! -d "vendor/xiaomi/alioth" ]; then
    git clone https://github.com/zenzer0s/android_vendor_xiaomi_alioth.git vendor/xiaomi/alioth -b main --depth 1
fi

# Clone packages_apps_GameBar
if [ ! -d "packages/apps/GameBar" ]; then
    git clone https://github.com/Sanjis-Android-Playground/packages_apps_GameBar.git  packages/apps/GameBar/
fi

# Clone vendor_xiaomi_camera
if [ ! -d "vendor/xiaomi/camera" ]; then
    git clone https://gitlab.com/johnmart19/vendor_xiaomi_camera vendor/xiaomi/camera -b aosp-16 --depth 1
    sed -i '/vendor\/xiaomi\/sm8250-common/d' vendor/xiaomi/camera/Android.bp 2>/dev/null || true
fi

# Clone device_xiaomi_camera
if [ ! -d "device/xiaomi/camera" ]; then
    git clone https://github.com/PocoF3Releases/device_xiaomi_camera device/xiaomi/camera -b aosp-16 --depth 1
fi

# Apply Binder threadpool patch
if [ -d "system/libhwbinder" ]; then
    cd system/libhwbinder
    if ! git log -n 50 | grep -q "Binder threadpool"; then
        git fetch https://github.com/custom-crdroid/system_libhwbinder.git d9d46e78cec0d09498fd5890eed9f7195baed0fd 2>/dev/null || true
        git cherry-pick d9d46e78cec0d09498fd5890eed9f7195baed0fd 2>/dev/null || true
    fi
    cd - > /dev/null
fi