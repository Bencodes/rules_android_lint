#!/bin/bash
#
# Behavior scenarios for the android_lint and android_lint_test rules, driven through a real
# child Bazel against a scaffolded consumer workspace.

# --- begin runfiles.bash initialization v2 ---
# Copy-pasted from the Bazel Bash runfiles library v2.
set -uo pipefail; f=bazel_tools/tools/bash/runfiles/runfiles.bash
source "${RUNFILES_DIR:-/dev/null}/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "${RUNFILES_MANIFEST_FILE:-/dev/null}" | cut -f2- -d' ')" 2>/dev/null || \
  source "$0.runfiles/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.exe.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  { echo>&2 "ERROR: cannot find $f"; exit 1; }; f=; set -e
# --- end runfiles.bash initialization v2 ---

source "$(rlocation rules_android_lint/tests/bashunit/unittest.bash)" || \
  (echo >&2 "Failed to locate unittest.bash" && exit 1)

source "$(rlocation rules_android_lint/tests/scripts/lint_helper.sh)" || \
  (echo >&2 "Failed to locate lint_helper.sh" && exit 1)

function set_up() {
  rm -rf -- * .bazelrc 2>/dev/null || true
  set_up_lint_workspace
}

function test_clean_sources_pass() {
  write_clean_source
  write_lint_targets "lib" "Foo.java"

  "${BIT_BAZEL_BINARY}" test //:lib_lint_test >& "$TEST_log" || fail "Expected lint test to pass"
}

function test_new_issue_fails_the_lint_test() {
  write_dirty_source
  write_lint_targets "lib" "Foo.java"

  "${BIT_BAZEL_BINARY}" test --test_output=all //:lib_lint_test >& "$TEST_log" \
    && fail "Expected lint test to fail" || true
  expect_log "DefaultLocale"
}

function test_build_rule_reports_issue_in_xml() {
  write_dirty_source
  write_lint_targets "lib" "Foo.java"

  "${BIT_BAZEL_BINARY}" build //:lib_lint >& "$TEST_log" || fail "Expected lint build to pass"
  local bazel_bin
  bazel_bin="$("${BIT_BAZEL_BINARY}" info bazel-bin 2>/dev/null)"
  grep -q "DefaultLocale" "${bazel_bin}/lib_lint.xml" \
    || fail "Expected DefaultLocale issue in the XML report"
}

function test_baseline_suppresses_existing_issue() {
  write_dirty_source
  write_lint_targets "lib" "Foo.java"

  # Generate the baseline from the build rule's report, mirroring the regenerate workflow.
  "${BIT_BAZEL_BINARY}" build //:lib_lint >& "$TEST_log" || fail "Expected lint build to pass"
  local bazel_bin
  bazel_bin="$("${BIT_BAZEL_BINARY}" info bazel-bin 2>/dev/null)"
  cp "${bazel_bin}/lib_lint.xml" lint_baseline.xml

  cat >> BUILD.bazel <<EOF

android_lint_test(
    name = "lib_baselined_lint_test",
    srcs = ["Foo.java"],
    baseline = "lint_baseline.xml",
    lib = ":lib",
    warnings_as_errors = True,
)
EOF

  "${BIT_BAZEL_BINARY}" test //:lib_baselined_lint_test >& "$TEST_log" \
    || fail "Expected baselined lint test to pass"
}

function test_disable_checks_suppresses_issue() {
  write_dirty_source
  cat > BUILD.bazel <<EOF
load("@rules_android_lint//rules:defs.bzl", "android_lint_test")

filegroup(
    name = "lib",
    srcs = ["Foo.java"],
)

android_lint_test(
    name = "lib_lint_test",
    srcs = ["Foo.java"],
    disable_checks = ["DefaultLocale"],
    lib = ":lib",
    warnings_as_errors = True,
)
EOF

  "${BIT_BAZEL_BINARY}" test //:lib_lint_test >& "$TEST_log" \
    || fail "Expected lint test with disabled check to pass"
}

function test_worker_and_local_strategies_agree() {
  write_dirty_source
  write_lint_targets "lib" "Foo.java"

  "${BIT_BAZEL_BINARY}" test --test_output=all --strategy=AndroidLint=worker \
    //:lib_lint_test >& "$TEST_log" \
    && fail "Expected worker-strategy lint test to fail" || true
  expect_log "DefaultLocale"

  "${BIT_BAZEL_BINARY}" clean >& /dev/null
  "${BIT_BAZEL_BINARY}" test --test_output=all --strategy=AndroidLint=local \
    //:lib_lint_test >& "$TEST_log" \
    && fail "Expected local-strategy lint test to fail" || true
  expect_log "DefaultLocale"
}

run_suite "android_lint rule behavior scenarios"
