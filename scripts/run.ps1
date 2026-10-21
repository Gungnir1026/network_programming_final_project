param(
    [ValidateSet('server', 'client')][string]$Mode = 'client',
    [ValidateRange(1, 65535)][int]$Port = 5555
)
$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$JarPath = Join-Path $ProjectRoot 'dist/eclassroom.jar'
if (-not (Test-Path $JarPath)) { & (Join-Path $PSScriptRoot 'build.ps1') }
if ($Mode -eq 'server') {
    & java '-Dfile.encoding=UTF-8' -cp $JarPath eclassroom.Server $Port
} else {
    & java '-Dfile.encoding=UTF-8' -jar $JarPath
}
exit $LASTEXITCODE
