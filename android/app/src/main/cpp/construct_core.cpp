#include <jni.h>
#include <android/log.h>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <cstdint>
#include <cstring>

namespace {
constexpr const char* CAP_HEADER = "Construct1.7";
std::vector<uint8_t> read_file(const char* path) {
    std::ifstream input(path, std::ios::binary);
    if (!input) return {};
    input.seekg(0, std::ios::end);
    std::streamoff size = input.tellg();
    input.seekg(0, std::ios::beg);
    std::vector<uint8_t> data(static_cast<size_t>(size));
    input.read(reinterpret_cast<char*>(data.data()), size);
    return data;
}

std::string json_escape(const std::string& value) {
    std::string out;
    for (unsigned char c : value) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\b': out += "\\b"; break;
            case '\f': out += "\\f"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (c < 0x20) {
                    char buf[7];
                    std::snprintf(buf, sizeof(buf), "\\u%04x", c);
                    out += buf;
                } else {
                    out += static_cast<char>(c);
                }
        }
    }
    return out;
}

std::string find_ascii_field(const std::vector<uint8_t>& bytes) {
    std::string best;
    for (size_t i = 13; i + 2 < bytes.size(); ++i) {
        uint16_t len = bytes[i] | (static_cast<uint16_t>(bytes[i + 1]) << 8);
        if (len > 0 && len < 512 && i + 2 + len <= bytes.size()) {
            bool printable = true;
            std::string candidate;
            candidate.reserve(len);
            for (size_t j = i + 2; j < i + 2 + len; ++j) {
                unsigned char c = bytes[j];
                if (c < 32 || c > 126) { printable = false; break; }
                candidate.push_back(static_cast<char>(c));
            }
            if (printable && candidate.size() > best.size()) best = candidate;
        }
        if (i + 4 < bytes.size()) {
            uint32_t len32 = bytes[i] | (uint32_t(bytes[i + 1]) << 8)
                           | (uint32_t(bytes[i + 2]) << 16) | (uint32_t(bytes[i + 3]) << 24);
            if (len32 > 0 && len32 < 512 && i + 4 + len32 <= bytes.size()) {
                std::string candidate(reinterpret_cast<const char*>(bytes.data() + i + 4), len32);
                if (candidate.find_first_not_of("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ._-()") == std::string::npos
                    && candidate.size() > best.size()) best = candidate;
            }
        }
    }
    return best;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_construct_editor_CoreNative_inspectCap(JNIEnv* env, jclass, jstring path) {
    const char* cpath = env->GetStringUTFChars(path, nullptr);
    std::vector<uint8_t> bytes = read_file(cpath);
    env->ReleaseStringUTFChars(path, cpath);
    bool valid = bytes.size() >= 13 &&
                 std::memcmp(bytes.data(), CAP_HEADER, 13) == 0;
    std::string name = valid ? find_ascii_field(bytes) : "";
    std::ostringstream json;
    json << "{";
    json << "\"valid\":" << (valid ? "true" : "false") << ",";
    json << "\"size\":" << bytes.size() << ",";
    json << "\"header\":\"" << (valid ? "Construct1.7" : "unknown") << "\",";
    json << "\"name\":\"" << json_escape(name) << "\",";
    json << "\"preservedRawBytes\":" << (valid ? "true" : "false");
    json << "}";
    return env->NewStringUTF(json.str().c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_construct_editor_CoreNative_openCap(JNIEnv* env, jclass, jstring path, jstring targetPath) {
    const char* cpath = env->GetStringUTFChars(path, nullptr);
    const char* ctarget = env->GetStringUTFChars(targetPath, nullptr);
    std::vector<uint8_t> bytes = read_file(cpath);
    bool valid = bytes.size() >= 13 && std::memcmp(bytes.data(), CAP_HEADER, 13) == 0;
    if (valid) {
        std::ofstream raw(ctarget, std::ios::binary);
        raw.write(reinterpret_cast<const char*>(bytes.data()), bytes.size());
    }
    env->ReleaseStringUTFChars(path, cpath);
    env->ReleaseStringUTFChars(targetPath, ctarget);
    return valid;
}
