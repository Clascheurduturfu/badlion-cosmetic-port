#pragma once
#include "json.hpp"
#include <set>
#include <stdexcept>
#include <string>

inline std::string build_payload(const nlohmann::json& catalog) {
    const auto& registered = catalog.at("registeredCosmetics");
    if (!registered.is_object()) throw std::runtime_error("registeredCosmetics must be an object");
    auto cosmetics = nlohmann::json::array();
    std::set<std::pair<std::string, long long>> seen;
    for (const auto& [type, items] : registered.items()) {
        if (type.empty() || !items.is_array()) throw std::runtime_error("invalid category");
        for (const auto& item : items) {
            const auto& value = item.at("cosmeticId");
            if (!value.is_number_integer()) throw std::runtime_error("cosmeticId must be an integer");
            const auto id = value.get<long long>();
            if (id < 0 || id > 2147483647LL) throw std::runtime_error("cosmeticId out of range");
            if (seen.emplace(type, id).second)
                cosmetics.push_back({{"cosmeticId", id}, {"cosmeticType", type}, {"active", false},
                    {"cosmeticData", nlohmann::json::object()}, {"sourceData", nlohmann::json::object()},
                    {"globalData", nlohmann::json::object()}});
        }
    }
    if (cosmetics.empty()) throw std::runtime_error("empty catalog");
    return nlohmann::json{{"cosmetics", std::move(cosmetics)}}.dump(-1, ' ', true);
}

inline std::string merge_payload(const nlohmann::json& stock, const nlohmann::json& additions) {
    auto response = stock;
    auto& owned = response.at("cosmetics");
    if (!owned.is_array()) throw std::runtime_error("stock cosmetics must be an array");
    std::set<std::pair<std::string, long long>> seen;
    for (const auto& entry : owned)
        seen.emplace(entry.at("cosmeticType").get<std::string>(), entry.at("cosmeticId").get<long long>());
    for (const auto& entry : additions.at("cosmetics")) {
        if (seen.emplace(entry.at("cosmeticType").get<std::string>(), entry.at("cosmeticId").get<long long>()).second)
            owned.push_back(entry);
    }
    return response.dump(-1, ' ', true);
}
