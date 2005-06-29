//
// File: DesktopIndicatorHandler.cpp
// Implement the DesktopIndicator C++ class,
// used by the JNI methods for the Java class
// com.limegroup.gnutella.gui.notify.WindowsNotifyUser.
//
#include "StdAfx.h"
#include "DesktopIndicatorHandler.hpp"
#include "DesktopIndicatorThread.hpp"

/////////////////////////////////////////////////////////////////////////////

#define WM_DESKTOPINDICATOR_CLICK (WM_USER+1)
#define MNU_EXIT                  (WM_USER+2)
#define MNU_RESTORE               (WM_USER+3)
#define MNU_ABOUT                 (WM_USER+4)
#define MNU_EXIT_AFTER            (WM_USER+5)

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
DesktopIndicatorHandler::DesktopIndicatorHandler
(JNIEnv   *jniEnv,
 jobject  joWindowsNotifyUser,
 HICON    hIcon,
 LPCWSTR  lpcwszTooltip)
{
    m_hWnd = NULL;
    m_hIcon = hIcon;
    // Copy UTF-16 string
    m_lpwszTooltip = new WCHAR[wcslen(lpcwszTooltip) + 1];
    wcscpy(m_lpwszTooltip, lpcwszTooltip);
    // Reference joWindowsNotifyUser (will not be garbage collected)
    m_joWindowsNotifyUser = jniEnv->NewGlobalRef(joWindowsNotifyUser);
    try {
        // Set the Java _handler field to this C++ handler
        jclass jcWindowsNotifyUser =
            jniEnv->GetObjectClass(m_joWindowsNotifyUser);
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();

        jfieldID jfid__handler = jniEnv->GetFieldID(
            jcWindowsNotifyUser, "_handler", "I");
        jniEnv->DeleteLocalRef(jcWindowsNotifyUser);
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();

        jniEnv->SetIntField(m_joWindowsNotifyUser, jfid__handler,
            (jint)this);
    } catch (jthrowable) {
        jniEnv->ExceptionDescribe(); //optional dump
        jniEnv->ExceptionClear();
    }

    m_lpwszItemRestore   = (LPWSTR)NULL;
    m_lpwszItemAbout     = (LPWSTR)NULL;
    m_lpwszItemExitLater = (LPWSTR)NULL;
    m_lpwszItemExit      = (LPWSTR)NULL;
    // Use the public static method of the "GUIMediator" class:
    // final String getStringResource(final String resourceKey);
    // in the "com.limegroup.gnutella.gui" package
    // A less complex way to do it would be to perform all that
    // directly in WindowsNotifyUser.java, and send these strings
    // to the native interface.
    try {
        // IN JDK 1.1, there was no ClassLoader was refered, and
        // FindClass() located classsed only in the system CLASSPATH.
        // In JDK 1.2 and more, FindClass() uses the ClassLoader of
        // the calling Java class from a native method was invoked to
        // run the current C/C++ function, so here it will use the
        // same ClassLoader as used by joWindowsNotifyUser.
        // A more precise location may require sending a ClassLoader
        // object, but it is not needed here since 1.2. With the JDK 1.1,
        // we could use the JNI GetClass(joWindowsNotifyUser) to get a
        // Class object, then invoke its Java method "GetClassLoader()"
        // then invoke its Java method "findClass(name)".
        jclass  jcGUIMediator = jniEnv->FindClass(
                "com/limegroup/gnutella/gui/GUIMediator");
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jmethodID   jmid_getStringResource = jniEnv->GetStaticMethodID(
                    jcGUIMediator, "getStringResource",
                    "(Ljava/lang/String;)Ljava/lang/String;");
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();

        // Local references used when calling this method
        jstring     jsResourceKey;
        jstring     jsResourceValue;
        jsize       cwcResourceValue;
        LPCWSTR     lpcwszResourceValue;

        //GUIMediator.getStringResource("TRAY_RESTORE_LABEL");
        jsResourceKey = jniEnv->NewStringUTF("TRAY_RESTORE_LABEL");
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jsResourceValue = (jstring)jniEnv->CallStaticObjectMethod(
            jcGUIMediator, jmid_getStringResource,
            jsResourceKey); //typecast result from jobject to jstring
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jniEnv->DeleteLocalRef(jsResourceKey);

        // Retreive the Unicode string in jsResourceValue
        lpcwszResourceValue = jniEnv->GetStringChars(jsResourceValue,
            (jboolean *)NULL);
        cwcResourceValue = jniEnv->GetStringLength(jsResourceValue);
        m_lpwszItemRestore = new WCHAR[cwcResourceValue + 1];
        wcsncpy(m_lpwszItemRestore, lpcwszResourceValue,
            cwcResourceValue + 1);
        jniEnv->ReleaseStringChars(jsResourceValue, lpcwszResourceValue);
        jniEnv->DeleteLocalRef(jsResourceValue);

        //GUIMediator.getStringResource("TRAY_ABOUT_LABEL");
        jsResourceKey = jniEnv->NewStringUTF("TRAY_ABOUT_LABEL");
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jsResourceValue = (jstring)jniEnv->CallStaticObjectMethod(
            jcGUIMediator, jmid_getStringResource,
            jsResourceKey); //typecast result from jobject to jstring
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jniEnv->DeleteLocalRef(jsResourceKey);

        // Retreive the Unicode string in jsResourceValue
        lpcwszResourceValue = jniEnv->GetStringChars(jsResourceValue,
            (jboolean *)NULL);
        cwcResourceValue = jniEnv->GetStringLength(jsResourceValue);
        m_lpwszItemAbout = new WCHAR[cwcResourceValue + 1];
        wcsncpy(m_lpwszItemAbout, lpcwszResourceValue,
            cwcResourceValue + 1);
        jniEnv->ReleaseStringChars(jsResourceValue, lpcwszResourceValue);
        jniEnv->DeleteLocalRef(jsResourceValue);

        //"&" + GUIMediator.getStringResource("TRAY_EXIT_LATER_LABEL");
        jsResourceKey = jniEnv->NewStringUTF("TRAY_EXIT_LATER_LABEL");
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jsResourceValue = (jstring)jniEnv->CallStaticObjectMethod(
            jcGUIMediator, jmid_getStringResource,
            jsResourceKey); //typecast result from jobject to jstring
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jniEnv->DeleteLocalRef(jsResourceKey);

        // Retreive the Unicode string in jsResourceValue
        lpcwszResourceValue = jniEnv->GetStringChars(jsResourceValue,
            (jboolean *)NULL);
        cwcResourceValue = jniEnv->GetStringLength(jsResourceValue);
        m_lpwszItemExitLater = new WCHAR[cwcResourceValue + 1];
        wcsncpy(m_lpwszItemExitLater, lpcwszResourceValue,
            cwcResourceValue + 1);
        jniEnv->ReleaseStringChars(jsResourceValue, lpcwszResourceValue);
        jniEnv->DeleteLocalRef(jsResourceValue);

        //"&" + GUIMediator.getStringResource("TRAY_EXIT_LABEL");
        jsResourceKey = jniEnv->NewStringUTF("TRAY_EXIT_LABEL");
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jsResourceValue = (jstring)jniEnv->CallStaticObjectMethod(
            jcGUIMediator, jmid_getStringResource,
            jsResourceKey); //typecast result from jobject to jstring
        if (jniEnv->ExceptionCheck())
            throw jniEnv->ExceptionOccurred();
        jniEnv->DeleteLocalRef(jsResourceKey);

        // Retreive the Unicode string in jsResourceValue
        lpcwszResourceValue = jniEnv->GetStringChars(jsResourceValue,
            (jboolean *)NULL);
        cwcResourceValue = jniEnv->GetStringLength(jsResourceValue);
        m_lpwszItemExit = new WCHAR[cwcResourceValue + 1];
        wcsncpy(m_lpwszItemExit, lpcwszResourceValue,
            cwcResourceValue + 1);
        jniEnv->ReleaseStringChars(jsResourceValue, lpcwszResourceValue);
        jniEnv->DeleteLocalRef(jsResourceValue);

        jniEnv->DeleteLocalRef(jcGUIMediator);
    } catch (jthrowable) {
        jniEnv->ExceptionDescribe();
        jniEnv->ExceptionClear();
    }
    // Default popup menu item labels
    if (m_lpwszItemRestore   == (LPWSTR)NULL)
        m_lpwszItemRestore   = wcsdup(L"&Restore");
    if (m_lpwszItemAbout     == (LPWSTR)NULL)
        m_lpwszItemAbout     = wcsdup(L"&About");
    if (m_lpwszItemExitLater == (LPWSTR)NULL)
        m_lpwszItemExitLater = wcsdup(L"&Exit After Transfers");
    if (m_lpwszItemExit      == (LPWSTR)NULL)
        m_lpwszItemExit      = wcsdup(L"E&xit");
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
DesktopIndicatorHandler::~DesktopIndicatorHandler()
{
    if (m_lpwszItemRestore   != (LPWSTR)NULL)
        delete[] m_lpwszItemRestore;
    if (m_lpwszItemAbout     != (LPWSTR)NULL)
        delete[] m_lpwszItemAbout;
    if (m_lpwszItemExitLater != (LPWSTR)NULL)
        delete[] m_lpwszItemExitLater;
    if (m_lpwszItemExit      != (LPWSTR)NULL)
        delete[] m_lpwszItemExit;
    JNIEnv *jniEnv = g_desktopIndicatorThread.m_jniEnv;
    // Reset the Java _handler field
    jniEnv->SetIntField(
        m_joWindowsNotifyUser, jniEnv->GetFieldID(
            jniEnv->GetObjectClass(m_joWindowsNotifyUser),
            "_handler", "I"),
        (jint)NULL);
    // Destroy window
    DestroyWindow(m_hWnd);
    // Free string
    if (m_lpwszTooltip) {
        delete[] m_lpwszTooltip;
    }
    // Release our Java notifier reference. Is really the
    // jniEnv cachable and valid among distinct threads ??
    // This code assumes that the calling Java thread (which uses
    // this constant jniEnv) will persist as long as both the
    // DesktopIndicatorHandler instance and the
    // DesktopIndicatorThread instance survive, and that all the
    // WindowsNotifyUser Java objects are created in the same
    // Java thread. The GlobalRef maintains a reference of an
    // object owned by the initial Java thread that created it, so
    // that that Java thread will NOT die completely before this
    // destructor is called (so that no event will be further sent
    // by this handler WndProc to a dead Java process.)
    jniEnv->DeleteGlobalRef(m_joWindowsNotifyUser);
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
DesktopIndicatorHandler *
DesktopIndicatorHandler::extract
(JNIEnv   *jniEnv,
 jobject   joWindowsNotifyUser)
{
    return (DesktopIndicatorHandler *)
        jniEnv->GetIntField(joWindowsNotifyUser,
            jniEnv->GetFieldID(
                jniEnv->GetObjectClass(joWindowsNotifyUser),
                "_handler", "I"));
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
void
DesktopIndicatorHandler::enable
(JNIEnv *jniEnv)
{
    g_desktopIndicatorThread.MakeSureThreadIsUp(jniEnv);
    while (!PostThreadMessage(
            g_desktopIndicatorThread.getWinThreadId(),
            WM_DESKTOPINDICATOR, (WPARAM)enableCode,
            (LPARAM)this)) {
        Sleep(0); //Yield to other threads with any priority
    }
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
void
DesktopIndicatorHandler::update
(HICON hIcon, LPCWSTR lpcwszTooltip)
{
    m_hIcon = hIcon;
    // Free string
    if (m_lpwszTooltip) {
        delete m_lpwszTooltip;
    }
    // Copy string
    m_lpwszTooltip = wcsdup(lpcwszTooltip);
    PostThreadMessage(
        g_desktopIndicatorThread.getWinThreadId(),
        WM_DESKTOPINDICATOR, (WPARAM)updateCode,
        (LPARAM)this);
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
void
DesktopIndicatorHandler::hide()
{
    PostThreadMessage(
        g_desktopIndicatorThread.getWinThreadId(),
        WM_DESKTOPINDICATOR, (WPARAM)hideCode,
        (LPARAM)this);
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
void
DesktopIndicatorHandler::disable()
{
    removeNotify();
    PostThreadMessage(
        g_desktopIndicatorThread.getWinThreadId(),
        WM_DESKTOPINDICATOR, (WPARAM)disableCode,
        (LPARAM)this);
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Java Swing dispatch thread
void
DesktopIndicatorHandler::doEnable()
{
    // Register window class
    WNDCLASSEX wndClass;

    ZeroMemory(&wndClass, sizeof(wndClass));
    wndClass.cbSize = sizeof(wndClass);
    wndClass.lpszClassName = TEXT("DesktopIndicatorHandlerClass");
    wndClass.lpfnWndProc = WndProc;
    if (!RegisterClassEx(&wndClass)) {
        return;
    }
    // Create window
    m_hWnd = CreateWindow(
        TEXT("DesktopIndicatorHandlerClass"),
        TEXT("DesktopIndicatorHandler"),
        WS_POPUP,
        0, 0, 0, 0,
        NULL,
        NULL,
        0,
        NULL);
    if (!m_hWnd) {
        return;
    }
    // Set this pointer
    SetWindowLong(m_hWnd, GWL_USERDATA, (LONG)this);

    // Add shell icon
    NOTIFYICONDATAW iconData;

    iconData.cbSize = sizeof(NOTIFYICONDATA);
    iconData.hWnd = m_hWnd;
    iconData.uID = 0;
    iconData.uFlags = NIF_MESSAGE |  NIF_ICON | NIF_TIP;
    iconData.uCallbackMessage = WM_DESKTOPINDICATOR_CLICK;
    iconData.hIcon = m_hIcon;
    /* copy the tooltip text up to its maximum size */
    wcsncpy(iconData.szTip, m_lpwszTooltip,
        sizeof(iconData.szTip) / sizeof(iconData.szTip[0]));
    iconData.szTip[
        sizeof(iconData.szTip) / sizeof(iconData.szTip[0]) - 1]
        = L'\0';
    UShell_NotifyIcon(NIM_ADD, &iconData);
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread
void
DesktopIndicatorHandler::doUpdate()
{
    // Modify shell icon
    NOTIFYICONDATAW iconData;

    iconData.cbSize = sizeof(NOTIFYICONDATA);
    iconData.hWnd = m_hWnd;
    iconData.uID = 0;
    UShell_NotifyIcon(NIM_DELETE, &iconData);

    iconData.cbSize = sizeof(NOTIFYICONDATA);
    iconData.hWnd = m_hWnd;
    iconData.uID = 0;
    iconData.uFlags = NIF_MESSAGE |  NIF_ICON | NIF_TIP;
    iconData.uCallbackMessage = WM_DESKTOPINDICATOR_CLICK;
    iconData.hIcon = m_hIcon;
    /* copy the tooltip text up to its maximum size */
    wcsncpy(iconData.szTip, m_lpwszTooltip,
        sizeof(iconData.szTip) / sizeof(iconData.szTip[0]));
    iconData.szTip[
        sizeof(iconData.szTip) / sizeof(iconData.szTip[0]) - 1]
        = L'\0';
    UShell_NotifyIcon(NIM_ADD, &iconData);
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread
void
DesktopIndicatorHandler::doHide()
{
    // Modify shell icon
    NOTIFYICONDATAW iconData;

    iconData.cbSize = sizeof(iconData);
    iconData.hWnd = m_hWnd;
    iconData.uID = 0;
    UShell_NotifyIcon(NIM_DELETE, &iconData);
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread
void
DesktopIndicatorHandler::removeNotify()
{
    // Delete shell icon
    NOTIFYICONDATAW iconData;

    iconData.cbSize = sizeof(iconData);
    iconData.hWnd = m_hWnd;
    iconData.uID = 0;
    UShell_NotifyIcon(NIM_DELETE, &iconData);
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread
void
DesktopIndicatorHandler::restoreApplication()
{
    // Problem here: we should relay it to the Java Swing dispatch thread
    JNIEnv *jniEnv = g_desktopIndicatorThread.m_jniEnv;
    jniEnv->CallVoidMethod(m_joWindowsNotifyUser,
        jniEnv->GetMethodID(
            jniEnv->GetObjectClass(m_joWindowsNotifyUser),
            "restoreApplication", "()V"));
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread
void
DesktopIndicatorHandler::exitApplication()
{
    // Problem here: we should relay it to the Java Swing dispatch thread
    // by posting an Event in the Swing Toolkit's EventQueue, with something
    // like this in Java:
    // Event evt = new Event(NOTIFY_EXIT);
    // EventQueue queue =
    //   Toolkit.getDefaultToolkit().getSystemEventQueue();
    // queue.postEvent(evt);
    // The alternative is to make this dispatch transfer within the
    // WindowsNotifyUser.exitApplication(void) method, and make sure
    // that this method will NOT alter the Swing GUI.
    JNIEnv *jniEnv = g_desktopIndicatorThread.m_jniEnv;
    jniEnv->CallVoidMethod(m_joWindowsNotifyUser,
        jniEnv->GetMethodID(
            jniEnv->GetObjectClass(m_joWindowsNotifyUser),
            "exitApplication", "()V"));
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread
void
DesktopIndicatorHandler::showAboutWindow()
{
    // Problem here: we should relay it to the Java Swing dispatch thread
    // by posting an Event in the Swing Toolkit's EventQueue, with something
    // like this in Java:
    // Event evt = new Event(NOTIFY_ABOUT);
    // EventQueue queue =
    //   Toolkit.getDefaultToolkit().getSystemEventQueue();
    // queue.postEvent(evt);
    // The alternative is to make this dispatch transfer within the
    // WindowsNotifyUser.showAboutWindow(void) method, and make sure
    // that this method will NOT alter the Swing GUI.
    JNIEnv *jniEnv = g_desktopIndicatorThread.m_jniEnv;
    jniEnv->CallVoidMethod(m_joWindowsNotifyUser,
        jniEnv->GetMethodID(
            jniEnv->GetObjectClass(m_joWindowsNotifyUser),
            "showAboutWindow", "()V"));
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread
void
DesktopIndicatorHandler::exitAfterTransfers()
{
    // Problem here: we should relay it to the Java Swing dispatch thread
    // by posting an Event in the Swing Toolkit's EventQueue, with something
    // like this in Java:
    // NotifyEvent evt = new NotifyEvent(EXIT_AFTER_TRANSFER);
    // EventQueue queue =
    //   Toolkit.getDefaultToolkit().getSystemEventQueue();
    // queue.postEvent(evt);
    // The Java application may then implement a EventListener to handle
    // this Event, and call exitAfterTransfers();
    // The alternative is to make this dispatch transfer within the
    // WindowsNotifyUser.exitAfterTransfers(void) method, and make sure
    // that this method will NOT alter the Swing GUI.
    JNIEnv *jniEnv = g_desktopIndicatorThread.m_jniEnv;
    jniEnv->CallVoidMethod(m_joWindowsNotifyUser,
        jniEnv->GetMethodID(
            jniEnv->GetObjectClass(m_joWindowsNotifyUser),
            "exitAfterTransfers", "()V"));
}

/////////////////////////////////////////////////////////////////////////////

// Called from the Windows notification handler thread, in its message loop
LRESULT CALLBACK
DesktopIndicatorHandler::WndProc
(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam)
{
    // Check for our special notification message
    if ((uMessage != WM_DESKTOPINDICATOR_CLICK)) {
        return DefWindowProc(hWnd, uMessage, wParam, lParam);
    }
    DesktopIndicatorHandler *pthis = (DesktopIndicatorHandler *)
        GetWindowLong(hWnd, GWL_USERDATA);
    switch (lParam) {
    case WM_LBUTTONDBLCLK:
        // Execute the default action of the right-click popup menu
        pthis->restoreApplication();
        break;
    //case WM_LBUTTONDOWN:
    //case WM_RBUTTONDOWN:
    //case WM_LBUTTONUP:
        //break;
    case WM_RBUTTONUP: // Display popup menu on right click
        {
            SetForegroundWindow(hWnd);
            POINT pos;
            GetCursorPos(&pos);
            int cxMenuCheck = GetSystemMetrics(SM_CXMENUCHECK);
            int cyMenuCheck = GetSystemMetrics(SM_CYMENUCHECK);
            HMENU hMenu = CreatePopupMenu();
            UAppendMenu(hMenu, MF_STRING, MNU_RESTORE,
                pthis->m_lpwszItemRestore);     // L"Restore";
            UAppendMenu(hMenu, MF_STRING, MNU_ABOUT,
                pthis->m_lpwszItemAbout);       // L"About...";
            UAppendMenu(hMenu, MF_SEPARATOR, 0, (LPCWSTR)NULL);
            UAppendMenu(hMenu, MF_STRING, MNU_EXIT_AFTER,
                pthis->m_lpwszItemExitLater);   // L"Exit After Transfers";
            UAppendMenu(hMenu, MF_STRING, MNU_EXIT,
                pthis->m_lpwszItemExit);        // L"Exit";
            //Load 15x15 bitmaps for unchecked state of menu options
            //Remap colors  #000000, #808080, #C0C0C0, #DFDFDF, #FFFFFF
            //to 3D colors: DlgText, 3DDark, 3DFace, 3DLight, WindowBg
            HBITMAP hbmpRestore = (HBITMAP)LoadImageA(
                ::g_hInstance, MAKEINTRESOURCEA(IDB_RESTORE),
                IMAGE_BITMAP, cxMenuCheck, cyMenuCheck,
                LR_LOADMAP3DCOLORS);
            HBITMAP hbmpHelp = (HBITMAP)LoadImageA(
                ::g_hInstance, MAKEINTRESOURCEA(IDB_HELP),
                IMAGE_BITMAP, cxMenuCheck, cyMenuCheck,
                LR_LOADMAP3DCOLORS);
            HBITMAP hbmpWaitClose = (HBITMAP)LoadImageA(
                ::g_hInstance, MAKEINTRESOURCEA(IDB_WAITCLOSE),
                IMAGE_BITMAP, cxMenuCheck, cyMenuCheck,
                LR_LOADMAP3DCOLORS);
            HBITMAP hbmpClose = (HBITMAP)LoadImageA(
                ::g_hInstance, MAKEINTRESOURCEA(IDB_CLOSE),
                IMAGE_BITMAP, cxMenuCheck, cyMenuCheck,
                LR_LOADMAP3DCOLORS);
            SetMenuItemBitmaps(hMenu, MNU_RESTORE, MF_BYCOMMAND,
                hbmpRestore, (HBITMAP)NULL);
            SetMenuItemBitmaps(hMenu, MNU_ABOUT, MF_BYCOMMAND,
                hbmpHelp, (HBITMAP)NULL);
            SetMenuItemBitmaps(hMenu, MNU_EXIT_AFTER, MF_BYCOMMAND,
                hbmpWaitClose, (HBITMAP)NULL);
            SetMenuItemBitmaps(hMenu, MNU_EXIT, MF_BYCOMMAND,
                hbmpClose, (HBITMAP)NULL);
            SetMenuDefaultItem(hMenu, MNU_RESTORE, MF_BYCOMMAND);
            switch (TrackPopupMenu(hMenu,
                TPM_CENTERALIGN | TPM_BOTTOMALIGN |
                TPM_LEFTBUTTON |
                TPM_NONOTIFY | TPM_RETURNCMD, /* return an item.wID instead of BOOL */
                pos.x, pos.y,
                0, hWnd, (LPCRECT)NULL)) {
            case MNU_RESTORE:
                pthis->restoreApplication();
                break;
            case MNU_ABOUT:
                pthis->showAboutWindow();
                break;
            case MNU_EXIT_AFTER:
                pthis->exitAfterTransfers();
                break;
            case MNU_EXIT:
                pthis->exitApplication();
                break;
            }
            DestroyMenu(hMenu);
            DeleteObject((HGDIOBJ)hbmpRestore);
            DeleteObject((HGDIOBJ)hbmpClose);
        }
        break;
    case WM_DESTROY:
        PostQuitMessage(0);
        break;
    }
    return 0;
}

/////////////////////////////////////////////////////////////////////////////
