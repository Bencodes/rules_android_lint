#!/bin/bash

cd lint-examples || exit

bazel test //android:lib_lint //simple:lib_lint --test_output=all
