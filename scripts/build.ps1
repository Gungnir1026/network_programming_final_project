$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path $PSScriptRoot -Parent
foreach ($Tool in @('javac', 'jar')) {
    if (-not (Get-Command $Tool -ErrorAction SilentlyContinue)) {
        throw "Missing $Tool. Install JDK 17 or newer and add its bin directory to PATH."
    }
}
$ClassDir = Join-Path $ProjectRoot ('build/classes.' + [guid]::NewGuid().ToString('N'))
$DistDir = Join-Path $ProjectRoot 'dist'
New-Item -ItemType Directory -Force -Path $ClassDir, $DistDir | Out-Null
try {
    $Sources = @(Get-ChildItem (Join-Path $ProjectRoot 'src/main/java/eclassroom/*.java') | ForEach-Object FullName)
    & javac --release 17 -encoding UTF-8 -d $ClassDir @Sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    Copy-Item (Join-Path $ProjectRoot 'src/main/resources/*') $ClassDir -Recurse -Force
    $JarPath = Join-Path $DistDir 'eclassroom.jar'
    & jar --create --file $JarPath --main-class eclassroom.Main -C $ClassDir .
    if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
    Write-Host "Built: $JarPath"
} finally {
    Remove-Item -LiteralPath $ClassDir -Recurse -Force
}
