@echo off
set MAVEN_VERSION=3.9.11
set BASE_DIR=%~dp0
set DIST_DIR=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%
if not exist "%DIST_DIR%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip'; $z='%BASE_DIR%.mvn\maven.zip'; New-Item -ItemType Directory -Force '%BASE_DIR%.mvn' | Out-Null; Invoke-WebRequest $u -OutFile $z; Expand-Archive $z '%BASE_DIR%.mvn' -Force; Remove-Item $z"
)
call "%DIST_DIR%\bin\mvn.cmd" %*
