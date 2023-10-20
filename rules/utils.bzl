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

utils = struct(
    first = _first,
    only = _only,
    list_or_depset_to_list = _list_or_depset_to_list,
    get_android_lint_toolchain = _get_android_lint_toolchain,
)
