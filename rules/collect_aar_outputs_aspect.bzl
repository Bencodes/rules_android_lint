"""Aspect to collect the aar outputs from aar_import
"""

load("@rules_android//providers:providers.bzl", "AndroidLibraryAarInfo")

AndroidLintAARInfo = provider(
    "A provider to collect all aars from transitive dependencies",
    fields = {
        "aar": "direct aar",
        "transitive_aar_artifacts": "depset of the transitive aars",
    },
)

AndroidLintAARNodeInfo = provider(
    "A provider to collect the individual aar to aar_directory pairs",
    fields = {
        "aar": "direct aar",
        "aar_dir": "path to the aar directory",
    },
)

def _collect_aar_outputs_aspect(tgt, ctx):
    deps = getattr(ctx.rule.attr, "deps", [])
    exports = getattr(ctx.rule.attr, "exports", [])
    associates = getattr(ctx.rule.attr, "associates", [])

    # Collect the transitive aar artifacts for the given dependencies
    transitive_aar_depsets = []
    for dep in deps + exports + associates:
        if AndroidLintAARInfo in dep:
            transitive_aar_depsets.append(dep[AndroidLintAARInfo].transitive_aar_artifacts)

    # Collect the direct arr artifact for the given target
    aar = None
    if ctx.rule.kind == "aar_import":
        if not hasattr(ctx.rule.attr, "aar"):
            fail("Found aar import without 'aar' rule attribute!")
        aar = ctx.rule.attr.aar.files.to_list()[0]
        # TODO(bencodes) I don't think we need this case since we should be able to extract the information we need
        # directly when constructing the lint action itself. Keeping for now though as it preserves the existing behavior.

    elif AndroidLibraryAarInfo in tgt:
        aar = tgt[AndroidLibraryAarInfo].aar

    current_info = AndroidLintAARNodeInfo(
        aar = None,
        aar_dir = None,
    )

    if aar:
        aar_extract = ctx.actions.declare_directory("aars/" + ctx.label.name + "-aar-contents")
        ctx.actions.run_shell(
            inputs = [aar],
            outputs = [aar_extract],
            mnemonic = "ExtractLintAar",
            progress_message = "Extracting AAR %s's " % (ctx.label.name),
            command = ("unzip -q -o %s -d %s/ " % (aar.path, aar_extract.path)),
        )
        current_info = AndroidLintAARNodeInfo(
            aar = aar,
            aar_dir = aar_extract,
        )

    return [
        AndroidLintAARInfo(
            aar = current_info,
            transitive_aar_artifacts = depset(
                direct = [current_info],
                transitive = transitive_aar_depsets,
            ),
        ),
    ]

collect_aar_outputs_aspect = aspect(
    implementation = _collect_aar_outputs_aspect,
    attr_aspects = ["aar", "deps", "exports", "associates"],
)
