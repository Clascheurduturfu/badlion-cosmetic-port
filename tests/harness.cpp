#define NOMINMAX
#include <windows.h>
#include <jni.h>
#include "catalog.hpp"
#include <iostream>
#include <fstream>
#include <string>
std::string captured;
jstring JNICALL capture(JNIEnv*, const char* value) {
    captured = value;
    return reinterpret_cast<jstring>(1);
}
const char* JNICALL read_string(JNIEnv*, jstring, jboolean*) { return captured.c_str(); }
void JNICALL release_string(JNIEnv*, jstring, const char*) {}
jboolean JNICALL no_exception(JNIEnv*) { return JNI_FALSE; }
void JNICALL delete_ref(JNIEnv*, jobject) {}
int main(int argc, char** argv) {
    if (argc < 3) return 10;
    JNINativeInterface_ table{};
    table.NewStringUTF = capture;
    table.GetStringUTFChars = read_string;
    table.ReleaseStringUTFChars = release_string;
    table.ExceptionCheck = no_exception;
    table.DeleteLocalRef = delete_ref;
    JNIEnv env{&table};
    auto mock = LoadLibraryW(L"badlion_electron.dll");
    if (!mock) return 11;
    auto target = reinterpret_cast<jstring (JNICALL*)(JNIEnv*, jobject)>(
        GetProcAddress(mock, "Java_net_badlion_client_Wrapper_getAvailableCosmetics"));
    target(&env, nullptr);
    const auto baseline = captured;
    auto proxy = LoadLibraryA(argv[1]);
    if (!proxy) { std::cerr << "load failed " << GetLastError(); return 13; }
    auto get_status = reinterpret_cast<int(*)()>(GetProcAddress(proxy, "UnlockerGetStatus"));
    if (!get_status) return 19;
    for (int i = 0; i < 200 && get_status() == 0; ++i) Sleep(25);
    const std::string mode = argv[2];
    const int expected = mode == "valid" || mode == "diagnostic" || mode == "owned-export" ? 1 : mode == "guard" ? -4 : mode == "skip" ? 2 : -1;
    if (get_status() != expected) { std::cerr << "unexpected status " << get_status(); return 14; }
    target(&env, nullptr);
    if (mode == "owned-export") {
        std::ifstream input("owned-cosmetics.json");
        const auto exported = nlohmann::json::parse(input);
        const auto stock = nlohmann::json::parse(baseline);
        if (exported.at("cosmetics") != stock.at("cosmetics") || exported.contains("privateRootField")) return 23;
        const auto merged = nlohmann::json::parse(captured);
        if (merged.at("cosmetics").at(0) != stock.at("cosmetics").at(0)
                || merged.at("cosmetics").size() != 3397) return 24;
        std::cout << "PASS original owned snapshot exported before merge; root account data excluded\n";
    } else if (mode == "valid") {
        std::ifstream stream("cosmetics.json");
        const auto stock = nlohmann::json::parse(stream);
        size_t total = 0;
        for (const auto& entries : stock["registeredCosmetics"]) total += entries.size();
        const auto result = nlohmann::json::parse(captured);
        if (result.at("cosmetics").size() != total) return 15;
        for (const auto& item : result.at("cosmetics")) {
            if (!item.at("cosmeticId").is_number_integer() || !item.at("cosmeticType").is_string()
                    || !item.at("active").is_boolean() || item.at("active").get<bool>()) return 16;
            if (!item.at("cosmeticData").is_object() || !item.at("sourceData").is_object()
                    || !item.at("globalData").is_object()) return 21;
        }
        std::cout << "PASS native detour, JNI string, " << total << " cosmetic records\n";
        std::ofstream("last-payload.json") << captured;
        auto known = result.at("cosmetics").at(0);
        known["active"] = true;
        known["cosmeticData"] = {{"customSetting", 7}};
        known["globalData"] = {{"color", 123}, {"location", 2}};
        const nlohmann::json fixture{{"cosmetics", nlohmann::json::array({known})}, {"extraField", "preserved"}};
        const auto merged = nlohmann::json::parse(merge_payload(fixture, result));
        if (merged.at("cosmetics").size() != total || merged.at("cosmetics").at(0) != known
                || merged.at("extraField") != "preserved") return 22;
        std::cout << "PASS owned record, active state, settings and unknown root fields preserved\n";
    } else {
        if (captured != baseline) return 17;
        if (mode == "diagnostic") {
            std::ifstream stream("badlion-cosmetic-schema.json");
            const auto schema = nlohmann::json::parse(stream);
            if (schema["fields"]["cosmetics"]["type"] != "array") return 20;
        }
        std::cout << "PASS " << mode << ": original function preserved\n";
    }
    // Verify a real Windows version API reaches the system DLL through the forwarder.
    auto size_fn = reinterpret_cast<DWORD(WINAPI*)(LPCWSTR, LPDWORD)>(
        GetProcAddress(proxy, "GetFileVersionInfoSizeW"));
    DWORD unused = 0;
    if (!size_fn || size_fn(L"C:\\Windows\\System32\\kernel32.dll", &unused) == 0) return 18;
    std::cout << "PASS system VERSION API forwarding\n";
    return 0;
}
