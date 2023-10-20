"""Aspect to collect the aar outputs from aar_import
"""

AndroidLintAARInfo = provider(
    "A provider to collect all aars from transitive dependencies",
    fields = {
        "aars": "depset of aars",
    },
)

def _collect_aar_outputs_aspect(_, ctx):
    deps = getattr(ctx.rule.attr, "deps", [])
    exports = getattr(ctx.rule.attr, "exports", [])
    associates = getattr(ctx.rule.attr, "associates", [])
    transitive_aar_depsets = []
    for dep in deps + exports + associates:
        if AndroidLintAARInfo in dep:
            transitive_aar_depsets.append(dep[AndroidLintAARInfo].aars)

    direct_aars = None
    if hasattr(ctx.rule.attr, "aar"):
        aar = ctx.rule.attr.aar.files.to_list()[0]
        direct_aars = [aar]

    return [
        AndroidLintAARInfo(
            aars = depset(
                direct = direct_aars,
                transitive = transitive_aar_depsets,
            ),
        ),
    ]

collect_aar_outputs_aspect = aspect(
    implementation = _collect_aar_outputs_aspect,
    attr_aspects = ["aar", "deps", "exports", "associates"],
)
