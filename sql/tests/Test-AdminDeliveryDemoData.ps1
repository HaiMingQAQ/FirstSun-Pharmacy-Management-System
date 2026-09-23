# Requires Docker Desktop with the mysql:8.0 image. Uses no host port or named volume.
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$name = 'firstsun-admin-delivery-pr48-sql-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$seed = Join-Path ([System.IO.Path]::GetTempPath()) $name
$password = 'pr48-synthetic-only'

# Docker/MySQL 会把无害的弃用提示写到 stderr。在 Stop 模式下直接 `& docker ... 2>&1` 会把
# 这些 stderr 行当成终止错误。这里临时切换为 Continue、合并 stderr，并保留真实退出码。
function Invoke-Docker {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $lines = & docker @args 2>&1
        $script:dockerExitCode = $LASTEXITCODE
        $result = @()
        foreach ($line in $lines) { $result += [string]$line }
        return $result
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Invoke-MySql([string]$database, [string]$statement, [bool]$expectFailure = $false) {
    $output = Invoke-Docker exec -e "MYSQL_PWD=$password" $name mysql -h 127.0.0.1 -uroot $database -N -e $statement
    $code = $script:dockerExitCode
    if ($expectFailure) {
        if ($code -eq 0) { throw "Expected SQL failure in $database" }
    } elseif ($code -ne 0) {
        throw "SQL failed in ${database}: $output"
    }
    return $output
}

function Assert-Value([string]$database, [string]$query, [string]$expected) {
    $actual = (Invoke-MySql $database $query).Trim()
    if ($actual -ne $expected) { throw "Unexpected result in ${database}: expected $expected, got $actual" }
}

try {
    New-Item -ItemType Directory -Path $seed | Out-Null
    $mounts = Get-Content (Join-Path $repo 'deploy\docker-compose.prod.yml')
    foreach ($line in $mounts) {
        if ($line -match '^\s+- \.\./([^:]+):/docker-entrypoint-initdb.d/(\d\d-[^:]+):ro' -and
            $Matches[2] -notlike '38-*') {
            Copy-Item -LiteralPath (Join-Path $repo $Matches[1]) -Destination (Join-Path $seed $Matches[2])
        }
    }
    if ((Get-ChildItem $seed -File).Count -ne 37) { throw 'Expected exactly 37 prerequisite init SQL files' }
    $runOut = Invoke-Docker run -d --rm --memory 768m --name $name `
        --mount "type=bind,source=$seed,target=/docker-entrypoint-initdb.d,readonly" `
        -e "MYSQL_ROOT_PASSWORD=$password" -e MYSQL_DATABASE=firstsun_pr48 `
        mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci `
        --innodb-buffer-pool-size=192M --performance-schema=OFF
    if ($script:dockerExitCode -ne 0) { throw "Independent MySQL container did not start: $runOut" }
    $cpOut = Invoke-Docker cp (Join-Path $repo 'sql\migrations\20260922_p_admin_delivery_demo_data.sql') "${name}:/tmp/38.sql"
    if ($script:dockerExitCode -ne 0) { throw "Could not copy migration into independent MySQL container: $cpOut" }
    $ready = $false
    for ($i = 0; $i -lt 120; $i++) {
        $logs = (Invoke-Docker logs $name) -join "`n"
        if ($logs -match 'MySQL init process done') { $ready = $true; break }
        if ($logs -match 'ERROR.*at line') { throw 'Prerequisite initialization failed' }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'Timed out waiting for prerequisite initialization' }
    # "init process done" 后 entrypoint 会重启正式 server，等待其接受 TCP 连接。
    $serverReady = $false
    for ($i = 0; $i -lt 60; $i++) {
        $ping = Invoke-Docker exec -e "MYSQL_PWD=$password" $name mysqladmin -h 127.0.0.1 -uroot ping
        if ($script:dockerExitCode -eq 0 -and ("$ping" -match 'mysqld is alive')) { $serverReady = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $serverReady) { throw 'Timed out waiting for MySQL server to accept connections' }

    # 用独立调用创建目标库：避免在 sh -c 字符串内嵌双引号（Windows 传参会破坏内部引号）。
    Invoke-MySql 'firstsun_pr48' 'CREATE DATABASE pr48_pk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci' | Out-Null
    Invoke-MySql 'firstsun_pr48' 'CREATE DATABASE pr48_key CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci' | Out-Null
    Invoke-MySql 'firstsun_pr48' 'CREATE DATABASE pr48_missing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci' | Out-Null
    # 通过容器内管道克隆（单引号字符串内不含双引号；Linux 下 -h127.0.0.1 粘连写法可靠）。
    $clonePk = Invoke-Docker exec -e "MYSQL_PWD=$password" $name sh -c 'mysqldump -h127.0.0.1 -uroot --single-transaction firstsun_pr48 | mysql -h127.0.0.1 -uroot pr48_pk'
    if ($script:dockerExitCode -ne 0) { throw "Could not clone pr48_pk: $clonePk" }
    $cloneKey = Invoke-Docker exec -e "MYSQL_PWD=$password" $name sh -c 'mysqldump -h127.0.0.1 -uroot --single-transaction firstsun_pr48 | mysql -h127.0.0.1 -uroot pr48_key'
    if ($script:dockerExitCode -ne 0) { throw "Could not clone pr48_key: $cloneKey" }
    $cloneMissing = Invoke-Docker exec -e "MYSQL_PWD=$password" $name sh -c 'mysqldump -h127.0.0.1 -uroot --single-transaction firstsun_pr48 | mysql -h127.0.0.1 -uroot pr48_missing'
    if ($script:dockerExitCode -ne 0) { throw "Could not clone pr48_missing: $cloneMissing" }

    # 前置缺失场景：删除被引用的供应商 163301（此时尚未执行第 38）
    Invoke-MySql 'pr48_missing' 'DELETE FROM ph_supplier WHERE id=163301' | Out-Null

    Invoke-MySql 'pr48_pk' "INSERT INTO ph_category(id,cat_code,cat_name,tenant_id) VALUES (163006,'OTHER-TENANT','synthetic collision',999)" | Out-Null
    Invoke-MySql 'pr48_key' "INSERT INTO ph_category(id,cat_code,cat_name,tenant_id) VALUES (999999,'FS-CAT-RX','synthetic collision',999)" | Out-Null
    Invoke-MySql 'firstsun_pr48' 'source /tmp/38.sql' | Out-Null
    Assert-Value 'firstsun_pr48' 'SELECT COUNT(*) FROM ph_drug WHERE tenant_id=163' '20'
    Invoke-MySql 'firstsun_pr48' 'UPDATE ph_inv_batch SET qty_total=qty_total-1,qty_avail=qty_avail-1 WHERE id=163506' | Out-Null
    $quantity = (Invoke-MySql 'firstsun_pr48' 'SELECT qty_avail FROM ph_inv_batch WHERE id=163506').Trim()
    Invoke-MySql 'firstsun_pr48' 'source /tmp/38.sql' | Out-Null
    Assert-Value 'firstsun_pr48' 'SELECT COUNT(*) FROM ph_drug WHERE tenant_id=163' '20'
    Assert-Value 'firstsun_pr48' 'SELECT qty_avail FROM ph_inv_batch WHERE id=163506' $quantity
    Invoke-MySql 'firstsun_pr48' 'UPDATE ph_drug SET category_id=163005 WHERE id=163106' | Out-Null
    Invoke-MySql 'firstsun_pr48' 'source /tmp/38.sql' $true | Out-Null
    Assert-Value 'firstsun_pr48' 'SELECT category_id FROM ph_drug WHERE id=163106' '163005'
    Assert-Value 'firstsun_pr48' 'SELECT qty_avail FROM ph_inv_batch WHERE id=163506' $quantity

    Invoke-MySql 'pr48_pk' 'source /tmp/38.sql' $true | Out-Null
    Assert-Value 'pr48_pk' 'SELECT tenant_id FROM ph_category WHERE id=163006' '999'
    Assert-Value 'pr48_pk' 'SELECT COUNT(*) FROM ph_category WHERE id=163007' '0'
    Assert-Value 'pr48_pk' 'SELECT COUNT(*) FROM ph_drug WHERE id=163106' '0'
    Invoke-MySql 'pr48_key' 'source /tmp/38.sql' $true | Out-Null
    Assert-Value 'pr48_key' "SELECT id FROM ph_category WHERE cat_code='FS-CAT-RX'" '999999'
    Assert-Value 'pr48_key' 'SELECT COUNT(*) FROM ph_category WHERE id=163006' '0'
    Assert-Value 'pr48_key' 'SELECT COUNT(*) FROM ph_drug WHERE id=163106' '0'

    # 前置依赖缺失：删除供应商 163301 后，迁移必须在写入任何数据前失败回滚，
    # 不留守卫行、不产生指向空记录的孤儿药品。
    Invoke-MySql 'pr48_missing' 'source /tmp/38.sql' $true | Out-Null
    Assert-Value 'pr48_missing' 'SELECT COUNT(*) FROM ph_category WHERE id=163999' '0'
    Assert-Value 'pr48_missing' 'SELECT COUNT(*) FROM ph_drug WHERE id=163106' '0'
    Assert-Value 'pr48_missing' 'SELECT COUNT(*) FROM ph_drug WHERE id>=163106' '0'
    Write-Output 'PASS: fresh migration, repeat without state reset, mismatched reference, cross-tenant key conflicts and missing prerequisites all fail or roll back safely'
} finally {
    $container = Invoke-Docker ps -a --filter "name=^/${name}$" --format '{{.Names}}'
    if ($container -eq $name) { Invoke-Docker stop $name | Out-Null }
    if (Test-Path -LiteralPath $seed) { Remove-Item -LiteralPath $seed -Recurse -Force }
}
