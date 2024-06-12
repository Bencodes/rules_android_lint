<!-- Generated with Stardoc: http://skydoc.bazel.build -->

Android Lint rules


<a id="android_lint"></a>

## android_lint

<pre>
android_lint(<a href="#android_lint-name">name</a>, <a href="#android_lint-android_lint_config">android_lint_config</a>, <a href="#android_lint-autofix">autofix</a>, <a href="#android_lint-custom_rules">custom_rules</a>, <a href="#android_lint-deps">deps</a>, <a href="#android_lint-disable_checks">disable_checks</a>, <a href="#android_lint-enable_checks">enable_checks</a>,
             <a href="#android_lint-is_test_sources">is_test_sources</a>, <a href="#android_lint-lib">lib</a>, <a href="#android_lint-manifest">manifest</a>, <a href="#android_lint-output_formats">output_formats</a>, <a href="#android_lint-resource_files">resource_files</a>, <a href="#android_lint-srcs">srcs</a>, <a href="#android_lint-warnings_as_errors">warnings_as_errors</a>)
</pre>



**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="android_lint-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="android_lint-android_lint_config"></a>android_lint_config |  Lint Android Lint configuration file.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional | <code>None</code> |
| <a id="android_lint-autofix"></a>autofix |  Enables lint autofix. This is a no-op right now.   | Boolean | optional | <code>False</code> |
| <a id="android_lint-custom_rules"></a>custom_rules |  Custom lint rules to run.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional | <code>[]</code> |
| <a id="android_lint-deps"></a>deps |  Dependencies that should be on the classpath during execution.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional | <code>[]</code> |
| <a id="android_lint-disable_checks"></a>disable_checks |  List of checks to disable.   | List of strings | optional | <code>[]</code> |
| <a id="android_lint-enable_checks"></a>enable_checks |  List of checks to enable.   | List of strings | optional | <code>[]</code> |
| <a id="android_lint-is_test_sources"></a>is_test_sources |  True when linting test sources, otherwise false.   | Boolean | optional | <code>False</code> |
| <a id="android_lint-lib"></a>lib |  The target being linted. This is needed to get the compiled R files.   | <a href="https://bazel.build/concepts/labels">Label</a> | required |  |
| <a id="android_lint-manifest"></a>manifest |  Android manifest to run Android Lint against.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional | <code>None</code> |
| <a id="android_lint-output_formats"></a>output_formats |  List of output formats to produce. Supported [xml, html]   | List of strings | optional | <code>["xml"]</code> |
| <a id="android_lint-resource_files"></a>resource_files |  Android resource files to run Android Lint against.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional | <code>[]</code> |
| <a id="android_lint-srcs"></a>srcs |  Sources to run Android Lint against.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | required |  |
| <a id="android_lint-warnings_as_errors"></a>warnings_as_errors |  When true, lint will treat warnings as errors.   | Boolean | optional | <code>False</code> |


<a id="android_lint_test"></a>

## android_lint_test

<pre>
android_lint_test(<a href="#android_lint_test-name">name</a>, <a href="#android_lint_test-android_lint_config">android_lint_config</a>, <a href="#android_lint_test-autofix">autofix</a>, <a href="#android_lint_test-baseline">baseline</a>, <a href="#android_lint_test-custom_rules">custom_rules</a>, <a href="#android_lint_test-deps">deps</a>, <a href="#android_lint_test-disable_checks">disable_checks</a>,
                  <a href="#android_lint_test-enable_checks">enable_checks</a>, <a href="#android_lint_test-is_test_sources">is_test_sources</a>, <a href="#android_lint_test-lib">lib</a>, <a href="#android_lint_test-manifest">manifest</a>, <a href="#android_lint_test-output_formats">output_formats</a>, <a href="#android_lint_test-resource_files">resource_files</a>, <a href="#android_lint_test-srcs">srcs</a>,
                  <a href="#android_lint_test-warnings_as_errors">warnings_as_errors</a>)
</pre>



**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="android_lint_test-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="android_lint_test-android_lint_config"></a>android_lint_config |  Lint Android Lint configuration file.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional | <code>None</code> |
| <a id="android_lint_test-autofix"></a>autofix |  Enables lint autofix. This is a no-op right now.   | Boolean | optional | <code>False</code> |
| <a id="android_lint_test-baseline"></a>baseline |  Lint baseline file.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional | <code>None</code> |
| <a id="android_lint_test-custom_rules"></a>custom_rules |  Custom lint rules to run.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional | <code>[]</code> |
| <a id="android_lint_test-deps"></a>deps |  Dependencies that should be on the classpath during execution.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional | <code>[]</code> |
| <a id="android_lint_test-disable_checks"></a>disable_checks |  List of checks to disable.   | List of strings | optional | <code>[]</code> |
| <a id="android_lint_test-enable_checks"></a>enable_checks |  List of checks to enable.   | List of strings | optional | <code>[]</code> |
| <a id="android_lint_test-is_test_sources"></a>is_test_sources |  True when linting test sources, otherwise false.   | Boolean | optional | <code>False</code> |
| <a id="android_lint_test-lib"></a>lib |  The target being linted. This is needed to get the compiled R files.   | <a href="https://bazel.build/concepts/labels">Label</a> | required |  |
| <a id="android_lint_test-manifest"></a>manifest |  Android manifest to run Android Lint against.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional | <code>None</code> |
| <a id="android_lint_test-output_formats"></a>output_formats |  List of output formats to produce. Supported [xml, html]   | List of strings | optional | <code>["xml"]</code> |
| <a id="android_lint_test-resource_files"></a>resource_files |  Android resource files to run Android Lint against.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional | <code>[]</code> |
| <a id="android_lint_test-srcs"></a>srcs |  Sources to run Android Lint against.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | required |  |
| <a id="android_lint_test-warnings_as_errors"></a>warnings_as_errors |  When true, lint will treat warnings as errors.   | Boolean | optional | <code>False</code> |


<a id="AndroidLintResultsInfo"></a>

## AndroidLintResultsInfo

<pre>
AndroidLintResultsInfo(<a href="#AndroidLintResultsInfo-baseline">baseline</a>, <a href="#AndroidLintResultsInfo-xml_output">xml_output</a>, <a href="#AndroidLintResultsInfo-html_output">html_output</a>)
</pre>

Info needed to evaluate lint results

**FIELDS**


| Name  | Description |
| :------------- | :------------- |
| <a id="AndroidLintResultsInfo-baseline"></a>baseline |  The Android Lint baseline output    |
| <a id="AndroidLintResultsInfo-xml_output"></a>xml_output |  The Android Lint xml output    |
| <a id="AndroidLintResultsInfo-html_output"></a>html_output |  The Android Lint html output    |


