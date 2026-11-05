$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    $localJdk = Get-ChildItem -LiteralPath '.tools/jdk17' -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or !(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        throw 'Configure JAVA_HOME with JDK 17 before running this script.'
    }
    $env:GRADLE_USER_HOME = Join-Path $projectDirectory '.gradle-user-home'
    if (Test-Path -LiteralPath '.tools/android-sdk') { $env:ANDROID_HOME = (Resolve-Path '.tools/android-sdk').Path }
    & .\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --no-daemon --console=plain
    exit $LASTEXITCODE
} finally { Pop-Location }
