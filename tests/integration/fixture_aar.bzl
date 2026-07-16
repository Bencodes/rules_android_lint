"""A hermetic AAR fixture containing a supplied custom lint jar."""

def _lint_aar_fixture_impl(ctx):
    output = ctx.actions.declare_file(ctx.label.name + ".aar")
    classes_jar = ctx.actions.declare_file(ctx.label.name + "_classes.jar")

    ctx.actions.run(
        executable = ctx.executable._zipper,
        outputs = [classes_jar],
        arguments = [
            "c",
            classes_jar.path,
            "META-INF/MANIFEST.MF=",
        ],
        mnemonic = "BuildEmptyLintAarClassesJar",
        progress_message = "Building empty classes jar for lint AAR fixture %{label}",
    )
    ctx.actions.run(
        executable = ctx.executable._zipper,
        inputs = [classes_jar, ctx.file.lint_jar, ctx.file.manifest],
        outputs = [output],
        arguments = [
            "c",
            output.path,
            "AndroidManifest.xml={}".format(ctx.file.manifest.path),
            "classes.jar={}".format(classes_jar.path),
            "R.txt=",
            "lint.jar={}".format(ctx.file.lint_jar.path),
        ],
        mnemonic = "BuildLintAarFixture",
        progress_message = "Building lint AAR fixture %{label}",
    )
    return [DefaultInfo(files = depset([output]))]

lint_aar_fixture = rule(
    implementation = _lint_aar_fixture_impl,
    attrs = {
        "lint_jar": attr.label(
            allow_single_file = [".jar"],
            mandatory = True,
        ),
        "manifest": attr.label(
            allow_single_file = [".xml"],
            mandatory = True,
        ),
        "_zipper": attr.label(
            default = Label("@bazel_tools//tools/zip:zipper"),
            cfg = "exec",
            executable = True,
        ),
    },
)
