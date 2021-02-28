load("@rules_android_lint//lint:rules.bzl", "lint_test")
load("@io_bazel_rules_kotlin//kotlin:android.bzl", "kt_android_library")

kt_android_library(
    name = "lib",
    srcs = ["src/bazel/android/MainActivity.kt"],
    manifest = "MyAndroidManifest.xml",
    resource_files = glob(["res/**"]),
    deps = ["@maven//:androidx_activity_activity"],
)

lint_test(
    name = "lib_lint",
    srcs = ["src/bazel/android/MainActivity.kt"],
    manifest = "MyAndroidManifest.xml",
    resource_files = glob(["res/**"]),
    self = ":lib",
    tags = ["lint"],
    deps = [
        ":lib",
        "@maven//:androidx_activity_activity",
    ],
)
