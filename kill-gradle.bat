@echo off
echo Stopping all Gradle processes...
taskkill /f /im java.exe 2>nul
taskkill /f /im gradle.exe 2>nul
taskkill /f /im gradlew.exe 2>nul

echo Removing Gradle lock files...
del /f /q ".gradle\8.13\executionHistory\*.lock" 2>nul
del /f /q ".gradle\8.13\fileHashes\*.lock" 2>nul
del /f /q ".gradle\8.13\gc.properties" 2>nul

echo Cleaning build directories...
rmdir /s /q "app\build" 2>nul
rmdir /s /q "build" 2>nul

echo Gradle cleanup completed!
pause
