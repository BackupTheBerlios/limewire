//
// File: DesktopIndicatorImages.cpp
//
#include "StdAfx.h"
#include "DesktopIndicatorImages.hpp"

/////////////////////////////////////////////////////////////////////////////

DesktopIndicatorImages g_desktopIndicatorImages;

/////////////////////////////////////////////////////////////////////////////

HICON
DesktopIndicatorImages::add
(LPCWSTR  lpcwszFilename)
{
    // Extract first icon from icon file
    return ::UExtractIcon(g_hInstance, lpcwszFilename, 0);
}

/////////////////////////////////////////////////////////////////////////////

// DVB19Oct99 - new version of add that accepts resource id, and assumes icon is in this DLL
HICON
DesktopIndicatorImages::add
(WORD  wResourceId)
{
    // Load icon from this dll's resource fork
    return ::LoadIconA(g_hInstance, (LPCSTR)MAKEINTRESOURCE(wResourceId));
}

/////////////////////////////////////////////////////////////////////////////

void
DesktopIndicatorImages::remove
(HICON  hIcon)
{
    if (hIcon) {
        // Destroy icon
        ::DestroyIcon(hIcon);
    }
}

/////////////////////////////////////////////////////////////////////////////
