#!/bin/bash

# Clone kernel_xiaomi_sm8250
if [ ! -d "kernel/xiaomi/alioth" ]; then
    git clone https://github.com/kvsnr113/xiaomi_sm8250_kernel kernel/xiaomi/sm8250 -b stable-bpf --depth 1
fi

# Clone hardware_xiaomi
if [ ! -d "hardware/xiaomi" ]; then
    git clone https://github.com/zen0s-aospforge/hardware_xiaomi hardware/xiaomi -b 16
fi

# Clone hardware_dolby
if [ ! -d "hardware/dolby" ]; then
    git clone https://github.com/zen0s-aospforge/hardware_dolby hardware/dolby -b c2
fi

# Clone vendor_xiaomi_alioth
if [ ! -d "vendor/xiaomi/alioth" ]; then
    git clone https://github.com/zen0s-aospforge/vendor_xiaomi_alioth vendor/xiaomi/alioth -b 16 --depth 1
fi

# Clone proprietary_vendor_xiaomi_sm8250-common
if [ ! -d "vendor/xiaomi/sm8250-common" ]; then
    git clone https://github.com/zen0s-aospforge/proprietary_vendor_xiaomi_sm8250-common vendor/xiaomi/sm8250-common -b 16 --depth 1
fi

# Clone packages_apps_GameBar
if [ ! -d "packages/apps/GameBar" ]; then
    git clone https://github.com/zen0s-aospforge/packages_apps_GameBar packages/apps/GameBar -b main
fi

# Clone vendor_xiaomi_camera
if [ ! -d "vendor/xiaomi/camera" ]; then
    git clone https://gitlab.com/johnmart19/vendor_xiaomi_camera vendor/xiaomi/camera -b aosp-16 --depth 1
fi

# Clone device_xiaomi_camera
if [ ! -d "device/xiaomi/camera" ]; then
    git clone https://github.com/PocoF3Releases/device_xiaomi_camera device/xiaomi/camera -b aosp-16 --depth 1
fi


# Script to apply Binder threadpool patch
(
set -e

echo "Applying Binder threadpool patch..."

if [ ! -d "system/libhwbinder" ]; then
    echo "Warning: system/libhwbinder directory not found."
    echo "Please run 'repo sync' first to fetch the source code."
    exit 1
fi

# Patch 1: system/libhwbinder
cd system/libhwbinder
echo "Fetching commit from custom-crdroid repository..."
if git fetch https://github.com/custom-crdroid/system_libhwbinder.git d9d46e78cec0d09498fd5890eed9f7195baed0fd 2>/dev/null; then
    echo "Applying commit d9d46e78cec0d09498fd5890eed9f7195baed0fd..."
    if git cherry-pick d9d46e78cec0d09498fd5890eed9f7195baed0fd 2>/dev/null; then
        echo "✅ Successfully applied Binder threadpool patch!"
    else
        echo "⚠️ Warning: Failed to cherry-pick libhwbinder commit. It may already be applied or have conflicts."
        echo "   Aborting this patch and continuing..."
        git cherry-pick --abort 2>/dev/null || true
    fi
else
    echo "⚠️ Warning: Failed to fetch libhwbinder commit from repository. Skipping this patch."
fi
cd - > /dev/null
)

# Script to apply bionic patch
if [ -d "bionic" ]; then
    cd bionic
    echo "Fetching commit from yaap/bionic repository..."
    if git fetch https://github.com/yaap/bionic.git 2c0a9fb575df103aef7cb257ff6f2699898a3f9c 2>/dev/null; then
        echo "Applying commit 2c0a9fb575df103aef7cb257ff6f2699898a3f9c..."
        if git cherry-pick 2c0a9fb575df103aef7cb257ff6f2699898a3f9c 2>/dev/null; then
            echo "✅ Successfully applied bionic patch!"
        else
            echo "⚠️ Warning: Failed to cherry-pick bionic commit. It may already be applied or have conflicts."
            echo "   Aborting this patch and continuing..."
            git cherry-pick --abort 2>/dev/null || true
        fi
    else
        echo "⚠️ Warning: Failed to fetch bionic commit from repository. Skipping this patch."
    fi
    cd - > /dev/null
else
    echo "⚠️ Warning: bionic directory not found. Skipping bionic patch."
fi
