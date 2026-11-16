#!/usr/bin/env bash
# Локальная сборка freshGram Android на этом ПК.
# Использование: ./build-local.sh [задача Gradle] [доп. аргументы]
#   ./build-local.sh                    -> assembleRelease (arm64-v8a)
#   ./build-local.sh assembleDebug      -> debug-сборка
#   ./build-local.sh assembleRelease -Pandroid.injected.build.abi=universal
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-$HOME/jdk25}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

TASK="${1:-assembleRelease}"
shift || true

ABI_ARG=(-Pandroid.injected.build.abi=arm64-v8a)
for a in "$@"; do
  [[ "$a" == -Pandroid.injected.build.abi=* ]] && ABI_ARG=()
done

echo "JDK:    $JAVA_HOME"
echo "SDK:    $ANDROID_HOME"
echo "Задача: $TASK ${ABI_ARG[*]:-} $*"
echo

exec ./gradlew "$TASK" "${ABI_ARG[@]}" --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC" \
  -Dorg.gradle.parallel=true \
  "$@"
