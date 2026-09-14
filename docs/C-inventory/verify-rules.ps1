param(
    [Parameter(Mandatory = $true)]
    [string] $JdkHome
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$modulePath = Join-Path $repositoryRoot 'backend/yudao-module-pharmacy'
$rulePath = Join-Path $modulePath 'src/main/java/cn/iocoder/yudao/module/pharmacy/service/inventory/rule'
$checkPath = Join-Path $modulePath 'src/test/java/cn/iocoder/yudao/module/pharmacy/inventory/InventoryRulesCheck.java'
# A fresh output directory prevents a failed compile from running stale bytecode. target is ignored.
$outputPath = Join-Path $modulePath ('target/inventory-rules-' + [guid]::NewGuid().ToString('N'))
$compilerPath = Join-Path $JdkHome 'bin/javac.exe'
$runtimePath = Join-Path $JdkHome 'bin/java.exe'
if (!(Test-Path -LiteralPath $compilerPath) -or !(Test-Path -LiteralPath $runtimePath)) {
    throw 'JdkHome must identify a JDK with bin/javac.exe and bin/java.exe'
}
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
$sourcePaths = @(Get-ChildItem -LiteralPath $rulePath -Filter '*.java' | ForEach-Object { $_.FullName })
$sourcePaths += $checkPath
# main now standardizes Java 17 / Spring Boot 3. Keep the standalone pure-rule check aligned.
& $compilerPath --release 17 -encoding UTF-8 -Xlint:all -Werror -d $outputPath @sourcePaths
if ($LASTEXITCODE -ne 0) { throw "Rule compilation failed: $LASTEXITCODE" }
& $runtimePath -cp $outputPath cn.iocoder.yudao.module.pharmacy.inventory.InventoryRulesCheck
if ($LASTEXITCODE -ne 0) { throw "Rule checks failed: $LASTEXITCODE" }
