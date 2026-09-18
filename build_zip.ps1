Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$moduleDir = "d:\1\Code\Github\magisk-module"
$outputZip = "$moduleDir\force_sim_network_mode_v1.0.zip"

# Remove old zip if exists
if (Test-Path $outputZip) { Remove-Item $outputZip -Force }

# Files to include in ZIP (relative path in zip -> actual file path)
$files = @(
    @{ ZipPath = "module.prop";    FilePath = "$moduleDir\module.prop" },
    @{ ZipPath = "service.sh";     FilePath = "$moduleDir\service.sh" },
    @{ ZipPath = "customize.sh";   FilePath = "$moduleDir\customize.sh" },
    @{ ZipPath = "META-INF/com/google/android/update-binary";   FilePath = "$moduleDir\META-INF\com\google\android\update-binary" },
    @{ ZipPath = "META-INF/com/google/android/updater-script";  FilePath = "$moduleDir\META-INF\com\google\android\updater-script" }
)

# Create ZIP stream
$zipStream = [System.IO.File]::Open($outputZip, [System.IO.FileMode]::Create)
$zip = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)

foreach ($f in $files) {
    Write-Host "Adding: $($f.ZipPath)"
    $entry = $zip.CreateEntry($f.ZipPath, [System.IO.Compression.CompressionLevel]::Optimal)

    # Read file content and convert CRLF -> LF (critical for shell scripts on Android)
    $rawContent = [System.IO.File]::ReadAllText($f.FilePath)
    $unixContent = $rawContent.Replace("`r`n", "`n").Replace("`r", "`n")
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($unixContent)

    $entryStream = $entry.Open()
    $entryStream.Write($bytes, 0, $bytes.Length)
    $entryStream.Close()
}

$zip.Dispose()
$zipStream.Close()

Write-Host ""
Write-Host "Done! ZIP created at: $outputZip"
Write-Host "Size: $((Get-Item $outputZip).Length) bytes"

# Verify ZIP contents
Write-Host ""
Write-Host "=== ZIP Contents ==="
$verifyStream = [System.IO.File]::OpenRead($outputZip)
$verifyZip = New-Object System.IO.Compression.ZipArchive($verifyStream, [System.IO.Compression.ZipArchiveMode]::Read)
foreach ($e in $verifyZip.Entries) {
    Write-Host "  $($e.FullName) ($($e.Length) bytes)"
}
$verifyZip.Dispose()
$verifyStream.Close()
