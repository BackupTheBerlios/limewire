
#ifndef __DesktopIndicatorHandler_h__
#define __DesktopIndicatorHandler_h__


#include "DesktopIndicatorThread.h"


class DesktopIndicatorHandler
{
	friend DWORD WINAPI DesktopIndicatorThread::ThreadProc( LPVOID lpParameter );

public:

	static DesktopIndicatorHandler *extract( JNIEnv *env, jobject object );

	DesktopIndicatorHandler( JNIEnv *env, jobject object, jint image, const char *tooltip );

	void enable( JNIEnv *env );
	void update( jint image, const char *tooltip );
	void disable();
	void hide();
	//void showMenu();

private:

	enum
	{
		enableCode = 1,
		updateCode = 2,
		disableCode = 3,
		hideCode = 4
	};

	~DesktopIndicatorHandler();

	void doEnable();
	void doUpdate();
	void restoreApplication();
	void exitApplication();
	void exitAfterTransfers();
	//void exitJVM();
	void doHide();
	void removeNotify();
	void showAboutWindow();

	//void makeMenuCallback();

	HWND m_window;
	HICON m_icon;
	jobject m_object;
	jint m_image;
	char *m_tooltip;
	//jmethodID m_fireClicked;
	jmethodID m_restoreApplication;
	jmethodID m_exitApplication;
	jmethodID m_exitAfterTransfers;
	//jmethodID m_menuCallback;
	jmethodID m_exitJVM;
	jmethodID m_aboutWindow;

	static LRESULT CALLBACK WndProc( HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam );
	static DWORD WINAPI ThreadProc( LPVOID lpParameter );
};


#endif
