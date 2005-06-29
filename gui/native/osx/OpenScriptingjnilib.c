
/*
 * Roger Kapsi's Java Package
 * Copyright (C) 2003 Roger Kapsi
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
http://developer.apple.com/qa/qa2001/qa1026.html
http://developer.apple.com/qa/qa2001/qa1111.html
*/

#include <JavaVM/jni.h>
#include <Carbon/Carbon.h>

#define OSA_SCRIPT(func)	Java_de_kapsi_util_OSAScript_##func


///////////////////////////////////////////////////////////////////////////////
//////////////////////// Shared functions and data types //////////////////////
///////////////////////////////////////////////////////////////////////////////

/**
 * The 'ptr' of OSAScript.java points to a SharedOSAScriptData
 * strcture. 
 */
typedef struct {
    ComponentInstance theComponent;
    OSAID scriptID;
    OSAID resultID;
    AEDesc scriptTextDesc;
} SharedOSAScriptData;

/**
 * Creates an AEDescList from a jobjectArray of strings
 */
static OSStatus CreateAEDescList(JNIEnv *env, jobjectArray arr, AEDescList *result) {
	
	OSStatus err = noErr;
	
	err = AECreateList(0, 0, false, result);
	
	if (err == noErr) {
	
		jsize length = (*env)->GetArrayLength(env, arr);
		
		jsize index;
		jstring item;
		const char* str;
		
		for(index = 0; index < length; index++) {
			
			item = (jstring)(*env)->GetObjectArrayElement(env, arr, index);
			str  = (*env)->GetStringUTFChars(env, item, JNI_FALSE);
			
			err = AEPutPtr(result, 0, typeText, str, 
						(*env)->GetStringLength(env, item));
	
			(*env)->ReleaseStringUTFChars(env, item, str);
			
			if (err != noErr) {
				AEDisposeDesc(result);
				break; // exit on error
			}
		}
	}
		
	return err;
}

/**
 * Copies the contents of 'src' into 'dst'
 */
static OSStatus CopyAEDescData(JNIEnv *env, AEDesc *src, jbyteArray dst, jsize pos, jsize length) {
	
	OSStatus err = noErr;
	
	jbyte *dst0 = (*env)->GetByteArrayElements(env, dst, JNI_FALSE);
						
	err = AEGetDescData(src, 
						&dst0[pos], 
						length);
								
	(*env)->ReleaseByteArrayElements(env, dst, dst0, 0);
	
	return err;
}

/**
 * Creates an Apple Event
 */
static OSStatus CreateAppleEvent(JNIEnv *env, jstring func, jobjectArray args, AppleEvent *result) {
    
    OSStatus err = noErr;
	
    AEAddressDesc targetAddr;
	AEDescList theParameters;
	
	AECreateDesc(typeNull, 0, 0, result);
	AECreateDesc(typeNull, 0, 0, &targetAddr);
	
	ProcessSerialNumber PSN = {0, kCurrentProcess};
	
    err = AECreateDesc(typeProcessSerialNumber, 
						(Ptr) &PSN, 
						sizeof(PSN), 
						&targetAddr);
    
    if (err == noErr) {
        
		err = AECreateAppleEvent('ascr', 
							 kASSubroutineEvent, 
							 &targetAddr, 
							 kAutoGenerateReturnID, 
							 kAnyTransactionID, 
							 result);
    
		if (err == noErr) {
			err = CreateAEDescList(env, args, &theParameters);
		
			if (err == noErr) {
			
				err = AEPutParamDesc(result, 
								keyDirectObject, 
								&theParameters);
				
				if (err == noErr) {
					const char* c_func = (*env)->GetStringUTFChars(env, func, JNI_FALSE);
					
					err = AEPutParamPtr(result, 
										keyASSubroutineName,
										typeText, 
										c_func, 
										(*env)->GetStringLength(env, func));
					
					(*env)->ReleaseStringUTFChars(env, func, c_func);
				}
				
				if (err != noErr) {
					AEDisposeDesc(result);
				}
			}
		}
    }

    AEDisposeDesc(&targetAddr);
    AEDisposeDesc(&theParameters);
	
    return err;
}

///////////////////////////////////////////////////////////////////////////////
//////////////////////////// Functions for OSAScript //////////////////////////
///////////////////////////////////////////////////////////////////////////////
 
/**
 * Creates a new 'OSAScript' from source and returns the 'ptr'
 * to the SharedOSAScriptData
 */
JNIEXPORT jint JNICALL OSA_SCRIPT(NewOSAScriptWithSrc)
    (JNIEnv *env, jclass clazz, jintArray ptr, jstring src)
{

    OSStatus err = noErr;
	
	ComponentInstance osaComponent = 
			OpenDefaultComponent(kOSAComponentType, kAppleScriptSubtype);
	
	if (osaComponent != 0) {
		
		SharedOSAScriptData *data = 
			(SharedOSAScriptData*)malloc(sizeof(SharedOSAScriptData));
		
		if (data != 0) {
			
			data->theComponent = osaComponent;
			data->scriptID = kOSANullScript;
			data->resultID = kOSANullScript;
	
			const char *c_src = (*env)->GetStringUTFChars(env, src, JNI_FALSE);
			jsize length = (*env)->GetStringLength(env, src);
			
			err = AECreateDesc(typeChar, 
							c_src, 
							length, 
							&data->scriptTextDesc);
			
			(*env)->ReleaseStringUTFChars(env, src, c_src);
			
			jint *ptr0 = (*env)->GetIntArrayElements(env, ptr, JNI_FALSE);
			ptr0[0] = (jint)data;
			(*env)->ReleaseIntArrayElements(env, ptr, ptr0, 0);
		}
	}
	
	return (jint)err;
}

/**
 * Creates an new 'OSAScript' from binaries and returns the 'ptr'
 * to the SharedOSAScriptData
 */
JNIEXPORT jint JNICALL OSA_SCRIPT(NewOSAScriptWithBin)
	(JNIEnv *env, jclass clazz, jintArray ptr, jbyteArray bin)
{
    OSStatus err = noErr;
	
	ComponentInstance osaComponent = 
			OpenDefaultComponent(kOSAComponentType, kAppleScriptSubtype);
		
	if (osaComponent != 0) {
		
		SharedOSAScriptData *data = 
			(SharedOSAScriptData*)malloc(sizeof(SharedOSAScriptData));
	
		if (data != 0) {
		
			data->theComponent = osaComponent;
			data->scriptID = kOSANullScript;
			data->resultID = kOSANullScript;
		
			jsize length = (*env)->GetArrayLength(env, bin);
			jbyte *bin0 = (*env)->GetByteArrayElements(env, bin, JNI_FALSE);
			
			err = AECreateDesc(typeOSAGenericStorage, 
								bin0, 
								(Size)length, 
								&data->scriptTextDesc);
			
			(*env)->ReleaseByteArrayElements(env, bin, bin0, 0);
			
			if (err == noErr) {
			
				err = OSALoad(data->theComponent, 
								&data->scriptTextDesc, 
								kOSAModeCompileIntoContext, 
								&data->scriptID);
			}
			
			jint *ptr0 = (*env)->GetIntArrayElements(env, ptr, JNI_FALSE);
			ptr0[0] = (jint)data;
			(*env)->ReleaseIntArrayElements(env, ptr, ptr0, 0);
		}
	}
	
	return (jint)err;
}

/**
 * Compiles the script and returns an error code.
 */
JNIEXPORT jint JNICALL OSA_SCRIPT(CompileOSAScript)
  (JNIEnv *env, jclass clazz, jint ptr)
{
    OSStatus err = noErr;
    
    if (ptr > 0) {
        SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
        
		err = OSACompile(data->theComponent, 
						&data->scriptTextDesc, 
						kOSAModeCompileIntoContext, 
						&data->scriptID);
    }
	
	return (jint)err;
}

/**
 * Executes the script and returns an error code.
 */
JNIEXPORT jint JNICALL OSA_SCRIPT(ExecuteOSAScript)
  (JNIEnv *env, jclass clazz, jint ptr)
{
    OSStatus err = noErr;
    
    if (ptr > 0) {
	
        SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
		
        err = OSAExecute(data->theComponent, 
						 data->scriptID, 
						 kOSANullScript, 
						 kOSAModeNull, 
						 &data->resultID);
    }
    
	return (jint)err;
}

/**
 * Executes a subroutine of the script with optional parameters and returns 
 * an error code.
 */
JNIEXPORT jint JNICALL OSA_SCRIPT(ExecuteOSAScriptEvent)
  (JNIEnv *env, jclass clazz, jint ptr, jstring subroutine, jobjectArray args)
{
    OSStatus err = noErr;
    
    if (ptr > 0) {
        
		SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
	
        AppleEvent theAEvent;
        
		err = CreateAppleEvent(env, subroutine, args, &theAEvent);
		
        if (err == noErr) {
            
            err = OSAExecuteEvent(data->theComponent, 
								  &theAEvent,
								  data->scriptID, 
								  kOSAModeNull, 
								  &data->resultID);
			
			AEDisposeDesc(&theAEvent);
        }
    }
    
	return (jint)err;
}

/**
 * Returns the size of this script in bytes
 */
JNIEXPORT jsize JNICALL OSA_SCRIPT(GetOSAScriptSize)
	(JNIEnv *env, jclass clazz, jint ptr)
{
	OSStatus err = noErr;
	jsize size = 0;
	
	if (ptr > 0) {
        SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
        
		AEDesc result = { typeNull, 0 };
		AECreateDesc(typeNull, 0, 0, &result);
        
		err = OSAStore(data->theComponent, 
					   data->scriptID,
					   typeOSAGenericStorage, 
					   kOSAModeNull, 
					   &result);
					
        if (err == noErr) {
            
            size = (jsize)AEGetDescDataSize(&result);
		}
		
		AEDisposeDesc(&result);
	}
	
	return size;
}

/**
 * Returns the binaries of this script (do not call if script isn't compiled!)
 */
JNIEXPORT jint JNICALL OSA_SCRIPT(GetOSAScript)
  (JNIEnv *env, jclass clazz, jint ptr, jbyteArray result, jsize pos, jsize length)
{
    OSStatus err = noErr;
    
    if (ptr > 0) {
        SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
        
		AEDesc tmp = { typeNull, 0 };
		AECreateDesc(typeNull, 0, 0, &tmp);
        
		err = OSAStore(data->theComponent, 
					   data->scriptID,
					   typeOSAGenericStorage, 
					   kOSAModeNull, 
					   &tmp);
					
        if (err == noErr) {
		
			err = CopyAEDescData(env, &tmp, result, pos, length);
        }
		
		AEDisposeDesc(&tmp);
    }
    
    return (jint)err;
}

/**
 * Releases the allocated memory (delete the 'OSAScript')...
 */
JNIEXPORT void JNICALL OSA_SCRIPT(ReleaseOSAScript)
	(JNIEnv *env, jclass clazz, jint ptr)
{
    if (ptr > 0) {
        SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
        
		AEDisposeDesc(&data->scriptTextDesc);
		
        if (data->scriptID != kOSANullScript) {
            OSADispose(data->theComponent, data->scriptID);
		}
		
        if (data->resultID != kOSANullScript) {
            OSADispose(data->theComponent, data->resultID);
		}
		
        if (data->theComponent != 0) {
            CloseComponent(data->theComponent);
		}
		 
        free(data);
    }
}

/**
 * Returns the Error Number from the last execution
 */
JNIEXPORT jint JNICALL OSA_SCRIPT(GetErrorNumber)
	(JNIEnv *env, jclass clazz, jint ptr)
{
	OSStatus err = noErr;
	
	jint errorNumber = 0;
	
	if (ptr > 0) {
		
		SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
		
		AEDesc result = { typeNull, 0 };
        AECreateDesc(typeNull, 0, 0, &result);
		
		err = OSAScriptError(data->theComponent, 
								kOSAErrorNumber,
								typeShortInteger, 
								&result);
		
		if (err == noErr && result.descriptorType != typeNull) {
			
			AEDesc tmp = { typeNull, 0 };
			
			err = AECoerceDesc(&result, 
								typeInteger, 
								&tmp);
			
			if (err == noErr) {
				errorNumber = (jint)(**(int**)tmp.dataHandle);
			}
			
			AEDisposeDesc(&tmp);
		}
		
		AEDisposeDesc(&result);
	}
	
	return errorNumber;
}

/**
 * Returns the Error Message from the last execution
 */
JNIEXPORT jstring JNICALL OSA_SCRIPT(GetErrorMessage)
	(JNIEnv *env, jclass clazz, jint ptr)
{
	OSStatus err = noErr;
	
	jstring msg = 0;
	
	if (ptr > 0) {
		
		SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
		
		AEDesc result = { typeNull, 0 };
		AECreateDesc(typeNull, 0, 0, &result);
		
        err = OSAScriptError(data->theComponent, 
							 kOSAErrorMessage,
							 typeChar, 
							 &result);
							 
		if (err == noErr && result.descriptorType != typeNull) {
            
            Size actualSize = AEGetDescDataSize(&result);
            
            // allocate an extra byte for the closing null character
            Size length = actualSize * sizeof(char) + 1;
            void *mem = (actualSize != 0) ? malloc(length) : 0;
            
            if (mem != 0) {
                
                memset(mem, 0, length);
				err = AEGetDescData(&result, mem, actualSize);
				
                if (err == noErr) {
                    msg = (*env)->NewStringUTF(env, (char*)mem);
                }
                
                free(mem);
            }
        }
		
		AEDisposeDesc(&result);
	}
	
	return msg;
}

/**
 * Returns the Type of this AEDesc
 */
JNIEXPORT jstring JNICALL OSA_SCRIPT(GetResultType)
	(JNIEnv *env, jclass clazz, jint ptr)
{
	OSStatus err = noErr;
	
	jstring type = 0;
	
	if (ptr > 0) {
		
		SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
		
		AEDesc resultData = { typeNull, 0 };
		AECreateDesc(typeNull, 0, 0, &resultData);
		
		err = OSACoerceToDesc(data->theComponent, 
							data->resultID, 
							typeWildCard, 
							kOSAModeNull, 
							&resultData);
		
		if (err == noErr && resultData.descriptorType != typeNull) {
		
			jchar tmp[4];
			tmp[0] = (char)(resultData.descriptorType >> 24);
			tmp[1] = (char)(resultData.descriptorType >> 16);
			tmp[2] = (char)(resultData.descriptorType >> 8);
			tmp[3] = (char)(resultData.descriptorType);
			
			type = (*env)->NewString(env, tmp, 4);
		}
		
		AEDisposeDesc(&resultData);
	}
	
	return type;
}

/**
 * Returns the size of this AEDesc
 */
JNIEXPORT jsize JNICALL OSA_SCRIPT(GetResultDataSize)
	(JNIEnv *env, jclass clazz, jint ptr)
{
	OSStatus err = noErr;
	
	jsize size = 0;
	
	if (ptr > 0) {
		
		SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
		
		AEDesc resultData = { typeNull, 0 };
		AECreateDesc(typeNull, 0, 0, &resultData);
		
		err = OSACoerceToDesc(data->theComponent, 
							data->resultID, 
							typeWildCard, 
							kOSAModeNull, 
							&resultData);
		
		if (err == noErr && resultData.descriptorType != typeNull) {
			size = (jsize)AEGetDescDataSize(&resultData);
		}
		
		AEDisposeDesc(&resultData);
	}
	
	return size;
}

/**
 * Copies the data of this AEDesc into result
 */
JNIEXPORT jint JNICALL OSA_SCRIPT(GetResultData)
	(JNIEnv *env, jclass clazz, jint ptr, jbyteArray result, jsize pos, jsize length)
{
	OSStatus err = noErr;

	if (ptr > 0) {
		
		SharedOSAScriptData *data = (SharedOSAScriptData*)ptr;
		
		if (data->resultID != kOSANullScript) {
	
			AEDesc resultData = { typeNull, 0 };
			AECreateDesc(typeNull, 0, 0, &resultData);
			
			err = OSACoerceToDesc(data->theComponent, 
							data->resultID, 
							typeWildCard, 
							kOSAModeNull, 
							&resultData);
			
			if (err == noErr) {
				err = CopyAEDescData(env, &resultData, result, pos, length);
			}
			
			AEDisposeDesc(&resultData);
		}
	}
	
	return err;
}