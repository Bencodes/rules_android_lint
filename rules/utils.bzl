"""Utility functions for Android Lint
"""

ANDROID_LINT_TOOLCHAIN_TYPE = Label("//toolchains:toolchain_type")

def _get_android_lint_toolchain(ctx):
    return ctx.toolchains[ANDROID_LINT_TOOLCHAIN_TYPE]

def _first(collection):
    """Returns the first item in the collection."""
    for i in collection:
        return i
    return fail("Error: The collection is empty.")

def _only(collection):
    """Returns the only item in the collection."""
    if len(collection) != 1:
        fail("Error: Expected one element, has %s." % len(collection))
    return _first(collection)

def _list_or_depset_to_list(list_or_depset):
    if type(list_or_depset) == "list":
        return list_or_depset
    elif type(list_or_depset) == "depset":
        return list_or_depset.to_list()
    else:
        return fail("Error: Expected a list or a depset. Got %s" % type(list_or_depset))

def _module_name(label):
    """Returns an injective, filesystem-safe module ID for a canonical Bazel label.

    Keeping the full label preserves repository, package, and target identity. Percent is escaped
    first so the remaining replacements are reversible and cannot collide with literal escape
    sequences in a label.
    """
    result = str(label)
    for character, replacement in [
        ("%", "%25"),
        ("/", "%2F"),
        ("\\", "%5C"),
        (":", "%3A"),
        ("=", "%3D"),
        ("<", "%3C"),
        (">", "%3E"),
        ("\"", "%22"),
        ("|", "%7C"),
        ("?", "%3F"),
        ("*", "%2A"),
    ]:
        result = result.replace(character, replacement)
    return result

utils = struct(
    first = _first,
    get_android_lint_toolchain = _get_android_lint_toolchain,
    list_or_depset_to_list = _list_or_depset_to_list,
    module_name = _module_name,
    only = _only,
)
