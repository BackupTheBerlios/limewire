//
// File: DesktopIndicatorImages.hpp
//
#ifndef _DesktopIndicatorImages_hpp
#define _DesktopIndicatorImages_hpp

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class DesktopIndicatorImages
{
public:
    HICON add(WORD wResourceId);      // loads icon from this DLL (DVB19Oct99)
    HICON add(LPCWSTR lpwszFilename); // creates icon handle from file
    void remove(HICON hIcon);
};

extern DesktopIndicatorImages g_desktopIndicatorImages;

#endif // _DesktopIndicatorImages_hpp
