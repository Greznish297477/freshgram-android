#!/usr/bin/env bash
# freshGram Android — сборка full и lite версий для выкладки на GitHub.
#
# Использование:
#   ./build-versions.sh            # собрать full и lite (universal APK + сплиты)
#   ./build-versions.sh full       # только full
#   ./build-versions.sh lite       # только lite
#
# Результат складывается в ./dist/
#
# Требования: JAVA_HOME (JDK 17+), ANDROID_HOME, ключ подписи в local.properties
# (storeFile/storePassword/keyAlias/keyPassword). ВАЖНО: менять ключ подписи нельзя
# без пересчёта анти-тампер констант в TMessagesProj/jni/colorado/colorado.h,
# иначе приложение будет себя убивать (SIGKILL) при старте.

set -euo pipefail
cd "$(dirname "$0")"

GRADLE_ARGS=(
  --no-daemon
  -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC"
  -Dorg.gradle.parallel=true
  -Pandroid.injected.testOnly=false
)

build() {
  local variant="$1"        # full | lite
  local extra=()
  if [ "$variant" = "lite" ]; then
    extra+=("-PfreshgramLite=true")
  fi

  echo "=== Сборка freshGram ($variant) ==="
  ./gradlew :TMessagesProj_App:assembleRelease "${extra[@]}" "${GRADLE_ARGS[@]}"

  local out="TMessagesProj_App/build/outputs/apk/release"
  mkdir -p dist
  # универсальный APK + основные сплиты
  for f in "$out"/freshGram-*-universal.apk \
           "$out"/freshGram-*-arm64-v8a.apk \
           "$out"/freshGram-*-armeabi-v7a.apk; do
    [ -f "$f" ] || continue
    local base
    base="$(basename "$f")"
    # freshGram-VER-CODE-ABI.apk  ->  freshGram-VER-CODE-<variant>-ABI.apk
    local renamed
    renamed="$(echo "$base" | sed -E "s/^(freshGram-[0-9.]+-[0-9]+)-/\1-${variant}-/")"
    cp -f "$f" "dist/$renamed"
    echo "  -> dist/$renamed"
  done
}

case "${1:-all}" in
  full) build full ;;
  lite) build lite ;;
  all)  build full; build lite ;;
  *) echo "Неизвестный аргумент: $1 (ожидается full|lite|all)"; exit 1 ;;
esac

echo "Готово. APK лежат в ./dist/"
