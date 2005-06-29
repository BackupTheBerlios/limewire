/*
* File: StdAfx.h
* Include file for standard system include files, or project-specific
* include files that are used frequently, but are changed infrequently
*/
#ifndef _StdAfx_h
#define _StdAfx_h

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

// Insert your headers here
//#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers
#define OEMRESOURCE                     // Include system shared OEM bitmaps

#include <windows.h>
#include <shellapi.h>

#include <stdlib.h>
#include <malloc.h>

#if defined(__cplusplus)
#define CPP_EXTERNC_BEGIN extern "C" {
#define CPP_EXTERNC_END   }
#else
#define CPP_EXTERNC_BEGIN //empty
#define CPP_EXTERNC_END   //empty
#endif

#include "resource.h"
#include "Win32UnicodeAPIs.h"

extern HINSTANCE g_hInstance;

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.
#endif /* _StdAfx_h */
