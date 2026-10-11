#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <jni.h>
#include <MinHook.h>
#include "catalog.hpp"
#include <atomic>
#include <filesystem>
#include <fstream>
#include <cstring>

namespace {
HMODULE self;
std::atomic<int> status{0}; // 0 starting, 1 ready, negative failure, 2 skipped
std::filesystem::path directory;
std::string payload;
nlohmann::json additions;
bool diagnostic_only = false;
std::atomic_flag schema_recorded = ATOMIC_FLAG_INIT;
std::atomic_flag ownership_recorded = ATOMIC_FLAG_INIT;
std::filesystem::path ownership_export;
using Native = jstring (JNICALL*)(JNIEnv*, jobject);
Native original = nullptr;
constexpr char target_name[] = "Java_net_badlion_client_Wrapper_getAvailableCosmetics";

void log(const std::string& message) {
    try { std::ofstream(directory / L"badlion-cosmetic-port.log", std::ios::app)
          << "[0.1.4] " << message << '\n'; } catch (...) {}
}
nlohmann::json describe_schema(const nlohmann::json& value, int depth = 0) {
    nlohmann::json schema{{"type", value.type_name()}};
    if (depth >= 5) return schema;
    if (value.is_object()) {
        schema["fields"] = nlohmann::json::object();
        for (const auto& [key, child] : value.items())
            schema["fields"][key] = describe_schema(child, depth + 1);
    } else if (value.is_array()) {
        schema["count"] = value.size();
        schema["samples"] = nlohmann::json::array();
        for (size_t i = 0; i < value.size() && i < 3; ++i)
            schema["samples"].push_back(describe_schema(value[i], depth + 1));
    }
    return schema;
}
jstring JNICALL replacement(JNIEnv* env, jobject object) {
    if (diagnostic_only) {
        auto result = original(env, object);
        if (!result || env->ExceptionCheck() || schema_recorded.test_and_set()) return result;
        const char* text = env->GetStringUTFChars(result, nullptr);
        if (!text) return result;
        try {
            // Store names and types only, never scalar values or the full native response.
            auto schema = describe_schema(nlohmann::json::parse(text));
            std::ofstream(directory / L"badlion-cosmetic-schema.json") << schema.dump(2);
            log("stock response returned unchanged; field names/types recorded");
        } catch (...) { log("stock response returned unchanged; schema inspection failed"); }
        env->ReleaseStringUTFChars(result, text);
        return result;
    }
    // Preserve owned records, active selections and all other native response fields.
    auto stock = original(env, object);
    if (!stock || env->ExceptionCheck()) return stock;
    const char* text = env->GetStringUTFChars(stock, nullptr);
    if (!text) return stock;
    std::string merged;
    try {
        const auto stock_json = nlohmann::json::parse(text);
        if (!ownership_export.empty() && stock_json.contains("cosmetics")
                && stock_json["cosmetics"].is_array() && !stock_json["cosmetics"].empty()
                && !ownership_recorded.test_and_set()) {
            try {
                auto records = nlohmann::json::array();
                for (const auto& entry : stock_json["cosmetics"]) {
                    nlohmann::json record;
                    for (const char* key : {"cosmeticId", "cosmeticType", "active", "cosmeticData", "sourceData", "globalData"})
                        if (entry.contains(key)) record[key] = entry[key];
                    records.push_back(std::move(record));
                }
                std::ofstream output(ownership_export);
                if (!output) throw std::runtime_error("ownership export file unavailable");
                output << nlohmann::json{{"cosmetics", std::move(records)},
                    {"source", "original native response before catalog merge"}}.dump(2);
                if (!output) throw std::runtime_error("ownership export write failed");
                log("original owned cosmetics exported; no account credentials included");
            } catch (...) { ownership_recorded.clear(); log("owned export failed; cosmetic response unaffected"); }
        }
        merged = merge_payload(stock_json, additions);
    }
    catch (...) {
        env->ReleaseStringUTFChars(stock, text);
        log("merge failed; stock response returned unchanged");
        return stock;
    }
    env->ReleaseStringUTFChars(stock, text);
    auto result = env->NewStringUTF(merged.c_str());
    if (result) env->DeleteLocalRef(stock);
    return result;
}
DWORD WINAPI worker(void*) {
    try {
        wchar_t filename[32768]{};
        const auto size = GetModuleFileNameW(self, filename, 32768);
        if (!size || size == 32768) { status = -1; return 0; }
        directory = std::filesystem::path(filename).parent_path();
        if (wcsstr(GetCommandLineW(), L" -version")) { status = 2; return 0; }
        diagnostic_only = std::filesystem::exists(directory / L"diagnostic-only");
        const auto export_config = directory / L"export-owned-to.txt";
        if (std::filesystem::exists(export_config)) {
            std::ifstream config(export_config);
            std::string path;
            std::getline(config, path);
            if (!path.empty()) {
                ownership_export = std::filesystem::u8path(path);
                if (ownership_export.filename() != L"owned-cosmetics.json" ||
                        !std::filesystem::is_directory(ownership_export.parent_path()))
                    throw std::runtime_error("invalid owned export destination");
            }
        }
        log(diagnostic_only ? "diagnostic mode: original response preserved" : "replacement mode");
        log("starting; catalog must be beside this DLL");
        const auto path = directory / L"cosmetics.json";
        if (std::filesystem::file_size(path) > 16 * 1024 * 1024)
            throw std::runtime_error("catalog exceeds 16 MiB");
        std::ifstream input(path);
        payload = build_payload(nlohmann::json::parse(input));
        additions = nlohmann::json::parse(payload);
        HMODULE module = nullptr;
        for (int attempt = 0; attempt < 2400 && !module; ++attempt) {
            module = GetModuleHandleW(L"badlion_electron.dll");
            if (!module) Sleep(50);
        }
        if (!module) { log("target module absent after 120 seconds"); status = -2; return 0; }
        auto target = reinterpret_cast<void*>(GetProcAddress(module, target_name));
        if (!target) { log("native cosmetic export missing"); status = -3; return 0; }
#ifndef BCP_TEST_BUILD
        // Exact prologue observed in the user's integrated Badlion 4.5.13.
        constexpr unsigned char expected[] = {0x48,0x89,0x5c,0x24,0x10,0x55,0x56,0x57,
            0x48,0x8d,0x6c,0x24,0x90,0x48,0x81,0xec,0x70,0x01,0x00,0x00,0x48,0x8b,0x05,0x65};
        if (memcmp(target, expected, sizeof(expected))) {
            log("unsupported native prologue; leaving function unchanged"); status = -4; return 0;
        }
#endif
        // Keep both modules alive for the lifetime of the hook.
        HMODULE pinned;
        if (!GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_PIN,
                reinterpret_cast<LPCWSTR>(target), &pinned) ||
            !GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_PIN,
                reinterpret_cast<LPCWSTR>(&replacement), &pinned))
            throw std::runtime_error("module pin failed");
        auto result = MH_Initialize();
        if (result != MH_OK) throw std::runtime_error(MH_StatusToString(result));
        result = MH_CreateHook(target, reinterpret_cast<void*>(&replacement), reinterpret_cast<void**>(&original));
        if (result != MH_OK) throw std::runtime_error(MH_StatusToString(result));
        result = MH_EnableHook(target);
        if (result != MH_OK) { MH_RemoveHook(target); throw std::runtime_error(MH_StatusToString(result)); }
        log("hook enabled; " + std::to_string(nlohmann::json::parse(payload)["cosmetics"].size()) + " cosmetics");
        status = 1;
    } catch (const std::exception& error) { log(std::string("startup failed: ") + error.what()); status = -1; }
    catch (...) { log("startup failed: unknown exception"); status = -1; }
    return 0;
}
}
extern "C" __declspec(dllexport) int UnlockerGetStatus() { return status.load(); }
BOOL WINAPI DllMain(HMODULE module, DWORD reason, LPVOID) {
    if (reason == DLL_PROCESS_ATTACH) {
        self = module;
        auto thread = CreateThread(nullptr, 0, worker, nullptr, 0, nullptr);
        if (thread) CloseHandle(thread); else status = -1;
    }
    return TRUE;
}
