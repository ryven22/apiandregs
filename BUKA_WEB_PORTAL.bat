@echo off
title CENA X REGS - WEB & LICENSE PORTAL
echo ===================================================
echo   MEMBUKA CENA X REGS WEB & LICENSE PORTAL
echo ===================================================
echo.
echo Server lokal sedang berjalan di: http://localhost:3000
echo.
echo Login Owner:
echo   Username : regsxd18
echo   Password : leaaaimut1
echo.
echo Menjalankan browser...
start http://localhost:3000
echo.
echo Tekan CTRL + C untuk menutup web portal jika sudah selesai.
echo ===================================================
cd /d "%~dp0\web"
python -m http.server 3000
pause
