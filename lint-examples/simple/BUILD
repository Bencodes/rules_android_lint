load("@rules_java//java:defs.bzl", "java_library")
load("@rules_android_lint//lint:rules.bzl", "lint_test")

java_library(
    name = "lib",
    srcs = ["Foo.java"],
    deps = [":lint_tests_dep"],
)

java_library(
    name = "lint_tests_dep",
    srcs = ["Bar.java"],
)

lint_test(
    name = "lib_lint",
    srcs = ["Foo.java"],
    config = "//:checks",
    self = ":lib",
    tags = ["lint"],
    deps = [":lib"],
)
