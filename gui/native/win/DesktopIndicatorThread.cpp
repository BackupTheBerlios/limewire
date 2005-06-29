//
// File: DesktopIndicatorThread.cpp
//
#include "StdAfx.h"
#include "DesktopIndicatorThread.hpp"
#include "DesktopIndicatorHandler.hpp"

/////////////////////////////////////////////////////////////////////////////

DesktopIndicatorThread g_desktopIndicatorThread;

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
DesktopIndicatorThread::DesktopIndicatorThread()
{
    m_jniEnv = (JNIEnv *)NULL;
    m_jVM = (JavaVM *)NULL;
    m_dwThread = 0;
    m_iHandlerCount = 0;
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
// This allows the calling Java thread to post messages to the desktop
// indicator working thread that performs the actual Windows actions.
DWORD
DesktopIndicatorThread::getWinThreadId()
{
    return m_dwThread;
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
void
DesktopIndicatorThread::MakeSureThreadIsUp
(JNIEnv  *jniEnv)
{
    if (!m_dwThread) {
        // Get VM
        jniEnv->GetJavaVM(&m_jVM);
        // Start "native" thread
        ::CreateThread(
            (LPSECURITY_ATTRIBUTES)NULL,
            0, // default stack size (committed)
            ThreadProc,
            (LPVOID)this, //lpParameter
            0, // Creation Flags: run immediately
            &m_dwThread); //receives the Thread Id
        // Yield immediately to any thread of any priority
        Sleep(0);
    }
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread
DWORD
DesktopIndicatorThread::run()
{
    // Attach the thread to the VM
    m_jVM->AttachCurrentThread((void **)&m_jniEnv, NULL);
    // Process incoming messages from any window in the current thread
    // or messages posted to this thread by any other thread by means
    // of PostThreadMessage().
    // The current thread does not create windows, so it mainly handles
    // (asynchronously) the messages posted by the DesktopIndicatorHandler
    // creator thread that receives Java requests but does not process them.
    MSG msg;
    while (::GetMessage(&msg, NULL, 0, 0)) {
        if (msg.message == WM_DESKTOPINDICATOR) {
            // Extract handler
            DesktopIndicatorHandler *handler =
                (DesktopIndicatorHandler *)msg.lParam;
            switch (msg.wParam) {
            case (WPARAM)DesktopIndicatorHandler::enableCode:
                m_iHandlerCount++;
                handler->doEnable();
                break;
            case (WPARAM)DesktopIndicatorHandler::updateCode:
                handler->doUpdate();
                break;
            case (WPARAM)DesktopIndicatorHandler::hideCode:
                handler->doHide();
                break;
            case (WPARAM)DesktopIndicatorHandler::disableCode:
                // Destroy it!
                delete handler;
                // No more handlers?
                if (!--m_iHandlerCount) {
                    m_dwThread = 0;
                    // Detach thread from VM
                    m_jVM->DetachCurrentThread();
                    // Time to die
                    ::ExitThread(0);
                }
                break;
            }
        } else {
            ::TranslateMessage(&msg);
            ::DispatchMessage(&msg);
        }
    }
    // Detach thread from VM
    m_jVM->DetachCurrentThread();
    return 0;
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread (entry point)
DWORD WINAPI
DesktopIndicatorThread::ThreadProc
(LPVOID  lpParameter)
{
    DesktopIndicatorThread *lpthis =
        (DesktopIndicatorThread *)lpParameter;
    return lpthis->run();
}

/////////////////////////////////////////////////////////////////////////////
