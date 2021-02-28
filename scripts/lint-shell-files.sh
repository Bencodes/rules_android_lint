#!/bin/bash

find . -type f -name "*.sh" -exec "shellcheck" {} \;
