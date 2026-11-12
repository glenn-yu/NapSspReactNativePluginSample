$ErrorActionPreference = "Stop"
# Compile the production registry without UIKit/React, then run lifecycle assertions.
# The .swift.txt extension keeps the harness out of the library's CocoaPods source glob.
$registryPath = Join-Path $PSScriptRoot "../NapSspFullScreenRegistry.swift"
$source = [System.IO.File]::ReadAllText($registryPath)
$source = $source.Substring(0, $source.IndexOf("/// Shared argument validation"))
$source = $source.Replace("import UIKit", "").Replace("import React", "")
$aliases = @"
typealias RCTPromiseResolveBlock = (Any?) -> Void
typealias RCTPromiseRejectBlock = (String?, String?, Error?) -> Void
"@
$fixture = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot "registry-lifecycle.swift.txt"))
$testDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("napssp-ios-registry-" + [guid]::NewGuid())
[System.IO.Directory]::CreateDirectory($testDirectory) | Out-Null
$testSource = Join-Path $testDirectory "main.swift"
[System.IO.File]::WriteAllText($testSource, $source + "`n" + $aliases + "`n" + $fixture)
$testExecutable = Join-Path $testDirectory "registry-test.exe"
& swiftc $testSource -o $testExecutable
if ($LASTEXITCODE -ne 0) { throw "Swift registry test compilation failed." }
& $testExecutable
if ($LASTEXITCODE -ne 0) { throw "Swift registry lifecycle assertions failed." }
