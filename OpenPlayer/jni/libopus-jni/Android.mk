LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := opus-jni

#add -g for debugging purposes
LOCAL_CFLAGS +=-I$(LOCAL_PATH)/../libopus/include -I$(LOCAL_PATH)/../libogg/include -fsigned-char

ifeq ($(TARGET_ARCH),arm)
	LOCAL_CFLAGS += -march=armv6 -marm -mfloat-abi=softfp -mfpu=vfp
endif

#$(warning localpath is: $(LOCAL_PATH))
LOCAL_SHARED_LIBRARIES := libogg libopus

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

# force including the opus_header for the opus_header_parse code
LOCAL_SRC_FILES := ../decodefeed/DecodeFeed.c ../libopus/src/opus_header.c OpusHeader.c org_xiph_opus_decoderjni_OpusDecoder.c

include $(BUILD_SHARED_LIBRARY)
