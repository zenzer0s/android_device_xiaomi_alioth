#!/bin/bash

base64 -d device/xiaomi/alioth/configs/camera/secret > device/xiaomi/alioth/configs/camera/st_license.lic

# Clone kernel_xiaomi_sm8250
git clone -b 16-R404 https://github.com/Meow-prjkt/android_kernel_xiaomi_sm8250.git kernel/xiaomi/sm8250 --depth 1 && cd kernel/xiaomi/sm8250 && sed -i '/kernelsu/d' drivers/Kconfig && cd - > /dev/null

# Clone hardware_xiaomi
git clone https://github.com/Meow-prjkt/android_hardware_xiaomi.git hardware/xiaomi

# Clone hardware_dolby
git clone https://github.com/Meow-prjkt/android_hardware_dolby.git hardware/dolby --depth 1


# Clone vendor_xiaomi_alioth
git clone https://github.com/zenzer0s/android_vendor_xiaomi_alioth.git vendor/xiaomi/alioth -b main --depth 1

# Clone packages_apps_GameBar
git clone https://github.com/kenway214/packages_apps_GameBar.git packages/apps/GameBar

# Clone vendor_xiaomi_camera
git clone https://gitlab.com/johnmart19/vendor_xiaomi_camera vendor/xiaomi/camera -b aosp-16 --depth 1

sed -i '/vendor\/xiaomi\/sm8250-common/d' vendor/xiaomi/camera/Android.bp

# Clone device_xiaomi_camera
git clone https://github.com/PocoF3Releases/device_xiaomi_camera device/xiaomi/camera -b aosp-16 --depth 1


# Clone vendor_infinity-priv_keys
git clone https://github.com/ProjectInfinity-X/vendor_infinity-priv_keys vendor/infinity-priv/keys


# Apply Binder threadpool patch
cd system/libhwbinder
git fetch https://github.com/custom-crdroid/system_libhwbinder.git d9d46e78cec0d09498fd5890eed9f7195baed0fd
git cherry-pick d9d46e78cec0d09498fd5890eed9f7195baed0fd
cd - > /dev/null
