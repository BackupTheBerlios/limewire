/**
* File: Win32UnicodeAPIs.h
* Add support for Win32 APIs with Unicode on Win 9x/ME.
*/
#ifndef _Win32UnicodeAPIs_h
#define _Win32UnicodeAPIs_h

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "StdAfx.h"

#include "jni.h"

#ifndef UAWIN32API
#if defined LIMEWIRE_EXPORTS /* for now in limewire.dll */
#define UAWIN32API  __declspec(dllexport)
#elif defined LIMEWIRE_NOIMPORTS /* statically linked */
#define UAWIN32API  
#else
#define UAWIN32API  __declspec(dllimport)
#endif
#endif

CPP_EXTERNC_BEGIN

/////////////////////////////////////////////////////////////////////////////
//
// Support tools for Unicode UTF-16 wrappers.
// Needed for Windows 9x/ME without relying on MSLU presence.
//
/////////////////////////////////////////////////////////////////////////////

/**
* Similar to wcstombs() function in Microsoft C RTL
* (but locale-independant: always use the local "ANSI" codepage,
* and the APIENTRY call convention, faster than __cdecl).
*/
UAWIN32API  size_t
APIENTRY    Uwcstombs
(LPSTR      lpszDest,    /*Windows "ANSI" codepage (SBCS or DBCS)*/
 LPCWSTR    lpcwszSrc,   /*UTF-16 and Java jchars*/
 size_t     cbDest);

/**
* Similar to mbstowcs() function in Microsoft C RTL
* (but locale-independant: always use the local "ANSI" codepage,
* and the APIENTRY call convention, faster than __cdecl).
*/
UAWIN32API  size_t
APIENTRY    Umbstowcs
(LPWSTR     lpwszDest,   /*UTF-16 and Java jchars*/
 LPCSTR     lpcszSrc,    /*Windows "ANSI" codepage (SBCS or DBCS)*/
 size_t     cwcDest);

/**
* Check whever the running platform supports Unicode APIs,
* Return TRUE for Windows NT/2K/XP.
*/
UAWIN32API  BOOL
APIENTRY    isWin32UnicodeAPIsSupported();

/////////////////////////////////////////////////////////////////////////////
//
// Uxxx() wrapper functions, that can directly use either:
// - Win32 Unicode xxxW() APIs using WCHAR if supported, or
// - Win32 "ANSI" xxxA() APIs using CHAR (through MBCS conversion)
//
/////////////////////////////////////////////////////////////////////////////

/**
* Similar to AppendMenuW() Win32 Base API (user32.dll)
*/
UAWIN32API  BOOL
WINAPI      UAppendMenu
(HMENU      hMenu,
 UINT       uFlags,
 UINT       uIDNewItem,
 LPCWSTR    lpcwszNewItem); /*UTF-16*/

/**
* Similar to LoadIconW() Win32 Base API (user32.dll)
*/
UAWIN32API  HICON
WINAPI      ULoadIcon
(HINSTANCE  hInst,
 LPCWSTR    lpcwszFileName); /*UTF-16*/

/////////////////////////////////////////////////////////////////////////////

/**
* Similar to ShellExecuteW() Win32 Shell API (shell32.dll)
*/
UAWIN32API  HINSTANCE
APIENTRY    UShellExecute
(HWND       hwnd,
 LPCWSTR    lpOperation,    /*UTF-16*/
 LPCWSTR    lpFile,         /*UTF-16*/
 LPCWSTR    lpParameters,   /*UTF-16*/
 LPCWSTR    lpDirectory,    /*UTF-16*/
 INT        nShowCmd);

/**
* Similar to FindExecutableW() Win32 Shell API (shell32.dll)
* (This function is missing in the Microsoft Layer for Unicode)
*/
UAWIN32API  HINSTANCE
APIENTRY    UFindExecutable
(LPCWSTR    lpFile,         /*UTF-16*/
 LPCWSTR    lpDirectory,    /*UTF-16*/
 LPWSTR     lpResult);      /*UTF-16*/

/**
* Similar to ExtractIconW() Win32 Shell API (shell32.dll)
*/
UAWIN32API  HICON
APIENTRY    UExtractIcon
(HINSTANCE  hInst,
 LPCWSTR    lpcwszExeFileName,
 UINT       nIconIndex);

/**
* Similar to Shell_NotifyIconW() Win32 Shell API (shell32.dll)
* (This function is missing in the Microsoft Layer for Unicode)
*/
UAWIN32API  BOOL
APIENTRY    UShell_NotifyIcon
(DWORD      dwMessage,
 PNOTIFYICONDATAW lpData);  /*UTF-16*/

/////////////////////////////////////////////////////////////////////////////

CPP_EXTERNC_END

#endif /* _Win32UnicodeAPIs_h */
