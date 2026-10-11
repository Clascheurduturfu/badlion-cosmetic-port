#include <jni.h>
#include <fstream>
#include <string>
#include <iterator>
extern "C" __declspec(dllexport) __declspec(noinline)
jstring JNICALL Java_net_badlion_client_Wrapper_getAvailableCosmetics(JNIEnv* env, jobject) {
    std::ifstream input("mock-owned-response.json");
    if (input) {
        const std::string response{std::istreambuf_iterator<char>(input), std::istreambuf_iterator<char>()};
        return env->NewStringUTF(response.c_str());
    }
    return env->NewStringUTF("{\"cosmetics\":[]}");
}
