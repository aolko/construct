cmake_minimum_required(VERSION 3.22.1)
project(construct_core LANGUAGES CXX)

add_library(construct_core SHARED construct_core.cpp)
find_library(log-lib log)
target_link_libraries(construct_core ${log-lib})
