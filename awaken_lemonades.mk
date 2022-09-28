#
# Copyright (C) 2018 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from lemonades device
$(call inherit-product, device/oneplus/lemonades/device.mk)

# Inherit some common awaken stuff.
$(call inherit-product, vendor/awaken/config/common_full_phone.mk)

# Inherit OnePlus Lab
$(call inherit-product, vendor/OnePlusLab/OnePlusLab.mk)

# Boot Animation
TARGET_BOOT_ANIMATION_RES := 1080

# Face Unlock
TARGET_FACE_UNLOCK_SUPPORTED := true

# Google Recorder
TARGET_SUPPORTS_GOOGLE_RECORDER := true

# Live Wallpapers
TARGET_INCLUDE_LIVE_WALLPAPERS := true

# Next Gen Assistant
TARGET_SUPPORTS_NEXT_GEN_ASSISTANT := true

# Official
AWAKEN_BUILD_TYPE := official

# Quick Tap
TARGET_SUPPORTS_QUICK_TAP := true

# Stock ARCore
TARGET_INCLUDE_STOCK_ARCORE := true

PRODUCT_NAME := awaken_lemonades
PRODUCT_DEVICE := lemonades
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := LE2101

PRODUCT_SYSTEM_NAME := OnePlus9R
PRODUCT_SYSTEM_DEVICE := OnePlus9R

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

PRODUCT_BUILD_PROP_OVERRIDES += \
    PRIVATE_BUILD_DESC="OnePlus9R_IND-user 12 RKQ1.211119.001 R.202206110121 release-keys" \
    TARGET_DEVICE=$(PRODUCT_SYSTEM_DEVICE) \
    TARGET_PRODUCT=$(PRODUCT_SYSTEM_NAME)

BUILD_FINGERPRINT := OnePlus/OnePlus9R_IND/OnePlus9R:12/RKQ1.211119.001/R.202206110121:user/release-keys
