LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH_ABI), armeabi)
LIBGSTREAMER_PATH        := armeabi


include $(CLEAR_VARS)
LOCAL_MODULE := liblive555rtsp
LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/liblive555rtsp.so
include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := libgstreamer_android
#LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/libgstreamer_android.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libnice4android
LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/libnice4android.so
include $(PREBUILT_SHARED_LIBRARY)

else ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
LIBGSTREAMER_PATH        := armeabi-v7a


include $(CLEAR_VARS)
LOCAL_MODULE := liblive555rtsp
LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/liblive555rtsp.so
include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := libgstreamer_android
#LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/libgstreamer_android.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libnice4android
LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/libnice4android.so
include $(PREBUILT_SHARED_LIBRARY)

else ifeq ($(TARGET_ARCH_ABI), x86)
LIBGSTREAMER_PATH        := x86


include $(CLEAR_VARS)
LOCAL_MODULE := liblive555rtsp
LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/liblive555rtsp.so
include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := libgstreamer_android
#LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/libgstreamer_android.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libnice4android
LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/libnice4android.so
include $(PREBUILT_SHARED_LIBRARY)

else ifeq ($(TARGET_ARCH_ABI), x86_64)
LIBGSTREAMER_PATH        := x86_64


include $(CLEAR_VARS)
LOCAL_MODULE := liblive555rtsp
LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/liblive555rtsp.so
include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := libgstreamer_android
#LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/libgstreamer_android.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libnice4android
LOCAL_SRC_FILES := $(LIBGSTREAMER_PATH)/libnice4android.so
include $(PREBUILT_SHARED_LIBRARY)

#else
#$(error Target arch ABI not supported)



endif
