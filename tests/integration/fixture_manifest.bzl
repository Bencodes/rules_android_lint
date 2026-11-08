"""Expose the Android binary's configured manifest for the positive control."""

load("@rules_android//providers:providers.bzl", "AndroidIdeInfo")

def _processed_manifest_impl(ctx):
    return [DefaultInfo(files = depset([ctx.attr.app[AndroidIdeInfo].generated_manifest]))]

processed_manifest = rule(
    implementation = _processed_manifest_impl,
    attrs = {"app": attr.label(mandatory = True, providers = [AndroidIdeInfo])},
)
