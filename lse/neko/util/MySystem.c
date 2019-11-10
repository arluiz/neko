#include <sys/time.h>
#include "jni.h"
#include "lse_neko_util_MySystem.h"

/*
 * Class:     lse_neko_util_MySystem
 * Method:    currentTimeMicros
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_lse_neko_util_MySystem_currentTimeMicros
  (JNIEnv * ignored1, jclass ignored2)
{
    struct timeval t;
    gettimeofday(&t, 0);
    return ((jlong)t.tv_sec) * 1000000 + (jlong)t.tv_usec;
}

