#include <jni.h>
#include <string>
#include <cstring>

// Conditional compilation based on CMake definition
#ifdef USE_OPENCV
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#endif

extern "C" JNIEXPORT void JNICALL
Java_com_example_edge_1detector_MainActivity_processImage(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray input,
        jint width,
        jint height,
        jint stride,
        jbyteArray output) {

    jbyte* srcData = env->GetByteArrayElements(input, nullptr);
    jbyte* dstData = env->GetByteArrayElements(output, nullptr);

#ifdef USE_OPENCV
    // --------------------------------------------------------
    // OpenCV Implementation
    // --------------------------------------------------------
    cv::Mat src(height, width, CV_8UC1, (unsigned char*)srcData, stride);
    cv::Mat dst(height, width, CV_8UC1, (unsigned char*)dstData);

    // Apply Canny Edge Detection
    cv::Canny(src, dst, 50, 150);

#else
    // --------------------------------------------------------
    // Fallback: Pass-through (Copy input to output)
    // --------------------------------------------------------
    // If stride matches width, we can memcpy the whole block
    if (stride == width) {
        std::memcpy(dstData, srcData, width * height);
    } else {
        // Copy row by row to remove stride padding
        for (int i = 0; i < height; ++i) {
            std::memcpy(dstData + (i * width), srcData + (i * stride), width);
        }
    }
#endif

    env->ReleaseByteArrayElements(input, srcData, JNI_ABORT);
    env->ReleaseByteArrayElements(output, dstData, 0);
}
