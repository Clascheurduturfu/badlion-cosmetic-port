#include <windows.h>

extern "C" __declspec(dllexport) void __stdcall Callback_ClassFileLoadHook(
    void*, void*, void*, void*, const char*, void*, int inputLength,
    const unsigned char* input, int* outputLength, unsigned char** output) {
    *outputLength = inputLength;
    *output = const_cast<unsigned char*>(input);
}
