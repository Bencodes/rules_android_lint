# How to Contribute

## Formatting

Starlark files should be formatted by buildifier.
We suggest using a pre-commit hook to automate this.
First [install pre-commit](https://pre-commit.com/#installation),
then run

```shell
pre-commit install
```

Otherwise later tooling on CI will yell at you about formatting/linting violations.

## Testing

Run the complete repository test suite with:

```shell
bazel test //...
```

For a focused run of the lightweight unit and analysis tests, use:

```shell
bazel test //tests:unit_tests
```

The rule behavior tests under `tests/integration` are part of the normal suite. Run them directly
with:

```shell
bazel test //tests/integration:tests
```

The broader end-to-end test uses a standalone consumer workspace:

```shell
cd e2e/simple-android
bazel test //...
```

## Using this as a development dependency of other rules

You'll commonly find that you develop in another WORKSPACE, such as
some other ruleset that depends on rules_android_lint, or in a nested
WORKSPACE under the `e2e` directory.

To always tell Bazel to use this directory rather than some release
artifact or a version fetched from the internet, run this from this
directory:

```sh
OVERRIDE="--override_repository=rules_android_lint=$(pwd)/rules_android_lint"
echo "common $OVERRIDE" >> ~/.bazelrc
```

This means that any usage of `@rules_android_lint` on your system will point to this folder.

## Releasing

1. Determine the next release version, following semver (could automate in the future from changelog)
1. Tag the repo and push it (or create a tag in GH UI)
1. Watch the automation run on GitHub actions
