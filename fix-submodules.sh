#!/usr/bin/env bash
# Приводит все сабмодули к коммитам, записанным в суперпроекте (gitlink SHA).
# Нужно потому, что `git submodule update --depth 1` в этом окружении
# оставил у части сабмодулей вершины веток вместо зафиксированных коммитов.
set -uo pipefail
cd "$(dirname -- "${BASH_SOURCE[0]}")"

fail=0
git submodule foreach --recursive '
  cur=$(git rev-parse HEAD 2>/dev/null || echo none)
  if [ "$cur" = "$sha1" ]; then
    exit 0
  fi
  echo ">> $sm_path: $cur -> $sha1"
  if git fetch --depth 1 --quiet origin "$sha1" 2>/dev/null; then
    git checkout --quiet "$sha1" || { echo "!! checkout failed: $sm_path"; exit 1; }
  else
    echo "!! fetch by SHA failed, unshallowing: $sm_path"
    git fetch --unshallow --quiet origin 2>/dev/null || git fetch --quiet origin
    git checkout --quiet "$sha1" || { echo "!! checkout failed: $sm_path"; exit 1; }
  fi
' || fail=1

echo "--- result ---"
git submodule status | grep -E "^[+-]" && echo "MISMATCHES REMAIN" || echo "all submodules at recorded commits"
exit $fail
