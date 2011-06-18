/*
 * Copyright (C) 2011 Iranian Supreme Council of ICT, The FarsiTel Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASICS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "FriBidi"

#include <utils/Log.h>

#include "jni.h"
#include "utils/misc.h"
#include "android_runtime/AndroidRuntime.h"
#include <nativehelper/JNIHelp.h>

#include <sys/types.h>
#include <dirent.h>
#include <time.h>
#include <openssl/dsa.h>
#include <openssl/engine.h>
#include <openssl/sha.h>
#include <fribidi.h>
#include <fribidi-char-sets.h>

#define PARAGRAPH_DIRECTION_LTR  1
#define PARAGRAPH_DIRECTION_RTL  2
#define PARAGRAPH_DIRECTION_ON   3
#define PARAGRAPH_DIRECTION_WLTR 4
#define PARAGRAPH_DIRECTION_WRTL 5

namespace android {

static jfieldID g_strField = 0;
static jfieldID g_dirField = 0;
static jfieldID g_btypesField = 0;
static jfieldID g_levelsField = 0;
static jfieldID g_ltovField = 0;

static jclass g_fribidiClass = 0;

static void android_text_FriBidi_analyze(JNIEnv* env, jobject This) {
    jstring string;
    jint direction;
    jintArray btypesArray;
    jbyteArray levelsArray;
    FriBidiChar *main_str;
    FriBidiStrIndex len;
    FriBidiParType pbase;
    FriBidiCharType *btypes;
    FriBidiLevel *embedding_levels;
    FriBidiJoiningType *jtypes;
    char *str;
    const char *const_str;

    string = (jstring)env->GetObjectField(This, g_strField);
    direction = env->GetIntField(This, g_dirField);
 
    const_str = env->GetStringUTFChars(string, NULL);
    len = strlen(const_str);
    main_str = (FriBidiChar *)malloc(sizeof(FriBidiChar) * len);
    len = fribidi_charset_to_unicode(FRIBIDI_CHAR_SET_UTF8, const_str, len, main_str);
    btypes = (FriBidiCharType *)malloc(sizeof(FriBidiCharType) * len);
    embedding_levels = (FriBidiLevel *)malloc(sizeof(FriBidiLevel) * len);
    jtypes = (FriBidiJoiningType *)malloc(sizeof(FriBidiJoiningType) * len);

    switch(direction) {
        case PARAGRAPH_DIRECTION_LTR:
        pbase = FRIBIDI_PAR_LTR;
        break;
        case PARAGRAPH_DIRECTION_RTL:
        pbase = FRIBIDI_PAR_RTL;
        break;
        case PARAGRAPH_DIRECTION_WLTR:
        pbase = FRIBIDI_PAR_WLTR;
        break;
        case PARAGRAPH_DIRECTION_WRTL:
        pbase = FRIBIDI_PAR_WRTL;
        break;
        case PARAGRAPH_DIRECTION_ON:
        default:
        pbase = FRIBIDI_PAR_ON;
    }

    fribidi_get_bidi_types(main_str, len, btypes);
    fribidi_get_par_embedding_levels(btypes, len, &pbase, embedding_levels);

    fribidi_get_joining_types(main_str, len, jtypes);
    fribidi_join_arabic(btypes, len, embedding_levels, jtypes);
    fribidi_shape(FRIBIDI_FLAGS_DEFAULT | FRIBIDI_FLAGS_ARABIC, embedding_levels, len, jtypes, main_str);

    switch(pbase) {
        case FRIBIDI_PAR_LTR:
        direction = PARAGRAPH_DIRECTION_LTR;
        break;
        case FRIBIDI_PAR_RTL:
        direction = PARAGRAPH_DIRECTION_RTL;
        break;
        case FRIBIDI_PAR_WLTR:
        direction = PARAGRAPH_DIRECTION_WLTR;
        break;
        case FRIBIDI_PAR_WRTL:
        direction = PARAGRAPH_DIRECTION_WRTL;
        break;
        case FRIBIDI_PAR_ON:
        default:
        direction = PARAGRAPH_DIRECTION_ON;
    }

    btypesArray = env->NewIntArray(len);
    levelsArray = env->NewByteArray(len);
    env->SetIntArrayRegion(btypesArray, 0, len, (jint *)btypes);
    env->SetByteArrayRegion(levelsArray, 0, len, (jbyte *)embedding_levels);
    str = (char *)malloc(4 * len);
    len = fribidi_unicode_to_charset(FRIBIDI_CHAR_SET_UTF8, main_str, len, str);

    env->SetObjectField(This, g_strField, env->NewStringUTF(str));
    env->SetIntField(This, g_dirField, direction);
    env->SetObjectField(This, g_btypesField, btypesArray);
    env->SetObjectField(This, g_levelsField, levelsArray);

    free(jtypes);
    free(main_str);
    free(btypes);
    free(embedding_levels);
}

static void android_text_FriBidi_reorderLine(JNIEnv* env, jobject This, jint offset, jint length) {
    jstring string;
    jint direction;
    jintArray btypesArray;
    jbyteArray levelsArray;
    jintArray ltovArray;
    FriBidiChar *main_str;
    FriBidiStrIndex len;
    int arrayLength;
    FriBidiParType pbase;
    FriBidiCharType *btypes;
    FriBidiLevel *embedding_levels;
    FriBidiStrIndex *map; // visual to logical mapping
    char *str;
    const char *const_str;

    string = (jstring)env->GetObjectField(This, g_strField);
    direction = env->GetIntField(This, g_dirField);
 
    const_str = env->GetStringUTFChars(string, NULL);
    len = strlen(const_str);
    main_str = (FriBidiChar *)malloc(sizeof(FriBidiChar) * len);
    len = fribidi_charset_to_unicode(FRIBIDI_CHAR_SET_UTF8, const_str, len, main_str);
    btypesArray = (jintArray)env->GetObjectField(This, g_btypesField);
    levelsArray = (jbyteArray)env->GetObjectField(This, g_levelsField);
    ltovArray = (jintArray)env->GetObjectField(This, g_ltovField);
    arrayLength = env->GetArrayLength(btypesArray);
    int maxLength = len;
    if(arrayLength > maxLength)
        maxLength = arrayLength;
    btypes = (FriBidiCharType *)malloc(sizeof(FriBidiCharType) * maxLength);
    embedding_levels = (FriBidiLevel *)malloc(sizeof(FriBidiLevel) * maxLength);
    map = (FriBidiStrIndex *)malloc(sizeof(FriBidiStrIndex) * maxLength);

    switch(direction) {
        case PARAGRAPH_DIRECTION_LTR:
        pbase = FRIBIDI_PAR_LTR;
        break;
        case PARAGRAPH_DIRECTION_RTL:
        pbase = FRIBIDI_PAR_RTL;
        break;
        case PARAGRAPH_DIRECTION_WLTR:
        pbase = FRIBIDI_PAR_WLTR;
        break;
        case PARAGRAPH_DIRECTION_WRTL:
        pbase = FRIBIDI_PAR_WRTL;
        break;
        case PARAGRAPH_DIRECTION_ON:
        default:
        pbase = FRIBIDI_PAR_ON;
    }

    env->GetIntArrayRegion(btypesArray, 0, len, (jint *)btypes);
    env->GetByteArrayRegion(levelsArray, 0, len, (jbyte *)embedding_levels);
    env->GetIntArrayRegion(ltovArray, 0, len, (jint *)map);

    fribidi_reorder_line(FRIBIDI_FLAG_SHAPE_MIRRORING | FRIBIDI_FLAG_REMOVE_SPECIALS, \
                         btypes, length, offset, pbase, embedding_levels, main_str, map);

    arrayLength = len;
    str = (char *)malloc(4 * len);
    len = fribidi_unicode_to_charset(FRIBIDI_CHAR_SET_UTF8, main_str, len, str);
    ltovArray = env->NewIntArray(arrayLength);
    env->SetIntArrayRegion(ltovArray, 0, arrayLength, (jint *)map);

    env->SetObjectField(This, g_ltovField, ltovArray);
    jstring temp = env->NewStringUTF(str);
    env->SetObjectField(This, g_strField, temp);
    env->SetIntField(This, g_dirField, direction);

    free(main_str);
    free(btypes);
    free(embedding_levels);
    free(map);
}

// JNI registration

static JNINativeMethod gMethods[] = {
    {"analyze", "()V", (void *)android_text_FriBidi_analyze},
    {"reorderLine", "(II)V", (void *)android_text_FriBidi_reorderLine}
};

int register_android_text_FriBidi(JNIEnv* env)
{
    jclass fribidiClass = env->FindClass("android/text/FriBidi");

    g_strField = env->GetFieldID(fribidiClass, "str", "Ljava/lang/String;");
    g_dirField = env->GetFieldID(fribidiClass, "direction", "I");
    g_btypesField = env->GetFieldID(fribidiClass, "btypes", "[I");
    g_levelsField = env->GetFieldID(fribidiClass, "embedding_levels", "[B");
    g_ltovField = env->GetFieldID(fribidiClass, "logical_to_visual", "[I");

    return AndroidRuntime::registerNativeMethods(env, "android/text/FriBidi", gMethods, NELEM(gMethods));
}

} //namespace android
