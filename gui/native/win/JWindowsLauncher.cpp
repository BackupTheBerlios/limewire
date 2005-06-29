//
// File: JWindowsLauncher.cpp
// Makes the native call to launch files their associated applications.
//
#include "StdAfx.h"
#include "JWindowsLauncher.h"

CPP_EXTERNC_BEGIN

/////////////////////////////////////////////////////////////////////////////

// Launches a file with its associated application on Windows.
//
JNIEXPORT jint
JNICALL   Java_com_limegroup_gnutella_util_WindowsLauncher_nativeLaunchFile
(JNIEnv   *jniEnv,
 jclass   jClass,
 jstring  jsFileName)
{
    LPCWSTR    lpcwszFileName;
    HINSTANCE  hInst;

    lpcwszFileName = jniEnv->GetStringChars(
        jsFileName, (jboolean *)NULL);
    hInst = UShellExecute(
        (HWND)NULL,
        L"open", lpcwszFileName, L"", L"",
        SW_SHOWNORMAL);
    jniEnv->ReleaseStringChars(jsFileName, lpcwszFileName);
    // It's ok to make this cast, since the HINSTANCE is not a true
    // window handle, but rather an error message holder.
    // (positive values lower than or equal to 32 are errors)
    if ((int)hInst <= 32) {
        return (jint)(-1);
    }
    return (jint)hInst;
}

/////////////////////////////////////////////////////////////////////////////

// Launches the specified url in the default web browser.
//
JNIEXPORT jint
JNICALL   Java_com_limegroup_gnutella_util_WindowsLauncher_nativeOpenURL
(JNIEnv   *jniEnv,
 jclass   jClass,
 jstring  jsUrl)
{
    HINSTANCE  hInst;
    LPCWSTR    lpcwszUrl;
    WCHAR      wszExe[MAX_PATH];

    // This is not the ideal way to do this, but we just search for the executable
    // associated with our empty htm file to launch the browser
    hInst = UFindExecutable(
        L"donotremove.htm", L"",
        wszExe);
    // (positive values lower than or equal to 32 are errors)
    if ((int)hInst <= 32) {
        return (jint)(-1);
    }

    // Get the JNI string as a UTF-16 string
    lpcwszUrl = (LPCWSTR)jniEnv->GetStringChars(
        jsUrl, (jboolean *)NULL);
    hInst = UShellExecute(
        (HWND)NULL,
        L"open", wszExe, lpcwszUrl, L"",
        SW_SHOWNORMAL);
    jniEnv->ReleaseStringChars(
        jsUrl, lpcwszUrl);
    // It's ok to make this cast, since the HINSTANCE is not a true
    // window handle, but rather an error message holder.
    // (positive values lower than or equal to 32 are errors)
    if ((int)hInst <= 32) {
        return (jint)(-1);
    }
    return (jint)hInst;
}

/////////////////////////////////////////////////////////////////////////////

CPP_EXTERNC_END
