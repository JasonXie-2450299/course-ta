param(
    [ValidateSet('Build','Test','Run','Audit')][string]$Task = 'Build',
    [string]$IdePath = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1'
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskSocketDir = Join-Path $env:USERPROFILE '.codex\tmp\courseta-sockets'
New-Item -ItemType Directory -Force -Path $taskSocketDir | Out-Null
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    # Windows AF_UNIX can fail with a short (8.3) TEMP path. Use an explicit full path.
    $env:JAVA_TOOL_OPTIONS = ($previousJavaOptions + ' "-Djdk.net.unixdomain.tmpdir=' + $taskSocketDir + '"').Trim()
    $gradleArgs = @('--console=plain')
    if (Test-Path -LiteralPath (Join-Path $IdePath 'product-info.json')) {
        $gradleArgs += '-PlocalIdePath=' + $IdePath
    }
    switch ($Task) {
        'Build' { $gradleArgs += @(':core:test','buildPlugin','verifyPluginProjectConfiguration') }
        'Test' { $gradleArgs += ':core:test' }
        'Run' { $gradleArgs += 'runIde' }
        'Audit' { $gradleArgs += @(':core:auditCourse',('-PcourseDir=' + (Join-Path $taskRoot 'course-materials'))) }
    }
    Push-Location $taskRoot
    try { & .\gradlew.bat @gradleArgs; if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" } }
    finally { Pop-Location }
} finally { $env:JAVA_TOOL_OPTIONS = $previousJavaOptions }
