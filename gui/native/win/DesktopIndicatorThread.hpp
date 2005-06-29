//
// File: DesktopIndicatorThread.hpp
//
#ifndef _DesktopIndicatorThread_hpp
#define _DesktopIndicatorThread_hpp

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class DesktopIndicatorThread
{
public:
    JNIEnv  *m_jniEnv;

    DesktopIndicatorThread();
    void     MakeSureThreadIsUp(JNIEnv *env);
    DWORD    getWinThreadId();
    
private:
    JavaVM  *m_jVM;
    DWORD    m_dwThread;
    int      m_iHandlerCount;

    static DWORD WINAPI ThreadProc(LPVOID lpParameter);
    DWORD  run();
};

extern DesktopIndicatorThread g_desktopIndicatorThread;

#define WM_DESKTOPINDICATOR  (WM_USER)

#endif // _DesktopIndicatorThread_hpp
