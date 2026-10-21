$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'build.ps1')
$JarPath = Join-Path $ProjectRoot 'dist/eclassroom.jar'
$TestDir = Join-Path $ProjectRoot ('build/tests.' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $TestDir | Out-Null
try {
    $Sources = @(Get-ChildItem (Join-Path $ProjectRoot 'src/test/java/eclassroom/*.java') | ForEach-Object FullName)
    & javac --release 17 -encoding UTF-8 -cp $JarPath -d $TestDir @Sources
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    $ClassPath = $JarPath + [IO.Path]::PathSeparator + $TestDir
    foreach ($Test in @('eclassroom.CoreTest', 'eclassroom.ServerIntegrationTest')) {
        & java '-Djava.awt.headless=true' -cp $ClassPath $Test
        if ($LASTEXITCODE -ne 0) { throw "$Test failed." }
    }
} finally {
    Remove-Item -LiteralPath $TestDir -Recurse -Force
}
