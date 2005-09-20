/*
* File: JWindowsNotifyUser.cpp
* Implement the JNI interface methods for the Java class
* com.limegroup.gnutella.gui.notify.WindowsNotifyUser.
*/
#include "StdAfx.h"
#include "JWindowsLauncher.h"
#include "DesktopIndicatorHandler.hpp"
#include "DesktopIndicatorImages.hpp"

CPP_EXTERNC_BEGIN

///////////////////////////////////////////////////////////////////////

/**
* Package:   com.limegroup.gnutella.gui.notify
* Class:     WindowsNotifyUser
* Call Type: static class method
* Method:    nativeLoadImage
* Signature: (Ljava/lang/String;)I
*/
JNIEXPORT jint
JNICALL   Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeLoadImage
(JNIEnv   *jniEnv,
 jclass   jClass,
 jstring  jsFilename)
{
    jboolean jbIsCopy;
    // Get Java string
    LPCWSTR lpcwszFilename =
        jniEnv->GetStringChars(jsFilename, &jbIsCopy);
    HICON hIcon =
        g_desktopIndicatorImages.add(lpcwszFilename);
    // Release Java string
    jniEnv->ReleaseStringChars(jsFilename, lpcwszFilename);
    return (jint)hIcon;
}

/**
* Package:   com.limegroup.gnutella.gui.notify
* Class:     WindowsNotifyUser
* Call Type: static class method
* Method:    nativeLoadImageFromResource
* Signature: (Ljava/lang/int;)I
*/
JNIEXPORT jint
JNICALL   Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeLoadImageFromResource
(JNIEnv   *jniEnv,
 jclass   jClass,
 jint     jiResourceId)
{
    //  DVB19Oct99 - use resource rather than external file
    return (jint)g_desktopIndicatorImages.add((WORD)jiResourceId);
}

/**
* Package:   com.limegroup.gnutella.gui.notify
* Class:     WindowsNotifyUser
* Call Type: static class method
* Method:    nativeFreeImage
* Signature: (I)V
*/
JNIEXPORT void
JNICALL   Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeFreeImage
(JNIEnv   *jniEnv,
 jclass   jClass,
 jint     jiImage)
{
    g_desktopIndicatorImages.remove((HICON)jiImage);
}

///////////////////////////////////////////////////////////////////////

/**
* Package:   com.limegroup.gnutella.gui.notify
* Class:     WindowsNotifyUser
* Call Type: instance method
* Method:    nativeEnable
* Signature: (ILjava/lang/String;)V
*/
JNIEXPORT void
JNICALL   Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeEnable
(JNIEnv   *jniEnv,
 jobject  jthis,
 jint     jiImage,
 jstring  jsTooltip)
{
    jboolean jbIsCopy;
    // Get Java string
    LPCWSTR lpcwszTooltip =
        jniEnv->GetStringChars(jsTooltip, &jbIsCopy);
    // Get handler
    DesktopIndicatorHandler *handler =
        DesktopIndicatorHandler::extract(jniEnv, jthis);
    if (handler) {
        // Already exists, so update it
        handler->update(
            (HICON)jiImage, lpcwszTooltip);
    } else {
        // Create our handler
		handler = new DesktopIndicatorHandler(jniEnv, jthis,
            (HICON)jiImage, lpcwszTooltip);
        // Enable it
        if (handler) {
            handler->enable(jniEnv);
        }
    }
    // Release Java string
    jniEnv->ReleaseStringChars(jsTooltip, lpcwszTooltip);
}

/**
* Package:   com.limegroup.gnutella.gui.notify
* Class:     WindowsNotifyUser
* Method:    nativeDisable
* Signature: ()V
* Call Type: instance method
*/
JNIEXPORT void
JNICALL   Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeDisable
(JNIEnv   *jniEnv,
 jobject  joWindowsNotifyUser)
{
    // Get handler
    DesktopIndicatorHandler *handler =
        DesktopIndicatorHandler::extract(jniEnv, joWindowsNotifyUser);
    // Disable it
    if (handler) {
        handler->disable();
    }
}

/**
* Package:   com.limegroup.gnutella.gui.notify
* Class:     WindowsNotifyUser
* Call Type: instance method
* Method:    nativeHide
* Signature: ()V
*/
JNIEXPORT void
JNICALL   Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeHide
(JNIEnv   *jniEnv,
 jobject  jthis)
{
    // Get handler
    DesktopIndicatorHandler *handler =
        DesktopIndicatorHandler::extract(jniEnv, jthis);
    if (handler) {
        handler->hide();
    }
}

///////////////////////////////////////////////////////////////////////

CPP_EXTERNC_END
