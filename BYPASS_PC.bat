@echo off
REM BYPASS_PC - hapus file external dari FF MAX & FF TH via adb
REM Syarat: HP konek USB + USB Debugging ON

setlocal
set SERIAL=10DD270CXH001AJ
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe

echo Cek device...
"%ADB%" -s %SERIAL% shell echo konek
if errorlevel 1 (
  echo HP tidak ketemu via USB. Cek kabel + USB Debugging, lalu ulangi.
  pause
  exit /b 1
)

echo Hapus file FF MAX...
"%ADB%" -s %SERIAL% shell "rm -f /sdcard/Android/data/com.dts.freefiremax/files/Assembly-CSharp-patch.bytes /sdcard/Android/data/com.dts.freefiremax/files/localConfig.json; ls /sdcard/Android/data/com.dts.freefiremax/files/"

echo Hapus file FF TH...
"%ADB%" -s %SERIAL% shell "rm -f /sdcard/Android/data/com.dts.freefireth/files/Assembly-CSharp-patch.bytes /sdcard/Android/data/com.dts.freefireth/files/localConfig.json; ls /sdcard/Android/data/com.dts.freefireth/files/"

echo Selesai. BYPASS beres.
pause
