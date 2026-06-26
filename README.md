# rules_android_lint

## Overview

This repository contains Bazel rules for running Android Lint.

## Features

- Running Android Lint on Android, Kotlin, and JVM targets
- Providing global and per-target configuration files
- Enabling/Disabling specific checks at the target level
- Custom Lint rules
- Automatic loading of custom rule jars from AARs
- Build and test actions
- Bzlmod support
- Persistent worker support

## Getting Started

### Requirements

The bundled Android Lint (32.x) ships Java 17 bytecode, so the lint worker must run on a
Java 17+ runtime. Add the following to your `.bazelrc` if you don't already target 17+:

```
common --java_runtime_version=remotejdk_17
common --tool_java_runtime_version=remotejdk_17
```

The default lint toolchain uses `hermetic_android_toolchains` to provide the Android SDK
through Bazel. Accept the pinned SDK license in your `.bazelrc`:

```
common --repo_env=ACCEPTED_ANDROID_SDK_LICENSE_VERSION=34
```

### Using a different Android Lint version

The lint binary is supplied by the toolchain. To pin your own version, build a deploy jar
from `com.android.tools.lint:lint` in your own `maven.install(...)` and point the toolchain
at it:

```python
android_lint_toolchain(
    name = "my_android_lint",
    android_lint = ":my_lint_deploy.jar",
)
```

### MODULE.bazel

To use these rules, add the following to your `MODULE.bazel`:

```python
archive_override(
    module_name = "rules_android_lint",
    integrity = "INTEGRITY",
    strip_prefix = "rules_android_lint-COMMIT_SHA",
    urls = [
        "https://github.com/bencodes/rules_android_lint/archive/COMMIT_SHA.tar.gz",
    ],
)

bazel_dep(name = "rules_android_lint")
```

### WORKSPACE

To use these rules, add the following to your `WORKSPACE`:

```python
# Not fully supported yet
```

## Usage

```python
load("@rules_android//android:rules.bzl", "android_library")
load("@rules_android_lint//rules:defs.bzl", "android_lint_test")

android_library(
    name = "lib",
    srcs = glob(["**/*.java"]),
    custom_package = "com.rules.android.lint.example.app",
)

android_lint_test(
    name = "lib_lint_test",
    srcs = ["Foo.java"],
    baseline = "lib_lint_test_lint_baseline.xml",
    lib = ":lib",
)
```
