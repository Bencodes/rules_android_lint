"""A hermetic AAR fixture containing a supplied custom lint jar."""

def _lint_aar_fixture_impl(ctx):
    output = ctx.actions.declare_file(ctx.label.name + ".aar")
    ctx.actions.run(
        executable = ctx.executable._builder,
        inputs = [ctx.file.lint_jar, ctx.file.manifest],
        outputs = [output],
        arguments = [
            "--lint-jar",
            ctx.file.lint_jar.path,
            "--manifest",
            ctx.file.manifest.path,
            "--output",
            output.path,
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
        "_builder": attr.label(
            default = Label("//tests/integration:fixture_aar_builder"),
            cfg = "exec",
            executable = True,
        ),
    },
)
