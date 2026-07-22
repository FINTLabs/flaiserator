#!/bin/sh

set -eu

git config --local core.hooksPath scripts/hooks/
chmod +x scripts/hooks/*
