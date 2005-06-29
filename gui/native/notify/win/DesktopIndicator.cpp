
#include "stdafx.h"
#include "DesktopIndicator.h"
#include "DesktopIndicatorHandler.h"
#include "DesktopIndicatorImages.h"
#include "resource.h"       //  DVB19Oct99


HINSTANCE g_instance = NULL;


BOOL WINAPI DllMain
(
	HINSTANCE hinstDLL,  // handle to DLL module
	DWORD fdwReason,     // reason for calling function
	LPVOID lpvReserved   // reserved
)
{
    switch( fdwReason )
	{
		case DLL_THREAD_ATTACH:
		case DLL_THREAD_DETACH:
		case DLL_PROCESS_DETACH:

		case DLL_PROCESS_ATTACH:
			g_instance = hinstDLL;
			break;
    }
    return TRUE;
}


extern "C"
/*
 * Class:     DesktopIndicator
 * Method:    nativeDisable
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeDisable
  (JNIEnv *env, jobject object)
{
	// Get handler
	DesktopIndicatorHandler *l_handler = DesktopIndicatorHandler::extract( env, object );

	// Disable it
	if( l_handler )
		l_handler->disable();
}


extern "C"
/*
 * Class:     DesktopIndicator
 * Method:    nativeEnable
 * Signature: (ILjava/lang/String;)V
 */
 //Java_com_eii_tooltray_DesktopIndicator_activate
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeEnable
  (JNIEnv *env, jobject object, jint image, jstring tooltip)
{
	jboolean l_IsCopy;

	// Get Java string
	const char *l_tooltip = env->GetStringUTFChars( tooltip, &l_IsCopy );

	// Get handler
	DesktopIndicatorHandler *l_handler = DesktopIndicatorHandler::extract( env, object );

	if( l_handler ) 
	{
		// Already exists, so update it
		l_handler->update( image, l_tooltip );
		
	}
	else
	{
		// Create our handler
		l_handler = new DesktopIndicatorHandler( env, object, image, l_tooltip );

		// Enable it
		if( l_handler )
			l_handler->enable( env );
	}

	// Release Java string
    env->ReleaseStringUTFChars( tooltip, l_tooltip );
}


extern "C"
/*
 * Class:     DesktopIndicator
 * Method:    nativeFreeImage
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeFreeImage
  (JNIEnv *env, jclass, jint image)
{
	g_DesktopIndicatorImages.remove( image );
}



extern "C"
/*
 * Class:     DesktopIndicator
 * Method:    nativeLoadImageFromResource
 * Signature: (Ljava/lang/int;)I
 */
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeLoadImageFromResource
  (JNIEnv *env, jclass, jint resourceid)
{
	//  DVB19Oct99 - use resource rather than external file
	jint image = g_DesktopIndicatorImages.add( resourceid );       

	return image;
}


extern "C"
/*
 * Class:     DesktopIndicator
 * Method:    nativeHide
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeHide
  (JNIEnv *env, jobject object)
{
	// Get handler
	DesktopIndicatorHandler *l_handler = DesktopIndicatorHandler::extract( env, object );

	if( l_handler )
	{
		l_handler->hide();
	}	
}


extern "C"
/*
 * Class:     DesktopIndicator
 * Method:    nativeLoadImage
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_gui_notify_WindowsNotifyUser_nativeLoadImage
  (JNIEnv *env, jclass, jstring filename)
{
	jboolean l_IsCopy;

	// Get Java string
	const char *l_filename = env->GetStringUTFChars( filename, &l_IsCopy );

	jint image = g_DesktopIndicatorImages.add( l_filename );

	// Release Java string
    env->ReleaseStringUTFChars( filename, l_filename );

	return image;
}
