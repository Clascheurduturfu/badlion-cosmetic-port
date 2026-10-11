#include <windows.h>
#include <filesystem>
#include <fstream>
#include <iterator>
#include <vector>

int wmain(int argc, wchar_t** argv) {
    if (argc != 2) return 2;
    std::ifstream stream(std::filesystem::path(argv[1]), std::ios::binary);
    std::vector<unsigned char> bytes((std::istreambuf_iterator<char>(stream)), {});
    if (bytes.empty()) return 3;
    auto module = LoadLibraryW(L"badlion_js.dll");
    if (!module) return 4;
    using Hook = void (__stdcall*)(void*, void*, void*, void*, const char*, void*,
                                  int, const unsigned char*, int*, unsigned char**);
    auto hook = reinterpret_cast<Hook>(GetProcAddress(module, "Callback_ClassFileLoadHook"));
    if (!hook) return 5;
    int outputLength = 0;
    unsigned char* output = nullptr;
    hook(nullptr, nullptr, nullptr, nullptr, "net/minecraft/client/main/Main", nullptr,
         static_cast<int>(bytes.size()), bytes.data(), &outputLength, &output);
    return outputLength == bytes.size() && output == bytes.data() ? 0 : 6;
}
