// StdAfx.cpp : source file that includes just the standard includes
//	win32.pch will be the pre-compiled header
//	StdAfx.obj will contain the pre-compiled type information

#include "StdAfx.h"
// TODO: reference any additional headers you need in StdAfx.h
// and not in this file

/////////////////////////////////////////////////////////////////////////////

HINSTANCE g_hInstance = NULL;

BOOL WINAPI
DllMain(
    HINSTANCE   hInstDll,       // handle to DLL module
    DWORD       dwReason,       // reason for calling function
    LPVOID      lpReserved)     // reserved
{
    switch (dwReason) {
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
    case DLL_PROCESS_DETACH:
    case DLL_PROCESS_ATTACH:
        g_hInstance = hInstDll;
        break;
    }
    return TRUE;
}

/////////////////////////////////////////////////////////////////////////////
