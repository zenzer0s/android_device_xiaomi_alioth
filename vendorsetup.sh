#!/bin/bash

base64 -d device/xiaomi/alioth/configs/camera/secret > device/xiaomi/alioth/configs/camera/st_license.lic

git clone https://github.com/PocoF3Releases/device_xiaomi_camera device/xiaomi/camera -b aosp-16 --depth 1
git clone https://github.com/Meow-prjkt/android_hardware_dolby.git hardware/dolby --depth 1
git clone https://github.com/zen0s-aospforge/hardware_xiaomi hardware/xiaomi -b 16
git clone https://github.com/Meow-prjkt/android_kernel_xiaomi_sm8250.git kernel/xiaomi/sm8250 --depth 1
git clone https://github.com/kenway214/packages_apps_GameBar.git packages/apps/GameBar
git clone https://github.com/KProfiles/android_packages_apps_KProfiles packages/apps/KProfiles
git clone https://github.com/zenzer0s/android_vendor_xiaomi_alioth.git vendor/xiaomi/alioth -b main --depth 1
git clone https://gitlab.com/johnmart19/vendor_xiaomi_camera vendor/xiaomi/camera -b aosp-16 --depth 1

# Apply Binder threadpool patch
if [ -d "system/libhwbinder" ]; then
    git -C system/libhwbinder fetch https://github.com/custom-crdroid/system_libhwbinder.git d9d46e78cec0d09498fd5890eed9f7195baed0fd
    git -C system/libhwbinder cherry-pick FETCH_HEAD || git -C system/libhwbinder cherry-pick --abort
fi
