/**
* All the conversion code below could have been avoided if Sun
* had documented a Windows-specific set of JNI conversion functions
* that perform the required conversions from Unicode to ANSI on
* Windows 9x/ME.
*
* Sun provides undocumented conversion functions for AWT, Shell
* integration classes, and in the internal JNU implementation.
* However it performs such conversions to/from ACP unconditionally
* even if the system is Unicode compliant.
*
* We'll try to make things at least as well as internal JDK
* functions, by using Win32 Unicode APIs directly on systems that
* support them natively (Win NT/2K/XP).
*/

#include "StdAfx.h"

/*
* Implementation notice:
*
* We definitely can't rely on UTF-8 strings on Windows 9x/ME, as native UTF-8
* support is not part of these systems. Instead they provide only a limited
* set of UTF-16 string conversion fonctions to and from "ANSI" or "OEM"
* codepages only for the filesystem APIs, or only "ANSI" for the GUI APIs.
*
* This implies that we'll work with UTF-16 native strings from the JNI
* interface, and won't use the UTF-8 JNI functions. Then we'll need to perform
* the necessary conversions. A more complex alternative would be to perform
* the conversion to the local "ANSI" codepage directly within Java ByteBuffers
* with help of the java.nio package (but this does not work with the 1.1.x
* versions of the JDK, and it is slow...).
*
* On Win 9x/ME, we'll use only the "ANSI" native APIs, and won't rely on the
* presence of the optional MSLU for Unicode when calling Win32 APIs.
*
* The Windows "ANSI" codepage (ACP) is a placeholder and not a concrete
* codepage, it depends on the local Windows System localized version. The
* same applies to the "OEM" codepage (OCP), however it can be set by the
* user in its running environment. The SBCS "Win1252" codepage is the ANSI
* codepage used by Western European versions of Windows based on ISO-8859-1.
* The MBCS "Win932" concrete codepage is the "ANSI" codepage (and also the
* "OEM" one) for Japanese Kanji versions of Windows based on S-JIS.
*
* The Microsoft Standard C RTL libraries for Windows rely on the ACP codepage
* used internally and on the current POSIX locale (it only supports
* conversions from ACP codepages supported by official localized versions of Windows).
*
* According to the JDK source files for Win32 Shell functions, and AWT
* Unicode support in j2se/src/win32/native/sun/windows, the
* MultiByteToWideChar() and WideCharToMultiByte() Win32 Base APIs are safe
* to use on any Win32 system when converting explicitly to/from the ANSI
* codepage only. It is used by the Microsoft C RTL library when working with
* non default locales, but it may fail if the POSIX locale is Japanese and
* the program runs on European versions of Win 9x/ME without the MSLU.
*
* Note: JNI does not support TLS (Thread Local Storage) because Java's
* System.LoadLibrary does not instanciate the TLS for each new Java thread
* using it (it does not call the DLL's main entrypoint for each new thread
* using it). Shame, the JNI interface requires multithread-safe DLL's!
* But they can't rely on "Multithread Safe" C RTL libraries that use TLS
* static variables for locks, and C++ exception management... (We can
* however use the Win32 exception APIs). This is clearly a limit (a bug?)
* in the current Sun JVM for Windows. This affects the POSIX locale functions
* in the Microsoft C RTL, which use TLS-stored thread environment, so these
* calls may need to create interthread locks on the C RTL libraries that use
* locale dependant functions... So we cannot safely use the POSIX locales
* dependent C RTL functions in a multithreaded DLL for JNI.
*/

/*/////////////////////////////////////////////////////////////////////////*/

/**
* Check whever the host system supports Unicode APIs
*/
UAWIN32API  BOOL
APIENTRY    isWin32UnicodeAPIsSupported()
{
    static BOOL bTestedWin32UnicodeAPIs = FALSE;
    static BOOL bSupportsWin32UnicodeAPIs = FALSE;

    if (!bTestedWin32UnicodeAPIs) {
        bTestedWin32UnicodeAPIs = TRUE;
        if (HIBYTE(HIWORD(GetVersion())) < 0x80) {
            /* Win NT/2K/XP */
            bSupportsWin32UnicodeAPIs = TRUE;
            /* May be we should check here if IMM support
             * is installed by querying the SystemMetrics... */
        } else {
            /* Win16 or Win 9x/ME */
            bSupportsWin32UnicodeAPIs = FALSE;
            /* May be we should check here if the optional
             * support MSLU (MS Layer for Unicode) is present... */
        }
    }
    //return FALSE; /* debug only: force the ANSI APIs on NT */
    return bSupportsWin32UnicodeAPIs;
}

/*/////////////////////////////////////////////////////////////////////////*/

/**
* UTF-16 to MBCS string conversion support for Win 9x/ME. We could use the
* CRT wcstombs() function, however a single unconvertable UTF-16 character
* will make the function fail. Instead we try matching each character
* individually, and will map unconvertable characters to an ASCII SUB (\x1a).
*
* This function will never fail, but assumes that the destination MBCS
* buffer is at least as large as the UTF-16 (wchar_t) encoded string.
* If not it will TRUNCATE silently the result string by storing a final NUL
* byte. Also the C RTL wcstombs() function does not guarantee NUL termination
* of the result string if the destination buffer is too small.
*
* This is a Windows 9x/ME hack, because the Unicode layer is not always
* supported on these native MBCS systems. On Win NT/2K/XP, we won't map
* UTF-16 to MBCS with this function, but will directly use Unicode APIs.
*
* Similar to the Microsoft C RTL mbstowcs() function, but does not depend
* depend on, or refer to, the current POSIX locale for standard C libraries.
* We will only use it on Windows 9x/ME. It relies on the MultiByteToWideChar()
* Win32 Base API that works on any Win9X/ME but only for characters defined
* in the ANSI codepage used by the running system.
*
* Support for other characters requires the optional MSLU installed by the 
* international support components (it can be installed with Win98/ME, but
* on Win95, it can be installed with MSIE4+), and it requires installing the
* NLS codepages for the desired languages.
*
* Caveat: pairs of UTF-16 surrogates, and uncomposed pairs of Unicode
* characters should not be broken before conversion. This version recognizes
* surrogates, but cannot parse uncomposed pairs of Unicode characters.
* We assume that only composed and canonical Unicode characters are present
* in the UTF-16 string. This means that an Unicode "LATIN SMALL LETTER E"
* followed by an Unicode "ACCUTE ACCENT" will not be converted to the "ANSI"
* MBCS equivalent of a precomposed "LATIN SMALL LETTER E WITH ACCUTE" Unicode
* character (Windows 9x/ME without MSLU lacks this support).
*/
UAWIN32API  size_t
APIENTRY    Uwcstombs(
    LPSTR       lpszDest,   /* Windows "ANSI" code page (SBCS or DBCS) */
    LPCWSTR     lpcwszSrc,  /* UTF-16 Unicode Standard */
    size_t      cbDest)
{
    CHAR   szMBC[32];
    size_t count;

    /* optional safety checks */
    if (lpcwszSrc == (LPCWSTR)NULL ||
        lpszDest == (LPSTR)NULL ||
        cbDest == 0) {
        return (size_t)(-1);
    }
    count = 1; /* count the required NUL termination byte */
    while (count < cbDest && *lpcwszSrc != (WCHAR)0) {
        size_t cbMBC;

        if (lpcwszSrc[0] >= (WCHAR)0xD800 && /* leading high surrogate */
            lpcwszSrc[0] <= (WCHAR)0xDBFF &&
            lpcwszSrc[1] >= (WCHAR)0xDC00 && /* trailing low surrogate */
            lpcwszSrc[1] <= (WCHAR)0xDFFF) {
            /* Detect valid pairs of UTF-16 surrogates, to support
             * characters not in UCS2 (such as Chinese/Japanese/Korean)
             * on CJK versions of Win9x/ME, or even Arabic ligatures
             * on Middle-East versions of Win9x/ME. The pair must
             * be converted with boths surrogates together.
             *
             * Don't check composite characters, made of a base character
             * followed by a non-spacing modifier, that could be precomposed
             * successfully during the conversion... We assume that they
             * are already precomposed to a single Unicode character when
             * such a ligature exists in Unicode. */
            cbMBC = WideCharToMultiByte(
                CP_ACP, 0, /* no WC_COMPOSITECHECK flag */
                lpcwszSrc, 2,
                szMBC, sizeof(szMBC),
                NULL, NULL); /* no default char for Win95 compatibility */
            lpcwszSrc += 2;
        } else {
            /* Simple UCS2 characters of the Unicode BMP use only one UTF-16
             * code in a wchar_t and are much simpler to convert... */
            cbMBC = WideCharToMultiByte(
                CP_ACP, 0, /* no WC_COMPOSITECHECK flag */
                lpcwszSrc++, 1,
                szMBC, sizeof(szMBC),
                NULL, NULL); /* no default char for Win95 compatibility */
        }
        /* Check that the Unicode character could be mapped to ANSI
         * If not, use our own default character. */
        if (cbMBC == (size_t)(-1)) {
            /* Unmappable character in the current ANSI locale: map it to an
             * ASCII SUB (26) character, also used as an EOF mark in OEM text
             * files. We cannot use GetCPInfo(CP_ACP, &cpInfo) on Win9x/ME to
             * get the best default character as it depends on MSLU support. */
            *lpszDest++ = '\x1a', count++;
        } else {
            size_t i;

            if (count + cbMBC > cbDest) {
                /* Multibyte char does not fully fit (should not
                 * occur with standard Win 9x MBCS, with the allocator
                 * used in this file)
                 */
                break; // truncate the result
            }
            for (i = 0; i < cbMBC; i++) {
                *lpszDest++ = szMBC[i], count++;
            }
        }
    }
    *lpszDest = '\0';
    return (size_t)(count - 1);
}

/**
* Similar to the Microsoft C RTL mbstowcs() function, but does not depend
* depend on, or refer to, the current POSIX locale for standard C libraries.
* We will only use it on Windows 9x/ME. It relies on the MultiByteToWideChar()
* Win32 Base API that works on any Win9X/ME but only for characters defined
* in the ANSI codepage used by the running system.
*
* Support for other characters requires the optional MSLU installed by the 
* international support components (it can be installed with Win98/ME, but
* on Win95, it can be installed with MSIE4+), and it requires installing the
* NLS codepages for the desired languages.
*/
UAWIN32API  size_t
APIENTRY    Umbstowcs(
    LPWSTR      lpwszDest,    /* UTF-16 Unicode Standard */
    LPCSTR      lpcszSrc,     /* Windows "ANSI" code page (SBCS or DBCS) */
    size_t      cwcDest)
{
    size_t count;

    /* optional safety checks */
    if (lpcszSrc == (LPSTR)NULL ||
        lpwszDest == (LPCWSTR)NULL ||
        cwcDest == 0) {
        return (size_t)(-1);
    }
    count = 1; /* count the required terminating U+0000 character */
    while (count < cwcDest && *lpcszSrc != '\0') {
        /* Find the size of the next MBCS character */
        size_t cbMBC = (size_t)(CharNextA(lpcszSrc) - lpcszSrc);
        /* Ignore uncomposed character sequences in MultiByte String, so this
         * should always be successful and return 1 WCHAR per multibyte
         * character found by CharNextA() unprecomposed sequences in MBCS will
         * become unprecomposed sequences in Unicode. However the function may
         * return a pair of surrogates for a single MBCS character. */
        size_t cwcUTF16 = MultiByteToWideChar(
            CP_ACP, 0, /* no WC_COMPOSITECHECK flag */
            lpcszSrc, cbMBC,
            lpwszDest, cwcDest - count);

        /* Check that the Unicode character could be mapped to ANSI
         * If not, use our own default character. */
        if (cwcUTF16 == (size_t)0) {
            /* Unrecognized MBCS sequence in the local ANSI map: map it to an
             * ASCII SUB (26) character, also used as an EOF mark in OEM text
             * files. This case even occurs with the Win1252 SBCS charset used
             * as the ANSI codepage of Western European versions of Win9x/ME
             * (for example the Euro character encoded 0x80 in this ANSI
             * codepage, but 0x20AC in UTF16, if the system patches for the
             * Euro on Windows 95 are not installed). Also, we cannot use
             * GetCPInfo(CP_ACP, &cpInfo) on Win9x/ME to get the best default
             * character, as it depends on MSLU support.
             * A few "valid" legacy sequences in the ANSI codepage do not
             * translate to Unicode as they are not defined as characters. */
            *lpwszDest++ = L'\x1a', count++;
        } else {
            /* Count 1 simple UCS2 character, or a pair of surrogates */
            count += cwcUTF16;
        }
        lpcszSrc += cbMBC;
    }
    *lpwszDest = L'\0';
    return (size_t)(count - 1);
}

/*/////////////////////////////////////////////////////////////////////////*/

/**
* Using the _alloca() built-in for stack-allocated temporary
* strings is fast and avoids complex exception management.
* But its use is limited to the function using it.
* So we will use macros, not functions...
*/
#define ALLOCA_LPSTR(lpsz, cwc, error_stmt) \
    lpsz = (LPSTR)_alloca((cwc) * 3 + 1);   \
    if (!lpsz) {                            \
        error_stmt;                         \
    }                                       \

/**
* This macro is used to convert parameter strings before
* calling an ANSI version of a WIN32 API.
* Only convert real strings, not user atoms or NULLs.
* Else this is not actually a string, so it is okay to call 
* straight through without converting the parameter
* 1) Get the size of the UTF-16 encoded Unicode string.
* Not locale dependent, so it is safe to use it in JNI.
* 2) Allocate enough space in local stack for the ANSI/MBCS
* string. Not all MBCS encodings are supported: only SBCS
* (ANSI) and DBCS (Asian versions of Win 9x/ME).
* So the factor 3 should be enough.
* 3) Convert the string from UTF-16 to ANSI SBCS/MBCS.
* 4) In case of success, replace the pointer parameter
* 5) The stack-allocated string will be automatically freed
* on return, or in case of exception.
*/
#define LPCWSTR_TO_LPSTR(lpcwsz, error_stmt)                        \
    if ((ULONG)(lpcwsz) > 0xffff) {                                 \
        size_t  cwc = wcslen(lpcwsz);                               \
        LPSTR   lpsz;                                               \
                                                                    \
        ALLOCA_LPSTR(lpsz, cwc, error_stmt);                        \
        if (Uwcstombs(lpsz, lpcwsz, cwc * 3 + 1) == (size_t)(-1)) { \
            error_stmt;                                             \
        }                                                           \
        lpcwsz = (LPCWSTR)lpsz;                                     \
    }

/**
* This macro is used to convert modified parameter strings
* after calling an ANSI version of a WIN32 API.
* Only convert real strings, not user atoms or NULLs.
* Else this is not actually a string, so it is okay to return
* straight through without converting the returned "string".
*/
#define LPWSTR_FROM_LPCSTR(lpcsz, lpwsz, cwc, error_stmt)     \
    if ((ULONG)(lpcsz) > 0xffff && (ULONG)(lpwsz) > 0xffff) { \
        if (Umbstowcs(lpwsz, lpcsz, cwc) == (size_t)(-1)) {   \
            error_stmt;                                       \
        }                                                     \
    }

/*///////////////////////////////////////////////////////////////////////////
// Support for both Win 9x/ME (that may have "dummy" Unicode
// Win32 APIs) and Win NT/2K/XP (that are Unicode enabled).
///////////////////////////////////////////////////////////////////////////*/

/**
* Similar to AppendMenuW() Win32 Base API (user32.dll),
* except that it will work also on Win9x/ME without MSLU support.
*/
UAWIN32API  BOOL
WINAPI      UAppendMenu(
    HMENU      hMenu,
    UINT       uFlags,
    UINT       uIDNewItem,
    LPCWSTR    lpcwszNewItem)   /*UTF-16 IN*/
{
    static BOOL bDisable_AppendMenuW = FALSE;
    if (!bDisable_AppendMenuW &&
        isWin32UnicodeAPIsSupported()) {
        typedef BOOL
            (WINAPI *FARPROC_AppendMenuW)(
            HMENU       hMenu,
            UINT        uFlags,
            UINT        uIDNewItem,
            LPCWSTR     lpcwszNewItem); /*UTF-16 IN*/
        static FARPROC_AppendMenuW farproc_AppendMenuW =
            (FARPROC_AppendMenuW)NULL;

        if (farproc_AppendMenuW == (FARPROC_AppendMenuW)NULL) {
            HMODULE hModule = GetModuleHandleA("user32.dll");
            farproc_AppendMenuW = (FARPROC_AppendMenuW)
                GetProcAddress(hModule, "AppendMenuW");
        }
        if (farproc_AppendMenuW != (FARPROC_AppendMenuW)NULL) {
            return (*farproc_AppendMenuW)(
                hMenu,
                uFlags,
                uIDNewItem,
                lpcwszNewItem);  /*UTF-16 IN*/
        }
        bDisable_AppendMenuW = TRUE;
    }
    /* Convert UTF-16 input strings to ANSI */
    LPCWSTR_TO_LPSTR(lpcwszNewItem,
        return FALSE
        );
    /* Execute the ANSI API */
    return AppendMenuA(
        hMenu,
        uFlags,
        uIDNewItem,
        (LPCSTR)lpcwszNewItem); /*ANSI IN*/
}

/*/////////////////////////////////////////////////////////////////////////*/

/**
* Similar to LoadIconW() Win32 Base API (user32.dll),
* except that it will work also on Win9x/ME without MSLU support.
*/
UAWIN32API  HICON
WINAPI      ULoadIcon(
    HINSTANCE   hInstance,
    LPCWSTR     lpcwszFileName) /*UTF-16 IN*/
{
    static BOOL bDisable_LoadIconW = FALSE;
    if (!bDisable_LoadIconW &&
        isWin32UnicodeAPIsSupported()) {
        typedef HICON
            (WINAPI *FARPROC_LoadIconW)(
            HINSTANCE   hInstance,
            LPCWSTR     lpIconName);    /*UTF-16 IN*/
        static FARPROC_LoadIconW farproc_LoadIconW =
            (FARPROC_LoadIconW)NULL;

        if (farproc_LoadIconW == (FARPROC_LoadIconW)NULL) {
            HMODULE hModule = GetModuleHandleA("user32.dll");
            farproc_LoadIconW = (FARPROC_LoadIconW)
                GetProcAddress(hModule, "LoadIconW");
        }
        if (farproc_LoadIconW != (FARPROC_LoadIconW)NULL) {
            return (*farproc_LoadIconW)(
                hInstance,
                lpcwszFileName);    /*UTF-16 IN*/
        }
        bDisable_LoadIconW = TRUE;
    }
    /* Convert UTF-16 input strings to ANSI */
    LPCWSTR_TO_LPSTR(lpcwszFileName,
        return (HICON)1
        );
    /* Execute the ANSI API */
    return LoadIconA(
        hInstance,
        (LPCSTR)lpcwszFileName);    /*ANSI IN*/
}

/*/////////////////////////////////////////////////////////////////////////*/

/**
* Similar to ShellExecuteW() Win32 Shell API (shell32.dll)
* except that it will work also on Win9x/ME without MSLU support.
*/
UAWIN32API  HINSTANCE
APIENTRY    UShellExecute(
    HWND        hWnd,
    LPCWSTR     lpcwszOperation,    /*UTF-16 IN*/
    LPCWSTR     lpcwszFile,         /*UTF-16 IN*/
    LPCWSTR     lpcwszParameters,   /*UTF-16 IN*/
    LPCWSTR     lpcwszDirectory,    /*UTF-16 IN*/
    INT         nShowCmd)
{
    static BOOL bDisable_ShellExecuteW = FALSE;
    if (!bDisable_ShellExecuteW &&
        isWin32UnicodeAPIsSupported()) {
        typedef HINSTANCE
            (WINAPI *FARPROC_ShellExecuteW)(
            HWND        hWnd,
            LPCWSTR     lpcwszOperation,    /*UTF-16 IN*/
            LPCWSTR     lpcwszFile,         /*UTF-16 IN*/
            LPCWSTR     lpcwszParameters,   /*UTF-16 IN*/
            LPCWSTR     lpcwszDirectory,    /*UTF-16 IN*/
            INT         nShowCmd);
        static FARPROC_ShellExecuteW farproc_ShellExecuteW =
            (FARPROC_ShellExecuteW)NULL;

        if (farproc_ShellExecuteW == (FARPROC_ShellExecuteW)NULL) {
            HMODULE hModule = GetModuleHandleA("shell32.dll");
            farproc_ShellExecuteW = (FARPROC_ShellExecuteW)
                GetProcAddress(hModule, "ShellExecuteW");
        }
        if (farproc_ShellExecuteW != (FARPROC_ShellExecuteW)NULL) {
            return (*farproc_ShellExecuteW)(
                hWnd,
                lpcwszOperation,    /*UTF-16 IN*/
                lpcwszFile,         /*UTF-16 IN*/
                lpcwszParameters,   /*UTF-16 IN*/
                lpcwszDirectory,    /*UTF-16 IN*/
                nShowCmd);
        }
        bDisable_ShellExecuteW = TRUE;
    }
    /* Convert UTF-16 input strings to ANSI */
    LPCWSTR_TO_LPSTR(lpcwszOperation,
        return (HINSTANCE)SE_ERR_ASSOCINCOMPLETE
        );
    LPCWSTR_TO_LPSTR(lpcwszFile,
        return (HINSTANCE)SE_ERR_ASSOCINCOMPLETE
        );
    LPCWSTR_TO_LPSTR(lpcwszParameters,
        return (HINSTANCE)SE_ERR_ASSOCINCOMPLETE
        );
    LPCWSTR_TO_LPSTR(lpcwszDirectory,
        return (HINSTANCE)SE_ERR_ASSOCINCOMPLETE
        );
    /* Execute the ANSI API */
    return ShellExecuteA(
        hWnd,
        (LPCSTR)lpcwszOperation,    /*ANSI IN*/
        (LPCSTR)lpcwszFile,         /*ANSI IN*/
        (LPCSTR)lpcwszParameters,   /*ANSI IN*/
        (LPCSTR)lpcwszDirectory,    /*ANSI IN*/
        nShowCmd);
}

/*/////////////////////////////////////////////////////////////////////////*/

/**
* Similar to ShellExecuteW() Win32 Shell API (shell32.dll),
* except that it will work also on Win9x/ME without MSLU support.
*
* Note: this Win32 function is also missing in the Microsoft Unicode
* Layer for Win9X/ME...
*
* Warning!
* There is no explicit size parameter for the destination buffer.
* This function may store up to 260 characters in the Result
* string, including the terminating NUL character.
*/
UAWIN32API  HINSTANCE
APIENTRY    UFindExecutable(
    LPCWSTR     lpcwszFile,         /*UTF-16 IN*/
    LPCWSTR     lpcwszDirectory,    /*UTF-16 IN*/
    LPWSTR      lpwszResult)        /*UTF-16 OUT*/
{
    HINSTANCE  hInst;
    LPSTR      lpszResult;

    static BOOL bDisable_FindExecutableW = FALSE;
    if (!bDisable_FindExecutableW &&
        isWin32UnicodeAPIsSupported()) {
        typedef HINSTANCE
            (WINAPI *FARPROC_FindExecutableW)(
            LPCWSTR     lpcwszFile,         /*UTF-16 IN*/
            LPCWSTR     lpcwszDirectory,    /*UTF-16 IN*/
            LPWSTR      lpwszResult);       /*UTF-16 OUT*/
        static FARPROC_FindExecutableW farproc_FindExecutableW =
            (FARPROC_FindExecutableW)NULL;

        if (farproc_FindExecutableW == (FARPROC_FindExecutableW)NULL) {
            HMODULE hModule = GetModuleHandleA("shell32.dll");
            farproc_FindExecutableW = (FARPROC_FindExecutableW)
                GetProcAddress(hModule, "FindExecutableW");
        }
        if (farproc_FindExecutableW != (FARPROC_FindExecutableW)NULL) {
            return (*farproc_FindExecutableW)(
                lpcwszFile,         /*UTF-16 IN*/
                lpcwszDirectory,    /*UTF-16 IN*/
                lpwszResult);       /*UTF-16 OUT*/
        }
        bDisable_FindExecutableW = TRUE;
    }
    /* Convert UTF-16 input strings to ANSI */
    LPCWSTR_TO_LPSTR(lpcwszFile,
        return (HINSTANCE)SE_ERR_ASSOCINCOMPLETE
        );
    LPCWSTR_TO_LPSTR(lpcwszDirectory,
        return (HINSTANCE)SE_ERR_ASSOCINCOMPLETE
        );
    /* Prepare temporary ANSI output strings */
    ALLOCA_LPSTR(lpszResult, _MAX_PATH,
        return (HINSTANCE)SE_ERR_OOM
        );
    /* Execute the ANSI API */
    hInst = FindExecutableA(
        (LPCSTR)lpcwszFile,         /*ANSI IN*/
        (LPCSTR)lpcwszDirectory,    /*ANSI IN*/
        lpszResult);                /*ANSI OUT*/
    /* On success, convert temporary output strings to UTF-16 */
    if ((int)hInst > 32) {
        LPWSTR_FROM_LPCSTR(lpszResult, lpwszResult, _MAX_PATH,
            return (HINSTANCE)SE_ERR_OOM
            );
    }
    return hInst;
}

/*/////////////////////////////////////////////////////////////////////////*/

/**
* Similar to ExtractIconW() Win32 Shell API (shell32.dll),
* except that it will work also on Win9x/ME without MSLU support.
*/
UAWIN32API  HICON
APIENTRY    UExtractIcon(
    HINSTANCE  hInstance,
    LPCWSTR    lpcwszExeFileName,   /*UTF-16 IN*/
    UINT       nIconIndex)
{
    static BOOL bDisable_ExtractIconW = FALSE;
    if (!bDisable_ExtractIconW &&
        isWin32UnicodeAPIsSupported()) {
        typedef HICON
            (WINAPI *FARPROC_ExtractIconW)(
            HINSTANCE   hInstance,
            LPCWSTR     lpcwszExeFileName,  /*UTF-16 IN*/
            UINT        nIconIndex);
        static FARPROC_ExtractIconW farproc_ExtractIconW =
            (FARPROC_ExtractIconW)NULL;

        if (farproc_ExtractIconW == (FARPROC_ExtractIconW)NULL) {
            HMODULE hModule = GetModuleHandleA("shell32.dll");
            farproc_ExtractIconW = (FARPROC_ExtractIconW)
                GetProcAddress(hModule, "ExtractIconW");
        }
        if (farproc_ExtractIconW != (FARPROC_ExtractIconW)NULL) {
            return (*farproc_ExtractIconW)(
                hInstance,
                lpcwszExeFileName,  /*UTF-16 IN*/
                nIconIndex);
        }
        bDisable_ExtractIconW = TRUE;
    }
    /* Convert UTF-16 input strings to ANSI */
    LPCWSTR_TO_LPSTR(lpcwszExeFileName,
        return (HICON)1 /*not an icon file*/
        );
    /* Execute the ANSI API */
    return ExtractIconA(
        hInstance,
        (LPCSTR)lpcwszExeFileName,  /*ANSI IN*/
        nIconIndex);
}

/*/////////////////////////////////////////////////////////////////////////*/

/**
* Similar to Shell_NotifyIconW() Win32 Shell API (shell32.dll),
* except that it will work also on Win9x/ME without MSLU support.
*
* Note: this version does not support the supplementary
* version 5.0 fields when converting to the ANSI API.
*/
UAWIN32API  BOOL
APIENTRY    UShell_NotifyIcon(
    DWORD               dwMessage,
    PNOTIFYICONDATAW    lpData) /*UTF-16 IN fields*/
{
    static BOOL bDisable_Shell_NotifyIconW = FALSE;
    if (!bDisable_Shell_NotifyIconW &&
        isWin32UnicodeAPIsSupported()) {
        typedef BOOL
            (WINAPI *FARPROC_Shell_NotifyIconW)(
            DWORD               dwMessage,
            PNOTIFYICONDATAW    lpData);    /*UTF-16 IN fields*/
        static FARPROC_Shell_NotifyIconW farproc_Shell_NotifyIconW =
            (FARPROC_Shell_NotifyIconW)NULL;

        if (farproc_Shell_NotifyIconW == (FARPROC_Shell_NotifyIconW)NULL) {
            HMODULE hModule = GetModuleHandleA("shell32.dll");
            farproc_Shell_NotifyIconW = (FARPROC_Shell_NotifyIconW)
                GetProcAddress(hModule, "Shell_NotifyIconW");
        }
        if (farproc_Shell_NotifyIconW != (FARPROC_Shell_NotifyIconW)NULL) {
            return (*farproc_Shell_NotifyIconW)(
                dwMessage,
                lpData);    /*UTF-16 IN fields*/
        }
        bDisable_Shell_NotifyIconW = TRUE;
    }
    /* don't support version 5.0 features with the ANSI conversion */
    /* support only the V1 notify structure (up to the 64-chars tip) */
    if (dwMessage == NIM_ADD || dwMessage == NIM_MODIFY ||
        dwMessage == NIM_DELETE) {
        /* Convert UTF-16 input strings to ANSI */
        NOTIFYICONDATAA data;
        /* Warning! Newer versions of VC++ extend this structure, for
         * the Windows 2000/XP extended Shell APIs.
         * If we set cbSize to larger values, it specified V5 or higher
         * behavior: this ensures VC98 headers compatibility.
         * (Focus state, balloons info and title won't be mapped) */

        data.cbSize = (DWORD)((LPSTR)&data.szTip[64] - (LPSTR)&data);
        data.hWnd   = lpData->hWnd;
        data.uID    = lpData->uID;
        if (dwMessage != NIM_DELETE) {
            data.uFlags = lpData->uFlags &
                ~(NIF_MESSAGE | NIF_ICON | NIF_TIP);
            if (data.uFlags == 0 && dwMessage == NIM_MODIFY) {
                return TRUE; /* trivial modify nothing */
            }
            if ((data.uFlags & NIF_MESSAGE) != 0) {
                data.uCallbackMessage = lpData->uCallbackMessage;
            } else {
                data.uCallbackMessage = (UINT)0;
            }
            if ((data.uFlags & NIF_ICON) != 0) {
                data.hIcon = lpData->hIcon;
            } else {
                data.hIcon = (HICON)NULL;
            }
            if ((lpData->uFlags & NIF_TIP) != 0) {
                if (Uwcstombs(data.szTip, lpData->szTip,
                    sizeof(data.szTip)) == (size_t)(-1)) {
                    return FALSE;
                }
            } else {
                data.szTip[0] = '\0';
            }
        }
        return Shell_NotifyIconA(
            dwMessage,
            &data);     /*ANSI IN fields*/
    }
    return FALSE;
}

/*/////////////////////////////////////////////////////////////////////////*/
