# Локальная сборка freshGram (Windows).
#
# Скрипт сам подставляет JAVA_HOME (JDK 25) и ANDROID_HOME (Android SDK),
# чтобы не настраивать переменные окружения вручную.
#
# Примеры:
#   .\Tools\build-local.ps1                          # собрать release APK (full)
#   .\Tools\build-local.ps1 -Task assembleDebug      # debug APK
#   .\Tools\build-local.ps1 -Lite                    # lite-вариант (без приватных фич)
#   .\Tools\build-local.ps1 -Task compileDebugJavaWithJavac   # быстрая проверка Java
#
# Требуется: JDK 25, Android SDK (platform 37, build-tools 36, NDK 27.3.13750724, CMake 3.22.1).

param(
    [string]$Task = "assembleRelease",
    [switch]$Lite,
    [int]$HeapGb = 6
)

$ErrorActionPreference = "Stop"

function Find-Jdk25 {
    $roots = @("C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java", "C:\Program Files\Android\Android Studio\jbr")
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        $candidates = Get-ChildItem $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '25' } |
            Sort-Object Name -Descending
        foreach ($c in $candidates) {
            $java = Join-Path $c.FullName "bin\java.exe"
            if (Test-Path $java) { return $c.FullName }
        }
    }
    return $null
}

function Find-AndroidSdk {
    $candidates = @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    ) | Where-Object { $_ -and (Test-Path (Join-Path $_ "platform-tools")) }
    if ($candidates.Count -gt 0) { return $candidates[0] }
    return $null
}

$jdk = Find-Jdk25
if (-not $jdk) {
    Write-Error "JDK 25 не найден. Установите: winget install --id EclipseAdoptium.Temurin.25.JDK"
}
$sdk = Find-AndroidSdk
if (-not $sdk) {
    Write-Error "Android SDK не найден (ожидается `$env:LOCALAPPDATA\Android\Sdk)."
}

$env:JAVA_HOME = $jdk
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

Write-Host "JDK:    $jdk"
Write-Host "SDK:    $sdk"
Write-Host "Task:   $Task$(if ($Lite) { '  (lite)' } else { '' })"

$repo = Split-Path -Parent $PSScriptRoot
Push-Location $repo
try {
    $args = @(":$Task", "--no-daemon", "--console=plain",
              "-Dorg.gradle.jvmargs=-Xmx${HeapGb}g -XX:MaxMetaspaceSize=1536m -Dfile.encoding=UTF-8")
    if ($Lite) { $args += "-PfreshgramLite=true" }
    & .\gradlew.bat @args
    $code = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($code -ne 0) {
    Write-Error "Сборка завершилась с кодом $code"
}
Write-Host "Готово: $Task"
