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
