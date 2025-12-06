#!/bin/bash

base64 -d device/xiaomi/alioth/configs/camera/secret > device/xiaomi/alioth/configs/camera/st_license.lic

# Clone kernel_xiaomi_sm8250
if [ ! -d "kernel/xiaomi/alioth" ]; then
    git clone https://github.com/Meow-prjkt/android_kernel_xiaomi_sm8250.git kernel/xiaomi/sm8250 --depth 1
fi

# Clone hardware_xiaomi
if [ ! -d "hardware/xiaomi" ]; then
    git clone https://github.com/zen0s-aospforge/hardware_xiaomi hardware/xiaomi -b 16
fi

# Clone hardware_dolby
if [ ! -d "hardware/dolby" ]; then
    git clone https://github.com/Meow-prjkt/android_hardware_dolby.git hardware/dolby --depth 1
fi

# Clone Kprofiles
if [ ! -d "packages/apps/KProfiles" ]; then
    git clone https://github.com/KProfiles/android_packages_apps_KProfiles packages/apps/KProfiles
fi

# Clone vendor_xiaomi_alioth
if [ ! -d "vendor/xiaomi/alioth" ]; then
    git clone https://github.com/zenzer0s/android_vendor_xiaomi_alioth.git vendor/xiaomi/alioth -b main --depth 1
fi

# Clone packages_apps_GameBar
if [ ! -d "packages/apps/GameBar" ]; then
    git clone https://github.com/zen0s-aospforge/packages_apps_GameBar packages/apps/GameBar
fi

# Clone vendor_xiaomi_camera
if [ ! -d "vendor/xiaomi/camera" ]; then
    git clone https://gitlab.com/johnmart19/vendor_xiaomi_camera vendor/xiaomi/camera -b aosp-16 --depth 1
fi

# Clone device_xiaomi_camera
if [ ! -d "device/xiaomi/camera" ]; then
    git clone https://github.com/PocoF3Releases/device_xiaomi_camera device/xiaomi/camera -b aosp-16 --depth 1
fi


# Apply Binder threadpool patch
if [ -d "system/libhwbinder" ]; then
    cd system/libhwbinder
    git fetch https://github.com/custom-crdroid/system_libhwbinder.git d9d46e78cec0d09498fd5890eed9f7195baed0fd
    git cherry-pick d9d46e78cec0d09498fd5890eed9f7195baed0fd || git cherry-pick --abort
    cd - > /dev/null
fi

