#!/usr/bin/env bash

set -o errexit -o nounset -o pipefail

# Don't include examples in the distribution artifact, to reduce size.
# You may want to add additional exclusions for folders or files that users don't need.
# NB: this mechanism relies on a `git archive` feature, which is much simpler and less
# error-prone than using Bazel to build a release artifact from sources in the repository.
# See https://git-scm.com/docs/git-archive#ATTRIBUTES
echo >>.git/info/attributes "examples export-ignore"

# Set by GH actions, see
# https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables
TAG=${GITHUB_REF_NAME}
# The prefix is chosen to match what GitHub generates for source archives
PREFIX="rules_android_lint-${TAG:1}"
ARCHIVE="rules_android_lint-$TAG.tar.gz"
git archive --format=tar --prefix=${PREFIX}/ ${TAG} | gzip > $ARCHIVE
SHA=$(shasum -a 256 $ARCHIVE | awk '{print $1}')

cat << EOF
## Using Bzlmod with Bazel 6

1. Enable with \`common --enable_bzlmod\` in \`.bazelrc\`.
2. Add to your \`MODULE.bazel\` file:

\`\`\`starlark
bazel_dep(name = "rules_android_lint", version = "${TAG:1}")
\`\`\`

## Using WORKSPACE

Paste this snippet into your `WORKSPACE.bazel` file:

\`\`\`starlark
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "rules_android_lint",
    sha256 = "${SHA}",
    strip_prefix = "${PREFIX}",
    url = "https://github.com/bencodes/rules_android_lint/releases/download/${TAG}/${ARCHIVE}",
)
EOF

echo "\`\`\`" 
