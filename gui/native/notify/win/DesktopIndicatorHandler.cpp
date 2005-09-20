
#include "stdafx.h"
#include "DesktopIndicatorHandler.h"
#include "DesktopIndicatorThread.h"


#define WM_DESKTOPINDICATOR_CLICK (WM_USER+1)
#define MNU_EXIT                  (WM_USER+2)
#define MNU_RESTORE               (WM_USER+3)
#define MNU_ABOUT                 (WM_USER+4)
#define MNU_EXIT_AFTER            (WM_USER+5)



DesktopIndicatorHandler *DesktopIndicatorHandler::extract( JNIEnv *env, jobject object )
{
	// Get field ID			
	jfieldID l_handlerId = env->GetFieldID( env->GetObjectClass( object ), "_handler", "I" );

	// Get field
	DesktopIndicatorHandler *l_handler = (DesktopIndicatorHandler *) env->GetIntField( object, l_handlerId );

	return l_handler;
}


DesktopIndicatorHandler::DesktopIndicatorHandler( JNIEnv *env, jobject object, jint image, const char *tooltip )
{
	m_window = NULL;

	m_icon = (HICON) image;

	// Copy string
	m_tooltip = strdup( tooltip );

	// Reference object
	m_object = env->NewGlobalRef( object );

	// Get method ID
	m_restoreApplication = 
	    env->GetMethodID(env->GetObjectClass(m_object), "restoreApplication", "()V");

	// Get method ID
	m_exitApplication = 
	    env->GetMethodID(env->GetObjectClass(m_object), "exitApplication", "()V");

	m_exitAfterTransfers = 
		env->GetMethodID(env->GetObjectClass(m_object), "exitAfterTransfers", "()V");

	// Get method ID
	m_aboutWindow = 
	    env->GetMethodID(env->GetObjectClass(m_object), "showAboutWindow", "()V");

	// Get field ID
	jfieldID l_handlerId = env->GetFieldID( env->GetObjectClass( m_object ), "_handler", "I" );

	// Set field
	env->SetIntField( m_object, l_handlerId, (jint) this );
}


DesktopIndicatorHandler::~DesktopIndicatorHandler()
{

	// Get field ID
	jfieldID l_handlerId = 
		g_DesktopIndicatorThread.m_env->GetFieldID( g_DesktopIndicatorThread.m_env->GetObjectClass( m_object ), "_handler", "I" );

	// Set field
	g_DesktopIndicatorThread.m_env->SetIntField( m_object, l_handlerId, 0 );


	// Destroy window
	DestroyWindow( m_window );

	// Free string
	if( m_tooltip )
		delete m_tooltip;


	// Release our reference
	g_DesktopIndicatorThread.m_env->DeleteGlobalRef( m_object );
	
}


void DesktopIndicatorHandler::enable( JNIEnv *env )
{
	g_DesktopIndicatorThread.MakeSureThreadIsUp( env );
	while( !PostThreadMessage( g_DesktopIndicatorThread, WM_DESKTOPINDICATOR, enableCode, (LPARAM) this ) )
		Sleep( 0 );
}


void DesktopIndicatorHandler::doEnable()
{
	// Register window class
	WNDCLASSEX l_Class;
	l_Class.cbSize = sizeof( l_Class );
	l_Class.style = 0;
	l_Class.lpszClassName = TEXT( "DesktopIndicatorHandlerClass" );
	l_Class.lpfnWndProc = WndProc;
	l_Class.hbrBackground = NULL;
	l_Class.hCursor = NULL;
	l_Class.hIcon = NULL;
	l_Class.hIconSm = NULL;
	l_Class.lpszMenuName = NULL;
	l_Class.cbClsExtra = 0;
	l_Class.cbWndExtra = 0;
	l_Class.hInstance = NULL;	//CB enables this code to work in WIN 98

	static bool bFirst=true;
	if( bFirst && !RegisterClassEx( &l_Class ) )
		return;
	bFirst=false;

	// Create window
	m_window = CreateWindow
	(
		TEXT( "DesktopIndicatorHandlerClass" ),
		TEXT( "DesktopIndicatorHandler" ),
		WS_POPUP,
		0, 0, 0, 0,
		NULL,
		NULL,
		0,
		NULL
	);

	if( !m_window )
		return;

	// Set this pointer
	SetWindowLong( m_window, GWL_USERDATA, (LONG) this );

	// Add shell icon
	NOTIFYICONDATA m_iconData;
	m_iconData.cbSize = sizeof(NOTIFYICONDATA);
	m_iconData.uFlags = NIF_MESSAGE |  NIF_ICON | NIF_TIP;
	m_iconData.uCallbackMessage = WM_DESKTOPINDICATOR_CLICK;
	m_iconData.uID = 0;
	m_iconData.hWnd = m_window;
	m_iconData.hIcon = m_icon;
	strcpy( m_iconData.szTip, m_tooltip );

	Shell_NotifyIcon( NIM_ADD, &m_iconData );
}


void DesktopIndicatorHandler::update( jint image, const char *tooltip )
{
	m_icon = (HICON) image;

	// Free string
	if( m_tooltip )
		delete m_tooltip;

	// Copy string
	m_tooltip = strdup( tooltip );

	PostThreadMessage( g_DesktopIndicatorThread, WM_DESKTOPINDICATOR, updateCode, (LPARAM) this );
}


void DesktopIndicatorHandler::doUpdate()
{
	// Modify shell icon
	NOTIFYICONDATA m_iconData;
	m_iconData.cbSize = sizeof(NOTIFYICONDATA);
	m_iconData.uFlags = NIF_MESSAGE |  NIF_ICON | NIF_TIP;
	m_iconData.uCallbackMessage = WM_DESKTOPINDICATOR_CLICK;
	m_iconData.uID = 0;
	m_iconData.hWnd = m_window;
	m_iconData.hIcon = m_icon;
	strcpy( m_iconData.szTip, m_tooltip );

	Shell_NotifyIcon( NIM_DELETE, &m_iconData );
	Shell_NotifyIcon( NIM_ADD, &m_iconData );
}

void DesktopIndicatorHandler::doHide()
{
// Modify shell icon
	NOTIFYICONDATA m_iconData;
	m_iconData.cbSize = sizeof( m_iconData );
	m_iconData.uID = 0;
	m_iconData.hWnd = m_window;
	m_iconData.hIcon = m_icon;
	strcpy( m_iconData.szTip, m_tooltip );
	Shell_NotifyIcon( NIM_DELETE, &m_iconData );
}

void DesktopIndicatorHandler::hide()
{
	PostThreadMessage( g_DesktopIndicatorThread, WM_DESKTOPINDICATOR, hideCode, (LPARAM) this );
}



void DesktopIndicatorHandler::disable()
{
	removeNotify();
	PostThreadMessage( g_DesktopIndicatorThread, WM_DESKTOPINDICATOR, disableCode, (LPARAM) this );
}


void DesktopIndicatorHandler::restoreApplication()
{
	g_DesktopIndicatorThread.m_env->CallVoidMethod( m_object, m_restoreApplication );
}

void DesktopIndicatorHandler::exitApplication()
{
	g_DesktopIndicatorThread.m_env->CallVoidMethod( m_object, m_exitApplication );
}

void DesktopIndicatorHandler::showAboutWindow()
{
	g_DesktopIndicatorThread.m_env->CallVoidMethod( m_object, m_aboutWindow );
}

void DesktopIndicatorHandler::exitAfterTransfers()
{
	g_DesktopIndicatorThread.m_env->CallVoidMethod( m_object, m_exitAfterTransfers );
}

void DesktopIndicatorHandler::removeNotify()
{
	// Delete shell icon
	NOTIFYICONDATA m_iconData;
	m_iconData.cbSize = sizeof( m_iconData );
	m_iconData.uID = 0;
	m_iconData.hWnd = m_window;

	Shell_NotifyIcon( NIM_DELETE, &m_iconData );
}
 


LRESULT CALLBACK DesktopIndicatorHandler::WndProc( HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam )
{
	// Check for our special notification message
	if( ( uMessage != WM_DESKTOPINDICATOR_CLICK ) ) 
	{
		return DefWindowProc( hWnd, uMessage, wParam, lParam );
	}

	DesktopIndicatorHandler *l_this = (DesktopIndicatorHandler *) GetWindowLong( hWnd, GWL_USERDATA );

	switch(lParam) {
		//case WM_LBUTTONDOWN:
		//do code
		//break;
		//return 0;
		//case WM_RBUTTONDOWN:
		//do code
		//break;
		//return 0;
		//case WM_LBUTTONUP:
		//do code
		//break;
		//return 0;
		case WM_RBUTTONUP: //Display popup menu
			HMENU hMenu;
			POINT pos;
			hMenu = CreatePopupMenu();
			AppendMenu(hMenu, MF_STRING, MNU_RESTORE,  "&Restore");
			AppendMenu(hMenu, MF_STRING, MNU_ABOUT, "&About");
			AppendMenu(hMenu, MF_SEPARATOR, 0, NULL);
			AppendMenu(hMenu, MF_STRING, MNU_EXIT_AFTER,  "&Exit After Transfers");
			AppendMenu(hMenu, MF_STRING, MNU_EXIT,  "E&xit");
			GetCursorPos(&pos);
	  
			SetForegroundWindow(hWnd);
			switch(TrackPopupMenu(hMenu, TPM_CENTERALIGN | TPM_BOTTOMALIGN | 
  								TPM_LEFTBUTTON | TPM_NONOTIFY | TPM_RETURNCMD, 
  								pos.x, pos.y, 0, hWnd, NULL)) {
  				case MNU_EXIT:	
  					l_this->exitApplication();
  					return 0;
				case MNU_EXIT_AFTER:	
  					l_this->exitAfterTransfers();
  					return 0;
  				case MNU_ABOUT:
  					l_this->showAboutWindow();
  					return 0;
  				case MNU_RESTORE:
  					l_this->restoreApplication();
  					return 0;
  				default:
  					return 0;   
  			}
			PostMessage(hWnd, WM_NULL, 0, 0);
	
			return 0;
			
		case WM_LBUTTONDBLCLK:
			l_this->restoreApplication();
			return 0;

		case WM_DESTROY:			
			PostQuitMessage (0);
			return 0;  
	  
		default:
			return 0;  
	}
}
