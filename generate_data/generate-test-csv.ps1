# Generates a large shipments CSV for import testing.
# Usage:  .\generate-test-csv.ps1 -Rows 1000000 -Users 10 -OutFile .\test-data\shipments.csv
param(
    [int]$Rows = 1000000,
    [int]$Users = 10,
    [string]$Prefix = "TRACK",
    [string]$OutFile = ".\test-data\shipments.csv"
)

$dir = Split-Path -Parent $OutFile
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }

$statuses = @('CREATED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED')
$baseDate = Get-Date '2025-01-01 00:00:00'

$writer = [System.IO.StreamWriter]::new($OutFile, $false, [System.Text.UTF8Encoding]::new($false))
try {
    $writer.WriteLine('tracking_number,description,current_status,customer_username,created_at')
    for ($i = 1; $i -le $Rows; $i++) {
        $tracking = '{0}{1:D9}' -f $Prefix, $i
        $desc = 'Package number {0}' -f $i
        $status = $statuses[$i % $statuses.Length]
        $user = 'user{0}' -f (($i % $Users) + 1)
        $created = $baseDate.AddSeconds($i).ToString('yyyy-MM-dd HH:mm:ss')
        $writer.WriteLine("$tracking,$desc,$status,$user,$created")
    }
}
finally {
    $writer.Close()
}

$sizeMb = [math]::Round((Get-Item $OutFile).Length / 1MB, 1)
Write-Output "Done: $Rows rows -> $OutFile ($sizeMb MB)"
