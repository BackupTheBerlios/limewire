/**
 * Makes the native call to launch files their associated applications.
 */

#include "com_limegroup_gnutella_util_WindowsLauncher.h"
#include "windows.h"


/** Launches a file with its associated application on Windows. */
extern "C"
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_WindowsLauncher_nativeLaunchFile
(JNIEnv *env, jclass jc, jstring fileName) {
  const char* cFileName;
  cFileName = env->GetStringUTFChars(fileName, NULL);
  HINSTANCE handle = ShellExecute(NULL, "open", cFileName, "", "", SW_SHOWNORMAL);
  // free the memory for the string
  env->ReleaseStringUTFChars(fileName, cFileName);
  
  // it's ok to make this cast, since the HINSTANCE is not a true
  // window handle, but rather an error message holder.
  return (jint)handle;
}

/** 
 * Launches the specified url in the default web browser.
 */
extern "C"
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_WindowsLauncher_nativeOpenURL
(JNIEnv *env, jclass jc, jstring url) {
  const char* curl    = env->GetStringUTFChars(url, NULL);
  TCHAR szExe[MAX_PATH]; 

  // this is not the ideal way to do this, but we just search for the executable
  // associated with our empty htm file to launch the browser
  HINSTANCE hInst = FindExecutable("donotremove.htm","",szExe);

  // return our error code if FindExecutable failed
  if((int)hInst <= 32) return -1;

  HINSTANCE handle = ShellExecute(NULL, "open", szExe, curl, "", SW_SHOWNORMAL);
  env->ReleaseStringUTFChars(url, curl);
  return (jint)handle;
}
