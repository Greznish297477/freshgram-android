@echo off
rem Обёртка для запуска проверки ключа без изменения ExecutionPolicy.
rem Использование:
rem   Tools\keystore-check.cmd -Keystore C:\Users\Gryaz\keys\freshgram-release.jks -Alias freshgram -CopyToClipboard
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0keystore-check.ps1" %*
