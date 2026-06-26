#!/bin/bash
#
# Scenarios for custom lint checks embedded in AAR dependencies (lint.jar), exercising the
# collect_aar_outputs_aspect extraction and auto-discovery path.

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
  enable_android_in_workspace
}

function test_aar_embedded_lint_jar_flags_issue() {
  write_composable_annotation_stub
  write_dirty_composable
  write_fixture_aar
  write_aar_dep_lint_targets

  "${BIT_BAZEL_BINARY}" test --test_output=all //:lib_lint_test >& "$TEST_log" \
    && fail "Expected lint test with AAR-embedded checks to fail" || true
  expect_log "ComposeNamingUppercase"
}

function test_aar_embedded_lint_jar_accepts_clean_composable() {
  write_composable_annotation_stub
  write_clean_composable
  write_fixture_aar
  write_aar_dep_lint_targets

  "${BIT_BAZEL_BINARY}" test --test_output=all //:lib_lint_test >& "$TEST_log" \
    || fail "Expected lint test with AAR-embedded checks to pass"
}

run_suite "android_lint AAR lint.jar discovery scenarios"
