param(
    [ValidateSet('phone', 'emulator')]
    [string]$Target = 'phone'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

try {
    # Match Gradle's local SDK location, without requiring adb on PATH.
    $sdkRoot = $env:ANDROID_HOME
    if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
    if (-not $sdkRoot) { $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    if (Test-Path -LiteralPath 'local.properties') {
        $sdkSetting = Get-Content -LiteralPath 'local.properties' |
            Where-Object { $_ -match '^sdk\.dir\s*=' } | Select-Object -First 1
        if ($sdkSetting) {
            $sdkRoot = ($sdkSetting -replace '^sdk\.dir\s*=\s*', '').Replace('\:', ':').Replace('\\', '\')
        }
    }
    $adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adb)) {
        throw 'ADB was not found. Set sdk.dir in local.properties to your Android SDK folder.'
    }

    $deviceLines = & $adb devices
    if ($LASTEXITCODE -ne 0) { throw 'ADB could not list connected devices.' }
    $devices = @(
        foreach ($line in $deviceLines) {
            if ($line -match '^(\S+)\s+(device|unauthorized|offline)\b') {
                $serial = $Matches[1]
                $state = $Matches[2]
                $isEmulator = $serial.StartsWith('emulator-')
                if (($Target -eq 'emulator') -eq $isEmulator) {
                    [pscustomobject]@{ Serial = $serial; State = $state }
                }
            }
        }
    )
    if ($devices.Count -eq 0) {
        if ($Target -eq 'emulator') {
            throw 'Start an Android emulator, wait for its home screen, then click Run again.'
        }
        throw 'Connect your phone, enable USB debugging, and accept its USB debugging prompt, then click Run again.'
    }
    if ($devices.Count -gt 1) {
        throw "More than one $Target is connected. Leave only the intended $Target connected, then click Run again."
    }
    $device = $devices[0]
    if ($device.State -ne 'device') {
        throw "Device $($device.Serial) is $($device.State). Unlock and authorize your phone, or wait for your emulator to finish starting."
    }
    $serial = $device.Serial
    Write-Host "Building debug app for $Target ($serial)..."
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Debug build failed. See the Gradle error above.' }

    $apkDirectory = Join-Path $projectRoot 'app\build\outputs\apk\debug'
    $metadata = Get-Content -LiteralPath (Join-Path $apkDirectory 'output-metadata.json') -Raw | ConvertFrom-Json
    $apk = Join-Path $apkDirectory $metadata.elements[0].outputFile
    $packageName = $metadata.applicationId

    Write-Host "Installing on $serial..."
    & $adb -s $serial install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK installation failed. See the ADB error above.' }

    $activityLines = & $adb -s $serial shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $packageName
    if ($LASTEXITCODE -ne 0) { throw 'Could not resolve the app launcher activity.' }
    $activity = $activityLines | Where-Object { $_ -match '^\S+/\S+$' } | Select-Object -Last 1
    if (-not $activity) { throw "No launcher activity found for $packageName." }

    Write-Host "Launching $packageName..."
    $launchOutput = & $adb -s $serial shell am start -S -W -n $activity
    $launchExitCode = $LASTEXITCODE
    $launchOutput | ForEach-Object { Write-Host $_ }
    if ($launchExitCode -ne 0 -or -not ($launchOutput -match '^Status: ok')) {
        throw 'Android could not launch the app. See the launch result above.'
    }
    Write-Host "App launched on $Target ($serial). This Run configuration does not attach a Kotlin breakpoint debugger."
} catch {
    Write-Host "Run failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
