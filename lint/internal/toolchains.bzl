"""TODO
"""

load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")
load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven")

_LINT_VERSION = "30.4.1"

def rules_android_lint_toolchains(lint_version = _LINT_VERSION):
    """Configures the required lint dependencies for lint to run.

    Args:
        lint_version: Version of lint to use
    """

    # Java
    rules_java_dependencies()
    rules_java_toolchains()

    # Install the artifacts that are needed
    maven_install(
        name = "rules_android_lint_dependencies",
        artifacts = [
            maven.artifact("junit", "junit", "4.13.2", testonly = True),
            maven.artifact("org.assertj", "assertj-core", "3.24.2", testonly = True),
            "io.reactivex.rxjava3:rxjava:3.0.10",
            "com.xenomachina:kotlin-argparser:2.0.7",
            # TODO These need to be passed in via the toolchain and dynamically
            "com.android.tools.lint:lint:{}".format(lint_version),
            "com.android.tools.lint:lint-api:{}".format(lint_version),
            "com.android.tools.lint:lint-checks:{}".format(lint_version),
            "com.android.tools.lint:lint-model:{}".format(lint_version),
        ],
        repositories = [
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
        fetch_sources = True,
    )
