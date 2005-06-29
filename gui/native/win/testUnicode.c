#include <stdio.h>
#include "Win32UnicodeAPIs.h"

int main()
{
    const WCHAR cwszSrc[] = {
        L'\x3041', // U+3041: Hiragana lowercase a syllab
        L'\xFF21', // U+FF21: Full-width Latin uppercase A letter
        L'\xFFE6', // U+FFE6: Full-width Korean Won currency symbol
        L'\xD800', L'\xDC41', // U+100041: surrogate pair
        0
    };

    LPCWSTR      lpcwszSrc;
    CHAR         szDest[256];
    LPSTR        lpszDest;
    size_t       cbDest;
    size_t       len;
    int          i;

    lpcwszSrc = cwszSrc;
    lpszDest  = szDest;
    cbDest = sizeof(szDest);
    len = Uwcstombs(lpszDest, lpcwszSrc, cbDest);
    printf("%d:", len);
    printf("%s.\n", lpszDest);

}
