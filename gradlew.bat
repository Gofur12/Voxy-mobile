@echo off
setlocal EnableExtensions
set "VERSION=8.6"
set "SHA256=9631d53cf3e74bfa726893aee1f8994fee4e060c401335946dba2156f440f24c"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "BASE=%GRADLE_USER_HOME%\wrapper\dists\gradle-%VERSION%-bin\voxymobile-bootstrap"
set "GRADLE=%BASE%\gradle-%VERSION%\bin\gradle.bat"
if exist "%GRADLE%" goto run
where java >nul 2>nul || (echo ERROR: Java 17+ is required.& exit /b 2)
if not exist "%BASE%" mkdir "%BASE%"
set "ARCHIVE=%BASE%\gradle-%VERSION%-bin.zip"
if exist "%ARCHIVE%" (
  for /f %%H in ('powershell -NoProfile -Command "(Get-FileHash -Algorithm SHA256 -LiteralPath '%ARCHIVE%').Hash.ToLower()"') do set "CACHED=%%H"
  if /i not "%CACHED%"=="%SHA256%" del /q "%ARCHIVE%" 2>nul
)
if not exist "%ARCHIVE%" (
  echo Downloading Gradle %VERSION% from the official distribution service...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%ARCHIVE%.part'; Move-Item -LiteralPath '%ARCHIVE%.part' -Destination '%ARCHIVE%' -Force } catch { exit 1 }"
  if errorlevel 1 (del /q "%ARCHIVE%.part" 2>nul & exit /b 2)
)
for /f %%H in ('powershell -NoProfile -Command "(Get-FileHash -Algorithm SHA256 -LiteralPath '%ARCHIVE%').Hash.ToLower()"') do set "ACTUAL=%%H"
if /i not "%ACTUAL%"=="%SHA256%" (
  del /q "%ARCHIVE%" 2>nul
  echo ERROR: Gradle ZIP checksum mismatch; refusing to execute it.
  exit /b 3
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ARCHIVE%' -DestinationPath '%BASE%' -Force"
if errorlevel 1 exit /b 3
del /q "%ARCHIVE%" 2>nul
if not exist "%GRADLE%" (echo ERROR: Gradle bootstrap extraction failed.& exit /b 3)
:run
call "%GRADLE%" %*
exit /b %ERRORLEVEL%
