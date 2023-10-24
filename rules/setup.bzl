"""Declare runtime dependencies
"""

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

#load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")
load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")
load("@rules_jvm_external//:defs.bzl", "maven_install")

def rules_android_lint_setup():
    rules_java_dependencies()
    rules_java_toolchains()

    kotlin_repositories()

    rules_jvm_external_deps()

    maven_install(
        name = "rules_jvm_external_deps",
        artifacts = [
            # Testing
            "org.assertj:assertj-core:3.24.2",
            "junit:junit:4.13.2",
            # Worker Dependencies
            # TODO(bencodes) Remove these and use the worker impl. that Bazel defines internally
            "com.squareup.moshi:moshi:1.15.0",
            "com.squareup.moshi:moshi-kotlin:1.15.0",
            "com.squareup.okio:okio-jvm:3.6.0",
            "io.reactivex.rxjava3:rxjava:3.1.8",
            "com.xenomachina:kotlin-argparser:2.0.7",
            # Lint Dependencies
            "com.android.tools.lint:lint:31.3.0-alpha09",
            "com.android.tools.lint:lint-api:31.3.0-alpha09",
            "com.android.tools.lint:lint-checks:31.3.0-alpha09",
            "com.android.tools.lint:lint-model:31.3.0-alpha09",
        ],
        repositories = [
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
    )
