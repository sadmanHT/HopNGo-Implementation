# Maven Installation Script
# Run this script after downloading apache-maven-3.9.11-bin.zip to D:\softwares\

$mavenZip = "D:\softwares\apache-maven-3.9.11-bin.zip"
$installDir = "D:\softwares"
$mavenHome = "D:\softwares\apache-maven-3.9.11"

Write-Host "Checking for Maven zip file..." -ForegroundColor Green

if (Test-Path $mavenZip) {
    Write-Host "Found Maven zip file. Extracting..." -ForegroundColor Green
    
    # Extract the zip file
    Expand-Archive -Path $mavenZip -DestinationPath $installDir -Force
    
    Write-Host "Maven extracted successfully to $mavenHome" -ForegroundColor Green
    
    # Add Maven to PATH
    $mavenBin = "$mavenHome\bin"
    $currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
    
    if ($currentPath -notlike "*$mavenBin*") {
        Write-Host "Adding Maven to PATH..." -ForegroundColor Green
        $newPath = "$currentPath;$mavenBin"
        [Environment]::SetEnvironmentVariable("PATH", $newPath, "User")
        
        # Also set MAVEN_HOME
        [Environment]::SetEnvironmentVariable("MAVEN_HOME", $mavenHome, "User")
        
        Write-Host "Maven has been added to PATH and MAVEN_HOME has been set." -ForegroundColor Green
        Write-Host "Please restart your terminal or IDE to use the new PATH." -ForegroundColor Yellow
        
        # Update current session PATH
        $env:PATH = "$env:PATH;$mavenBin"
        $env:MAVEN_HOME = $mavenHome
        
        Write-Host "Testing Maven installation..." -ForegroundColor Green
        & "$mavenBin\mvn" --version
        
    } else {
        Write-Host "Maven is already in PATH." -ForegroundColor Yellow
    }
    
} else {
    Write-Host "Maven zip file not found at $mavenZip" -ForegroundColor Red
    Write-Host "Please download apache-maven-3.9.11-bin.zip from:" -ForegroundColor Red
    Write-Host "https://maven.apache.org/download.cgi" -ForegroundColor Red
    Write-Host "And save it to D:\softwares\" -ForegroundColor Red
}

Write-Host "Press any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")