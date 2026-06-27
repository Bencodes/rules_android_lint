#!/bin/bash
#
# Helpers for scaffolding a minimal consumer workspace of rules_android_lint inside a
# script_test. The scaffolded copy of the ruleset keeps the Starlark sources verbatim but
# replaces the Kotlin CLI and the maven-derived lint jar with prebuilt deploy jars from the
# outer build, so the child Bazel never compiles Kotlin or resolves maven.

# Writes a scaffolded rules_android_lint module and a consumer workspace in the CWD.
function set_up_lint_workspace() {
  local dest="${TEST_TMPDIR}/rules_android_lint"
  rm -rf "${dest}"
  mkdir -p "${dest}/rules" "${dest}/toolchains" "${dest}/src/cli" "${dest}/third_party"

  # Starlark sources, copied verbatim.
  local rules_dir
  rules_dir="$(dirname "$(rlocation rules_android_lint/rules/defs.bzl)")"
  cp -L "${rules_dir}"/*.bzl "${dest}/rules/"
  touch "${dest}/rules/BUILD"

  cp -L "$(rlocation rules_android_lint/toolchains/toolchain.bzl)" "${dest}/toolchains/"

  # Prebuilt binaries replace the Kotlin + maven subtrees. The jar is renamed because
  # cli_deploy.jar collides with the java_binary's implicit <name>_deploy.jar output.
  cp -L "$(rlocation rules_android_lint/src/cli/cli_deploy.jar)" "${dest}/src/cli/cli_prebuilt.jar"
  cp -L "$(rlocation rules_android_lint/third_party/com_android_tools_lint_lint_deploy.jar)" \
    "${dest}/third_party/"
  cp -L "$(rlocation rules_android_lint/src/android_lint_output_validator.py)" "${dest}/src/"

  cat > "${dest}/MODULE.bazel" <<EOF
module(name = "rules_android_lint")

bazel_dep(name = "hermetic_android_toolchains", version = "0.2.0")
bazel_dep(name = "rules_android", version = "0.7.3")
bazel_dep(name = "rules_java", version = "9.3.0")
bazel_dep(name = "rules_python", version = "1.7.0")
bazel_dep(name = "bazel_skylib", version = "1.9.0")
bazel_dep(name = "platforms", version = "1.0.0")

android = use_extension(
    "@hermetic_android_toolchains//:extensions.bzl",
    "android",
    dev_dependency = True,
)
android.sdk(
    build_tools_version = "35.0.0",
    version = "34",
)
use_repo(android, "androidsdk")

rules_android_sdk = use_extension(
    "@rules_android//rules/android_sdk_repository:rule.bzl",
    "android_sdk_repository_extension",
)
override_repo(rules_android_sdk, "androidsdk")

register_toolchains("@androidsdk//:all", dev_dependency = True)
register_toolchains("//toolchains:default_toolchain", dev_dependency = True)
EOF

  cat > "${dest}/src/BUILD.bazel" <<EOF
load("@rules_python//python:defs.bzl", "py_binary")

py_binary(
    name = "android_lint_output_validator",
    srcs = ["android_lint_output_validator.py"],
    visibility = ["//visibility:public"],
)
EOF

  cat > "${dest}/src/cli/BUILD" <<EOF
load("@rules_java//java:defs.bzl", "java_binary", "java_import")

java_import(
    name = "cli_jar",
    jars = ["cli_prebuilt.jar"],
)

java_binary(
    name = "cli",
    main_class = "com.rules.android.lint.cli.AndroidLintAction",
    visibility = ["//visibility:public"],
    runtime_deps = [":cli_jar"],
)
EOF

  cat > "${dest}/third_party/BUILD.bazel" <<EOF
exports_files(["com_android_tools_lint_lint_deploy.jar"])
EOF

  cat > "${dest}/toolchains/BUILD.bazel" <<EOF
load(":toolchain.bzl", "android_lint_toolchain")

toolchain_type(
    name = "toolchain_type",
    visibility = ["//visibility:public"],
)

android_lint_toolchain(
    name = "default_toolchain_impl",
    android_home = "@androidsdk//:files",
    android_lint = "//third_party:com_android_tools_lint_lint_deploy.jar",
)

toolchain(
    name = "default_toolchain",
    toolchain = ":default_toolchain_impl",
    toolchain_type = ":toolchain_type",
)
EOF

  # The consumer workspace, in the CWD.
  cat > MODULE.bazel <<EOF
bazel_dep(name = "rules_android_lint")
bazel_dep(name = "hermetic_android_toolchains", version = "0.2.0")
bazel_dep(name = "rules_android", version = "0.7.3")

local_path_override(
    module_name = "rules_android_lint",
    path = "${dest}",
)

android = use_extension(
    "@hermetic_android_toolchains//:extensions.bzl",
    "android",
    dev_dependency = True,
)
android.sdk(
    build_tools_version = "35.0.0",
    version = "34",
)
use_repo(android, "androidsdk")

rules_android_sdk = use_extension(
    "@rules_android//rules/android_sdk_repository:rule.bzl",
    "android_sdk_repository_extension",
    dev_dependency = True,
)
override_repo(rules_android_sdk, "androidsdk")

register_toolchains("@androidsdk//:all", dev_dependency = True)
register_toolchains("//toolchains:default_toolchain", dev_dependency = True)
EOF

  mkdir -p toolchains
  cat > toolchains/BUILD.bazel <<EOF
load("@rules_android_lint//toolchains:toolchain.bzl", "android_lint_toolchain")

android_lint_toolchain(
    name = "default_toolchain_impl",
    android_home = "@androidsdk//:files",
)

toolchain(
    name = "default_toolchain",
    toolchain = ":default_toolchain_impl",
    toolchain_type = "@rules_android_lint//toolchains:toolchain_type",
)
EOF

  cat > .bazelrc <<EOF
common --lockfile_mode=off
# Lint 32.x ships Java 17 bytecode; the lint worker must run on a 17+ JVM.
common --java_language_version=17
common --java_runtime_version=remotejdk_17
common --tool_java_language_version=17
common --tool_java_runtime_version=remotejdk_17
# Required by hermetic_android_toolchains for the pinned SDK version.
common --repo_env=ACCEPTED_ANDROID_SDK_LICENSE_VERSION=34
# Shared across the test functions in one script_test run.
common --repository_cache=${TEST_TMPDIR}/repository_cache
EOF
}

# Writes a lintable target. $1: target name, $2: java source file name.
function write_lint_targets() {
  local name="$1"
  local src="$2"
  cat > BUILD.bazel <<EOF
load("@rules_android_lint//rules:defs.bzl", "android_lint", "android_lint_test")

# A filegroup keeps the scenario free of java toolchain setup; the lint action only
# needs the source files. (AndroidLibraryResourceClassJarProvider is optional on lib.)
filegroup(
    name = "lib",
    srcs = ["${src}"],
)

android_lint(
    name = "${name}_lint",
    srcs = ["${src}"],
    lib = ":lib",
    warnings_as_errors = True,
)

android_lint_test(
    name = "${name}_lint_test",
    srcs = ["${src}"],
    lib = ":lib",
    warnings_as_errors = True,
)
EOF
}

# Foo.java with no lint issues.
function write_clean_source() {
  cat > Foo.java <<EOF
package com.example;

public class Foo {
  public int add(int a, int b) {
    return a + b;
  }
}
EOF
}

# Foo.java that trips the DefaultLocale check (error under warnings_as_errors).
function write_dirty_source() {
  cat > Foo.java <<EOF
package com.example;

public class Foo {
  public String shout(String input) {
    return input.toUpperCase();
  }
}
EOF
}

# Copies the compose-lint-checks jar (the custom-rule test fixture) into the CWD.
function copy_compose_lint_checks() {
  cp -L "$(rlocation rules_android_lint/tests/scripts/compose_lint_checks.jar)" \
    compose_lint_checks.jar
}

# A source-level stand-in for androidx.compose.runtime.Composable so the compose-lints
# checks can resolve the annotation without the Compose runtime on the classpath.
function write_composable_annotation_stub() {
  cat > Composable.kt <<EOF
package androidx.compose.runtime

@Target(
  AnnotationTarget.FUNCTION,
  AnnotationTarget.TYPE,
  AnnotationTarget.TYPE_PARAMETER,
  AnnotationTarget.PROPERTY_GETTER,
)
annotation class Composable
EOF

  cat > Modifier.kt <<EOF
package androidx.compose.ui

interface Modifier {
  companion object : Modifier
}
EOF

  # A leaf composable for fixtures to call so they actually "use composition";
  # the suppression keeps the stub itself out of ComposeRedundantComposable.
  cat > Stubs.kt <<EOF
package com.example

import androidx.compose.runtime.Composable

@Suppress("ComposeRedundantComposable")
@Composable
fun Text(text: String) {
}
EOF
}

# A unit-returning composable whose lowercase name trips ComposeNamingUppercase.
function write_dirty_composable() {
  cat > MyComposables.kt <<EOF
package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun badButton(modifier: Modifier = Modifier) {
  Text("hello")
}
EOF
}

# A correctly named composable that compose-lints accepts.
function write_clean_composable() {
  cat > MyComposables.kt <<EOF
package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GoodButton(modifier: Modifier = Modifier) {
  Text("hello")
}
EOF
}

# Enables rules_android's legacy aar_import API in the consumer workspace.
function enable_android_in_workspace() {
  cat >> .bazelrc <<EOF
common --experimental_google_legacy_api
EOF
}

# Builds a minimal AAR in the CWD with the compose-lints jar embedded as its lint.jar,
# exercising the collect_aar_outputs_aspect extraction + lint.jar auto-discovery path.
function write_fixture_aar() {
  copy_compose_lint_checks
  python3 - <<'PYEOF'
import io
import zipfile

classes = io.BytesIO()
zipfile.ZipFile(classes, "w").close()

with zipfile.ZipFile("fixture.aar", "w") as aar:
    aar.writestr(
        "AndroidManifest.xml",
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
        'package="com.example.aarfixture"/>',
    )
    aar.writestr("classes.jar", classes.getvalue())
    aar.writestr("R.txt", "")
    aar.write("compose_lint_checks.jar", "lint.jar")
PYEOF
}

# Lint targets that depend on the fixture AAR; custom checks must be discovered from the
# AAR's embedded lint.jar rather than the custom_rules attribute.
function write_aar_dep_lint_targets() {
  cat > BUILD.bazel <<EOF
load("@rules_android//rules:rules.bzl", "aar_import")
load("@rules_android_lint//rules:defs.bzl", "android_lint_test")

aar_import(
    name = "fixture_aar",
    aar = "fixture.aar",
)

filegroup(
    name = "lib",
    srcs = [
        "Composable.kt",
        "Modifier.kt",
        "MyComposables.kt",
        "Stubs.kt",
    ],
)

android_lint_test(
    name = "lib_lint_test",
    srcs = [
        "Composable.kt",
        "Modifier.kt",
        "MyComposables.kt",
        "Stubs.kt",
    ],
    lib = ":lib",
    warnings_as_errors = True,
    deps = [":fixture_aar"],
)
EOF
}

# Lint targets whose custom checks come from the compose-lints jar via custom_rules.
function write_custom_rules_lint_targets() {
  cat > BUILD.bazel <<EOF
load("@rules_android_lint//rules:defs.bzl", "android_lint_test")

filegroup(
    name = "lib",
    srcs = [
        "Composable.kt",
        "Modifier.kt",
        "MyComposables.kt",
        "Stubs.kt",
    ],
)

android_lint_test(
    name = "lib_lint_test",
    srcs = [
        "Composable.kt",
        "Modifier.kt",
        "MyComposables.kt",
        "Stubs.kt",
    ],
    custom_rules = ["compose_lint_checks.jar"],
    lib = ":lib",
    warnings_as_errors = True,
)
EOF
}
