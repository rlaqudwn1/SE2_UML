#!/usr/bin/env bash
set -euo pipefail

se2_repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
se2_build_dir="$(mktemp -d "${TMPDIR:-/tmp}/se2-model-check.XXXXXX")"
trap 'rm -rf -- "$se2_build_dir"' EXIT

mapfile -t se2_sources < <(find "$se2_repo_root/src/main/java" "$se2_repo_root/src/test/java" -name '*.java' -print | sort)
javac --release 25 -Xlint:all -Werror -d "$se2_build_dir" "${se2_sources[@]}"
java -cp "$se2_build_dir" org.se2uml.layout.SequenceLayoutContractCheck
