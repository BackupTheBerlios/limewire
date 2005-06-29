@echo off

set CLASSPATH=^
.;..\core;..\lib\messagebundles\;^
..\lib\jars\other\themes.jar;..\lib\jars\other\log4j.jar;^
..\lib\jars\xerces.jar;..\lib\jars\jl011.jar;..\lib\jars\logicrypto.jar;^
..\lib\jars\mp3sp14.jar;..\lib\jars\commons-httpclient.jar;..\lib\jars\commons-logging.jar;^
..\lib\jars\i18n.jar;..\lib\jars\icu4j.jar;..\lib\jars\ProgressTabs.jar;..\lib\jars\id3v2.jar;^
..\lib\jars\jcraft.jar;..\lib\jars\looks.jar;..\lib\jars\daap.jar;..\lib\jars\jmdns.jar;^
..\lib\jars\tritonus.jar;..\lib\jars\vorbis.jar;..\lib\jars\clink.jar;..\lib\jars\xml-apis.jar

@set OLDPATH=%PATH%
@set PATH=%PATH%;..\lib\native\windows
java -ss32k -oss32k -ms4m -Xminf0.10 -Xmaxf0.25 -Djava.net.preferIPv4Stack=true com.limegroup.gnutella.gui.Main
@set PATH=%OLDPATH%

REM Alternate Java options for developers:
REM * deprecated JRI native stack size options for Java before 1.4
REM     -ss32k -oss32k 
REM * total VM memory pool settings for system integration (accurate for -client)
REM   but raises the garbage collector too often on servers.
REM     -ms4m -Xminf0.1 -Xmaxf0.25
REM * alternate VM choice for LimeWire running 24\24 7\7 with dedicated memory:
REM     -server -ms8m -Xminf0.25 -Xmaxf0.50
REM * basic settings to tune up VM garbage collector generations (Java 1.4)
REM     -XX:NewRatio=6 -XX:NewSize=3M -XX:SurvivorRatio=18 -XX:TargetSurvivorRatio=75
REM * settings to tune up the Java-bytecode to native-code runtime compiler
REM   requires tuning first the VM memory pool size
REM     -XX:+PrintCompilation -XX:CompileThreshold=1024 -XX:-CITime
REM * settings for the built-in profiler (generates java.hprof.txt in start folder)
REM     -Xrunhprof:cpu=samples,depth=6,thread=y
REM How to run the Profiler Analyzer (requires PerfAnal.jar from Sun's website)
REM java -Djava.util.logging.config.file=jdk14.logging.properties -jar lib\PerfAnal.jar java.hprof.txt
