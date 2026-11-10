<#
.SYNOPSIS
    Opens or adds a website shortcut to TV Bro on the Fire TV.
.EXAMPLE
    .\addsite.ps1 "Twitch" "https://twitch.tv"
#>
param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Name,

    [Parameter(Mandatory=$true, Position=1)]
    [string]$Url
)

$adb = Join-Path $PSScriptRoot "tools\platform-tools\adb.exe"
$device = "10.0.0.51:5555"

# Ensure URL has protocol
if (-not ($Url.StartsWith("http://") -or $Url.StartsWith("https://"))) {
    $Url = "https://" + $Url
}

Write-Host "Connecting to Fire TV at $device..." -ForegroundColor Cyan
& $adb connect $device | Out-Null

Write-Host "Opening '$Name' ($Url) in TV Bro on Fire TV..." -ForegroundColor Green
& $adb -s $device shell am start -n com.phlox.tvwebbrowser/.activity.main.MainActivity -a android.intent.action.VIEW -d "`"$Url`""

Write-Host "`nWebsite '$Name' is now open on your TV in TV Bro!" -ForegroundColor Green
Write-Host "To save it permanently to your TV Bro visual home tiles with the remote:" -ForegroundColor Yellow
Write-Host "1. Press the Menu button (3 horizontal lines) on your Fire TV remote." -ForegroundColor Yellow
Write-Host "2. Click the Star (Bookmark) icon." -ForegroundColor Yellow
Write-Host "Done! It will now appear on your home grid every time you open TV Bro." -ForegroundColor Yellow

