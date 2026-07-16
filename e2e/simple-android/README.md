# Simple Android end-to-end test

This standalone Bazel workspace verifies that a consumer can register the Android lint toolchain and use `android_lint` and `android_lint_test` with a real Android dependency. CI runs it with both local and worker execution strategies.

Run it from this directory:

```sh
bazel test //...
```
