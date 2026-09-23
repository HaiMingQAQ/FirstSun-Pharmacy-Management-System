# Requires Docker Desktop with the mysql:8.0 image. Uses no host port or named volume.
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$name = 'firstsun-admin-delivery-pr48-sql-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$seed = Join-Path ([System.IO.Path]::GetTempPath()) $name
$password = 'pr48-synthetic-only'

function Invoke-MySql([string]$database, [string]$statement, [bool]$expectFailure = $false) {
    $output = & docker exec -e "MYSQL_PWD=$password" $name mysql -uroot $database -N -e $statement 2>&1
    $code = $LASTEXITCODE
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
    & docker run -d --rm --memory 768m --name $name --mount "type=bind,source=$seed,target=/docker-entrypoint-initdb.d,readonly" `
        -e "MYSQL_ROOT_PASSWORD=$password" -e MYSQL_DATABASE=firstsun_pr48 `
        mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci `
        --innodb-buffer-pool-size=192M --performance-schema=OFF | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Independent MySQL container did not start' }
    & docker cp (Join-Path $repo 'sql\migrations\20260922_p_admin_delivery_demo_data.sql') "${name}:/tmp/38.sql" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy migration into independent MySQL container' }
    $ready = $false
    for ($i = 0; $i -lt 120; $i++) {
        $logs = & docker logs $name 2>&1
        if ($logs -match 'MySQL init process done') { $ready = $true; break }
        if ($logs -match 'ERROR.*at line') { throw 'Prerequisite initialization failed' }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'Timed out waiting for prerequisite initialization' }

    & docker exec -e "MYSQL_PWD=$password" $name sh -c `
        'mysql -uroot -e "CREATE DATABASE pr48_pk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE pr48_key CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" && mysqldump -uroot --single-transaction firstsun_pr48 | mysql -uroot pr48_pk && mysqldump -uroot --single-transaction firstsun_pr48 | mysql -uroot pr48_key' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not clone prerequisite database' }

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
    Write-Output 'PASS: fresh migration, repeat without state reset, mismatched reference and cross-tenant key conflicts roll back'
} finally {
    $container = & docker ps -a --filter "name=^/${name}$" --format '{{.Names}}' 2>$null
    if ($container -eq $name) { & docker stop $name | Out-Null }
    if (Test-Path -LiteralPath $seed) { Remove-Item -LiteralPath $seed -Recurse -Force }
}
