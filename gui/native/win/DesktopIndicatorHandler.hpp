//
// File: DesktopIndicatorHandler.hpp
//
#ifndef _DesktopIndicatorHandler_hpp
#define _DesktopIndicatorHandler_hpp

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "DesktopIndicatorThread.hpp"

class DesktopIndicatorHandler
{
public:
    DesktopIndicatorHandler(
        JNIEnv *jniEnv, jobject joWindowsNotifyUser,
        HICON hIcon, LPCWSTR lpcwszTooltip);
    
    // These actions are not run in the calling thread
    void enable(JNIEnv *env);
    void update(HICON hIcon, LPCWSTR lpcwsztooltip);
    void disable();
    void hide();

    static DesktopIndicatorHandler *extract(
        JNIEnv *env, jobject joWindowsNotifyUser);
    
private:
    // Messages posted to the DesktopIndicatorThread
    enum {
        enableCode = 1,
        updateCode = 2,
        disableCode = 3,
        hideCode = 4
    };

    HWND       m_hWnd;
    HICON      m_hIcon;
    LPWSTR     m_lpwszTooltip;
    jobject    m_joWindowsNotifyUser;
    // Strings for the popup menu
    LPWSTR     m_lpwszItemRestore;
    LPWSTR     m_lpwszItemAbout;
    LPWSTR     m_lpwszItemExitLater;
    LPWSTR     m_lpwszItemExit;
    
    ~DesktopIndicatorHandler();

    // all these actions are performed in the DesktopIndicatorThread

    friend DWORD DesktopIndicatorThread::run();

    static LRESULT WINAPI WndProc(
        HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam);

    void doEnable();
    void doUpdate();
    void doHide();
    void removeNotify();
    
    // popup menu actions
    void restoreApplication();
    void exitApplication();
    void exitAfterTransfers();
    void showAboutWindow();

};

#endif // _DesktopIndicatorHandler_hpp
