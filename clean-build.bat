@echo off
echo === Gradle Lock Issue Fix ===

echo 1. Killing all Java processes...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im javaw.exe >nul 2>&1

echo 2. Waiting for processes to terminate...
timeout /t 3 >nul

echo 3. Using temporary Gradle home...
set GRADLE_USER_HOME=C:\temp\gradle-cache
if not exist "C:\temp\gradle-cache" mkdir "C:\temp\gradle-cache"

echo 4. Running clean build with no daemon...
gradle clean build --no-daemon --no-build-cache --no-parallel --init-script init.gradle

echo 5. Build completed!
pause
