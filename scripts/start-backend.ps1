# AI-assisted: PowerShell equivalent; refuses to run without an external JWT key.
$ErrorActionPreference = 'Stop'
if (-not $env:JWT_SECRET) { throw '请先设置 JWT_SECRET，至少32字节' }
Set-Location (Join-Path $PSScriptRoot '../backend')
mvn verify
if ($LASTEXITCODE -ne 0) { throw '后端测试或构建失败' }
java -jar target/dorm-repair-backend-1.0.0.jar
