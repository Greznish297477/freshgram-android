<#
.SYNOPSIS
    Проверяет ключ подписи freshGram и готовит значения для секретов GitHub Actions.

.DESCRIPTION
    Скрипт делает три вещи:
      1. показывает сертификат из .jks (владелец, SHA-256, срок действия);
      2. сравнивает размер и CRC32 сертификата с константами CERT_SIZE / CERT_HASH
         из TMessagesProj/jni/colorado/colorado.h — при несовпадении приложение
         будет убито анти-тампером сразу после запуска (SIGKILL);
      3. печатает base64 от .jks — это значение секрета KEYSTORE_JKS.

.PARAMETER Keystore
    Путь к .jks / .keystore.

.PARAMETER Alias
    Alias ключа внутри хранилища (для freshGram — freshgram).

.PARAMETER StorePassword
    Пароль хранилища. Если не указан, keytool спросит интерактивно.

.PARAMETER CopyToClipboard
    Скопировать base64 в буфер обмена (для вставки в GitHub как есть).

.EXAMPLE
    .\tools\keystore-check.ps1 -Keystore C:\keys\freshgram-release.jks -Alias freshgram -CopyToClipboard
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Keystore,
    [string]$Alias = 'freshgram',
    [string]$StorePassword,
    [switch]$CopyToClipboard
)

$ErrorActionPreference = 'Stop'

# Чтобы русский текст корректно отображался и в старом conhost, и в Windows Terminal.
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

if (-not (Test-Path -LiteralPath $Keystore)) {
    throw "Файл не найден: $Keystore"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$coloradoPath = Join-Path $repoRoot 'TMessagesProj/jni/colorado/colorado.h'

$keytool = 'keytool'
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
    if (Test-Path -LiteralPath $candidate) { $keytool = $candidate }
}

# keytool пишет прогресс в stderr; в Windows PowerShell 5.1 это ErrorRecord,
# поэтому на время вызова отключаем Stop и собираем вывод в строки.
function Invoke-Keytool {
    param([string[]]$Arguments)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $text = (& $keytool @Arguments 2>&1) | ForEach-Object { $_.ToString() }
        [pscustomobject]@{ ExitCode = $LASTEXITCODE; Lines = @($text) }
    } finally {
        $ErrorActionPreference = $prev
    }
}

$storePassArgs = @()
if (-not $StorePassword) {
    # Спрашиваем один раз: без этого keytool запрашивал бы пароль на каждый вызов.
    $secure = Read-Host -AsSecureString "Пароль хранилища $([System.IO.Path]::GetFileName($Keystore))"
    if ($secure.Length -gt 0) {
        $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            $StorePassword = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        } finally {
            [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
}
if ($StorePassword) { $storePassArgs = @('-storepass', $StorePassword) }

# --- 1. сертификат ------------------------------------------------------------
$tmp = [System.IO.Path]::GetTempFileName()
try {
    $export = Invoke-Keytool (@('-exportcert', '-keystore', $Keystore, '-alias', $Alias, '-file', $tmp) + $storePassArgs)
    if ($export.ExitCode -ne 0) {
        throw "Не удалось прочитать сертификат. Проверьте alias '$Alias' и пароль хранилища.`n$($export.Lines -join "`n")"
    }
    $der = [System.IO.File]::ReadAllBytes($tmp)
} finally {
    Remove-Item -LiteralPath $tmp -ErrorAction SilentlyContinue
}

# --- CRC32 (как zlib.crc32 в colorado.cpp) ------------------------------------
$table = New-Object 'uint32[]' 256
for ($i = 0; $i -lt 256; $i++) {
    [uint32]$c = [uint32]$i
    for ($k = 0; $k -lt 8; $k++) {
        if ($c -band 1) {
            $c = [uint32]((0xEDB88320L -bxor ($c -shr 1)) -band 0xFFFFFFFFL)
        } else {
            $c = [uint32]($c -shr 1)
        }
    }
    $table[$i] = $c
}
[uint32]$crc = [uint32]4294967295
foreach ($b in $der) {
    [uint32]$idx = (($crc -bxor [uint32]$b) -band 0xFF)
    $crc = [uint32](($table[$idx] -bxor ($crc -shr 8)) -band 0xFFFFFFFFL)
}
[uint32]$crc = [uint32](($crc -bxor 0xFFFFFFFFL) -band 0xFFFFFFFFL)

Write-Host ''
Write-Host '=== Сертификат ===' -ForegroundColor Cyan
$info = Invoke-Keytool (@('-list', '-v', '-keystore', $Keystore, '-alias', $Alias) + $storePassArgs)
$info.Lines |
    Select-String -Pattern 'Owner:|Issuer:|Serial number:|Valid from:|SHA256:|Signature algorithm' |
    ForEach-Object { '  ' + $_.Line.Trim() }
Write-Host ("  Размер (DER): {0} (0x{1:x})" -f $der.Length, $der.Length)
Write-Host ("  CRC32       : 0x{0:x8}" -f $crc)

# --- 2. сравнение с colorado.h ------------------------------------------------
Write-Host ''
Write-Host '=== Проверка анти-тампера (colorado.h) ===' -ForegroundColor Cyan
if (-not (Test-Path -LiteralPath $coloradoPath)) {
    Write-Host "  colorado.h не найден: $coloradoPath" -ForegroundColor Yellow
} else {
    $header = Get-Content -LiteralPath $coloradoPath -Raw
    $mSize = [regex]::Match($header, '#define\s+CERT_SIZE\s+0x([0-9a-fA-F]+)')
    $mHash = [regex]::Match($header, '#define\s+CERT_HASH\s+0x([0-9a-fA-F]+)')
    if (-not $mSize.Success -or -not $mHash.Success) {
        Write-Host '  Не нашёл CERT_SIZE / CERT_HASH в colorado.h' -ForegroundColor Yellow
    } else {
        $wantSize = [Convert]::ToInt64($mSize.Groups[1].Value, 16)
        $wantHash = [Convert]::ToInt64($mHash.Groups[1].Value, 16)
        $okSize = ($wantSize -eq $der.Length)
        $okHash = ($wantHash -eq [int64]$crc)
        $sizeState = if ($okSize) { 'OK' } else { 'НЕ СОВПАЛО' }
        $hashState = if ($okHash) { 'OK' } else { 'НЕ СОВПАЛО' }
        Write-Host ("  CERT_SIZE 0x{0:x}  -> {1}" -f $wantSize, $sizeState) -ForegroundColor $(if ($okSize) { 'Green' } else { 'Red' })
        Write-Host ("  CERT_HASH 0x{0:x8} -> {1}" -f $wantHash, $hashState) -ForegroundColor $(if ($okHash) { 'Green' } else { 'Red' })

        if ($okSize -and $okHash) {
            Write-Host '  Ключ подходит для сборки — приложение не будет убито при старте.' -ForegroundColor Green
        } else {
            Write-Host ''
            Write-Host '  ВНИМАНИЕ: этим ключом собирать нельзя — анти-тампер убьёт процесс.' -ForegroundColor Red
            Write-Host '  Либо возьмите исходный ключ, либо впишите в colorado.h:' -ForegroundColor Red
            Write-Host ("    #define CERT_HASH 0x{0:x8}" -f $crc)
            Write-Host ("    #define CERT_SIZE 0x{0:x}" -f $der.Length)
            Write-Host '  После смены ключа уже установленное приложение придётся переустановить:' -ForegroundColor Yellow
            Write-Host '  Android не даёт обновить APK с другой подписью.' -ForegroundColor Yellow
        }
    }
}

# --- 3. base64 для секрета KEYSTORE_JKS --------------------------------------
Write-Host ''
Write-Host '=== Значение секрета KEYSTORE_JKS ===' -ForegroundColor Cyan
$base64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $Keystore)))
Write-Host ("  Длина base64: {0} символов" -f $base64.Length)
if ($CopyToClipboard) {
    Set-Clipboard -Value $base64
    Write-Host '  Скопировано в буфер обмена — вставьте в поле Value секрета KEYSTORE_JKS.' -ForegroundColor Green
} else {
    Write-Host '  (запустите с -CopyToClipboard, чтобы сразу скопировать в буфер обмена)' -ForegroundColor Yellow
    Write-Host ''
    Write-Host $base64
}
Write-Host ''
