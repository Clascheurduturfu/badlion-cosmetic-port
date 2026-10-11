#include <windows.h>
#include <filesystem>
#include <fstream>
#include <string>
#include <vector>

int wmain() {
    wchar_t executable[32768];
    GetModuleFileNameW(nullptr, executable, 32768);
    auto directory = std::filesystem::path(executable).parent_path();
    std::wstring command = L"\"" + (directory / L"badlion-java-capture.exe").wstring() +
        L"\" \"" + (directory / L"Main.class").wstring() + L"\"";
    std::vector<wchar_t> mutableCommand(command.begin(), command.end());
    mutableCommand.push_back(0);
    STARTUPINFOW startup{sizeof(startup)};
    PROCESS_INFORMATION process{};
    if (!CreateProcessW(nullptr, mutableCommand.data(), nullptr, nullptr, FALSE, 0,
                        nullptr, directory.c_str(), &startup, &process)) return 2;
    DWORD outcome = WaitForSingleObject(process.hProcess, 30000);
    DWORD code = 100;
    if (outcome == WAIT_OBJECT_0) GetExitCodeProcess(process.hProcess, &code);
    CloseHandle(process.hProcess);
    CloseHandle(process.hThread);
    std::ofstream(directory / L"fixture-result.txt") << code;
    return static_cast<int>(code);
}
