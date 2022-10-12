LOCAL_PATH := $(call my-dir)

# Google Camera Go
include $(CLEAR_VARS)
LOCAL_MODULE := GoogleCameraGo
LOCAL_SRC_FILES := priv-app/GoogleCameraGo/GoogleCameraGo.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_PRIVILEGED_MODULE := true
LOCAL_OVERRIDES_PACKAGES := Snap Camera2
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_DEX_PREOPT := false
include $(BUILD_PREBUILT)
