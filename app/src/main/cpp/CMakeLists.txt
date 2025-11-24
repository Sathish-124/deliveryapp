cmake_minimum_required(VERSION 3.22.1)

project("edge_detector")

# ------------------------------------------------------------------
# OpenCV Configuration
# ------------------------------------------------------------------
# ATTENTION: Set the path to your OpenCV Android SDK 'sdk/native/jni' folder here.
# If not set correctly, the app will build in "Fallback Mode" (no edge detection).
#
# Example: set(OpenCV_DIR "C:/Users/Admin/Downloads/opencv-android-sdk/sdk/native/jni")
if(NOT DEFINED OpenCV_DIR)
    set(OpenCV_DIR "C:/opencv/sdk/native/jni")
endif()

message(STATUS "Attempting to find OpenCV in: ${OpenCV_DIR}")

# Try to find OpenCV, but don't fail strictly if missing (allows project sync)
find_package(OpenCV QUIET)

if(OpenCV_FOUND)
    message(STATUS "OpenCV found! Libraries: ${OpenCV_LIBS}")
    add_definitions(-DUSE_OPENCV)
else()
    message(WARNING "OpenCV NOT FOUND at ${OpenCV_DIR}. Building in FALLBACK mode (Passthrough). Please download OpenCV Android SDK and update OpenCV_DIR in CMakeLists.txt.")
endif()

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

if(OpenCV_FOUND)
    target_link_libraries(${CMAKE_PROJECT_NAME}
            android
            log
            ${OpenCV_LIBS})
else()
    target_link_libraries(${CMAKE_PROJECT_NAME}
            android
            log)
endif()
