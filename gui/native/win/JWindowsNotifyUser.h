/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_limegroup_gnutella_gui_notify_WindowsNotifyUser */

#ifndef _Included_com_limegroup_gnutella_gui_notify_WindowsNotifyUser
#define _Included_com_limegroup_gnutella_gui_notify_WindowsNotifyUser
#ifdef __cplusplus
extern "C" {
#endif

///////////////////////////////////////////////////////////////////////
// Load native Windows icons that can be used in the notication area
// Native icons are created as (HICON) and returned to Java as (jint)
///////////////////////////////////////////////////////////////////////

/*
 * Class:     com_limegroup_gnutella_gui_notify_WindowsNotifyUser
 * Method:    nativeLoadImage
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeLoadImage
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_limegroup_gnutella_gui_notify_WindowsNotifyUser
 * Method:    nativeLoadImageFromResource
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeLoadImageFromResource
  (JNIEnv *, jclass, jint);

/*
 * Class:     com_limegroup_gnutella_gui_notify_WindowsNotifyUser
 * Method:    nativeFreeImage
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeFreeImage
  (JNIEnv *, jclass, jint);

///////////////////////////////////////////////////////////////////////
// Display a tooltip (a jstring) and an icon (a jint holding a HICON,
// created by above functions) in the native notification area
///////////////////////////////////////////////////////////////////////

/*
 * Class:     com_limegroup_gnutella_gui_notify_WindowsNotifyUser
 * Method:    nativeEnable
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeEnable
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     com_limegroup_gnutella_gui_notify_WindowsNotifyUser
 * Method:    nativeDisable
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeDisable
  (JNIEnv *, jobject);

/*
 * Class:     com_limegroup_gnutella_gui_notify_WindowsNotifyUser
 * Method:    nativeHide
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeHide
  (JNIEnv *, jobject);

///////////////////////////////////////////////////////////////////////

#ifdef __cplusplus
}
#endif
#endif