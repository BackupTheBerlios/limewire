Beta internationalized version of LimeWire20.dll for Windows.

This project was written several months ago to replace the existing support DLL 
needed on Windows to support the system tray icon and its menu, and to support 
launching external files such as a browser associated to a HTML file, or a 
system default media player to play files in the library.

It now supports internationalized menus displaying the correct characters, read 
from the MessagesBundle using the Java GUIMediator through JNI. It can also 
successfully launch media files containing non ASCII characters in their names.

It was not released for now due to an unsolved compatibility issue with Windows 
95 (for an unknown reason, the Java VM refuses to load it through JNI...

See extended comments in Win32UnicodeAPIs.c (.h) about the compatibility issues.

The main change in this version is that the C/C++ code does no more get/set 
UTF-8 strings through the JNI interface, but instead uses the native UTF-16 
encoding used by Java strings.

UTF-8 strings are not usable to display internationlized menus on any version 
of Windows, but Windows NT/2000/XP can use UTF-16 strings directly with the 
Unicode version of Win32 APIs.

However Unicode versions of Win32 APIs only work on NT/2000/XP (or on Windows 
9x/ME in a limited way, provided that the optional Microsoft Layer for Unicode, 
alias MSLU, is installed). Using Unicode APIs is the best choice for these 
NT-based systems.

It can be done automatically at compile time by using APIs without A or W 
suffixes, by defining a macro. But the compled code will only run on NT/2000/XP, 
as Windows 9x/ME versions of these APIs are No-ops that return an error status.

So the DLL is compiled in a compatible mode, where default APIs are mapped to 
their ANSI version. Using Unicode versions of these APIs requires detecting 
first if we are running on an NT-based system.

However, the code is a little more complex, as it cannot use if/else constructs 
to determine which API to use (Unicode or "ANSI"): the DLL would be bound to 
DLL entries that don't exist on Windows 9x, and the DLL will not be loaded due 
to unresolved references at run-time. Instead, it uses the LoadLibrary to 
locate Unicode APIs, and if not found it will use ANSI APIs.

As Java JNI provides strings in Unicode format only, we need a way to convert 
these strings to their ANSI encoding when appropriate. This support is built 
into Win32UnicodeAPIs.c, using code and technics derived from the Microsoft C 
RTL library sources and from Java VM 1.4 internal implementation C sources.

There are somme complications to know when internationalizing LimeWire on 
these "ANSI"-only platforms (Windows 95/98/98SE/ME): the ANSI encoding is just 
a logical name which maps to several distinct physical encodings, depending on 
the platform type, version and localization: there is no such "ANSI" 
chararacter set, but "codepages" named according to the Microsoft SDK with 
numbers such as 1252 for Western European versions of Windows, 1250 for Eastern 
European versions, 1251 for Cyrillic versions (Russian, Bulgarian, Serbian, 
Macedonian), ...

The situation is more complicate because some ANSI encodings used on Windows 
are  not all single-byte encoded: Chinese, Japanese and Korean versions of 
Windows use specific codepages (between 900 and 999) based on a simplified 
version of national standards (Shift-JIS for Japanese, KSC5601 for Korean, 
GB2312 for Simplified Chinese in PRC and Singapore, Big5 for Traditional 
Chinese in Taiwan, Macau and Hong Kong). These encodings are "multibyte 
character sets" (MBCS), where some characters (in fact only printable ASCII 
characters in their standard half-width and orientable variants) are encoded 
with one byte, while others are encoded with a leading byte and one or more 
trailing bytes from a distinct subset of byte values.

Including support for SBCS encodings (European Latin 1 and 2, Cyrillic, Greek, 
Hebrew, Arabic...)  would require small lookup tables for each detected SBCS 
ANSI encoding. However adding support for MBCS encodings would be a huge task 
requiring building up large lookup tables which in fact are already built into 
the supporting OS.

So this implementation uses the MultibyteToWideChar() and WideCharToMultibyte() 
Win32 APIs to make these conversions, only between Unicode  UTF-16 and the 
OS-supported ANSI codepage (other conversions are possible but supported only 
on NT-based systems with additional encoding tables installed). According to 
Microsoft in MSDN, and to the Microsoft C RTL sources, and to the Sun Java 1.4 
C sources for Windows, these two APIs will work on all Win32 platforms (this is 
actually the case in all applications built with Visual Studio and in all Sun 
Java VM implementations for Windows).

STATUS:

Currently this DLL works great on NT/2000/XP and allows full 
internationalization of Limewire, as a FULLY COMPATIBLE REPLACEMENT for the 
current version of the DLL shipped with LimeWire since version 1.9 up to the 
current version 2.8.3: it requires no change in the current Java source classes 
of Limewire or in Java JNI interface classes. It even works with all released 
versions of LimeWire since version 1.9 without recompiling them. It just 
requires some properties in the MessagesBundle for the internationalized system 
tray menu options and its tooltip.

TODO:

For an unknown reason, it still has a problem to load on Windows 9x/ME, despite 
it was specifically and carefully designed to run on these platforms. This is 
an unsolved bug, and if you have suggestions to solve this problem, they would 
be welcome, as it would allow LimeWire to include a better integration code on 
Windows.
