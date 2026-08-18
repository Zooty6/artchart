[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArguments
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $projectRoot '.env'

if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    Write-Error "The required .env file was not found in the project root: $projectRoot"
    exit 1
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()

    if ($line -and -not $line.StartsWith('#')) {
        if ($line -match '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
            $name = $Matches[1]
            $value = $Matches[2].Trim()

            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'mvnw.cmd') spring-boot:run @MavenArguments
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
