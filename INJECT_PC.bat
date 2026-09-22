@echo off
REM INJECT_PC - dorong file external ke FF MAX & FF TH via adb (terbukti bisa di Android 14)
REM Syarat: HP konek USB + USB Debugging ON

setlocal
set SERIAL=10DD270CXH001AJ
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
set ROOT=%~dp0

echo [1/5] Cek device...
"%ADB%" -s %SERIAL% shell echo konek
if errorlevel 1 (
  echo HP tidak ketemu via USB. Cek kabel + USB Debugging, lalu ulangi.
  pause
  exit /b 1
)

echo [2/5] Inject FF MAX...
"%ADB%" -s %SERIAL% push "%ROOT%app\src\main\assets\patches\com.dts.freefiremax\files\Assembly-CSharp-patch.bytes" "/sdcard/Android/data/com.dts.freefiremax/files/Assembly-CSharp-patch.bytes"
"%ADB%" -s %SERIAL% push "%ROOT%app\src\main\assets\patches\com.dts.freefiremax\files\localConfig.json" "/sdcard/Android/data/com.dts.freefiremax/files/localConfig.json"

echo [3/5] Inject FF TH...
"%ADB%" -s %SERIAL% push "%ROOT%app\src\main\assets\patches\com.dts.freefireth\files\Assembly-CSharp-patch.bytes" "/sdcard/Android/data/com.dts.freefireth/files/Assembly-CSharp-patch.bytes"
"%ADB%" -s %SERIAL% push "%ROOT%app\src\main\assets\patches\com.dts.freefireth\files\localConfig.json" "/sdcard/Android/data/com.dts.freefireth/files/localConfig.json"

echo [4/5] Verifikasi...
"%ADB%" -s %SERIAL% shell "ls -l /sdcard/Android/data/com.dts.freefiremax/files/Assembly-CSharp-patch.bytes /sdcard/Android/data/com.dts.freefiremax/files/localConfig.json /sdcard/Android/data/com.dts.freefireth/files/Assembly-CSharp-patch.bytes /sdcard/Android/data/com.dts.freefireth/files/localConfig.json"

echo [5/5] Selesai. Buka game dan cek.
pause
