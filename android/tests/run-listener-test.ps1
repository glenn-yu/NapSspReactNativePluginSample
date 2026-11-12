param(
    [string]$GradleUserHome = $(if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }),
    [string]$KotlinVersion = '2.1.20',
    [string]$SdkVersion = '2.5.1'
)
$ErrorActionPreference = 'Stop'
$cache = Join-Path $GradleUserHome 'caches/modules-2/files-2.1'

function Find-CachedJar([string]$coordinate) {
    $artifact = Get-ChildItem (Join-Path $cache $coordinate) -Filter '*.jar' -Recurse |
        Where-Object Name -NotMatch 'sources|javadoc' | Select-Object -First 1
    if (!$artifact) { throw "Missing cached JAR: $coordinate" }
    return $artifact.FullName
}

$stdlib = Find-CachedJar "org.jetbrains.kotlin/kotlin-stdlib/$KotlinVersion"
$compilerClasspath = @(
    (Find-CachedJar "org.jetbrains.kotlin/kotlin-compiler-embeddable/$KotlinVersion"),
    $stdlib,
    (Find-CachedJar "org.jetbrains.kotlin/kotlin-reflect/$KotlinVersion"),
    (Find-CachedJar "org.jetbrains.kotlin/kotlin-script-runtime/$KotlinVersion"),
    (Find-CachedJar 'org.jetbrains.intellij.deps/trove4j'),
    (Find-CachedJar 'org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm'),
    (Find-CachedJar 'org.jetbrains/annotations')
) -join [IO.Path]::PathSeparator
$sdk = Get-ChildItem (Join-Path $cache "io.github.nasmedia-tech/admixer-ssp/$SdkVersion") -Filter '*.aar' -Recurse | Select-Object -First 1
if (!$sdk) { throw "Missing cached AdMixer SDK $SdkVersion AAR" }

$output = Join-Path ([IO.Path]::GetTempPath()) ('nap-listener-test-' + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $output | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($sdk.FullName)
try {
    [IO.Compression.ZipFileExtensions]::ExtractToFile($archive.GetEntry('classes.jar'), (Join-Path $output 'sdk.jar'))
} finally { $archive.Dispose() }

$classpath = @((Join-Path $output 'sdk.jar'), $stdlib) -join [IO.Path]::PathSeparator
$testJar = Join-Path $output 'listener-test.jar'
$source = Join-Path $PSScriptRoot '../src/main/java/com/nasmedia/admixerssp/NapAdListener.kt'
& java -cp $compilerClasspath org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -classpath $classpath -d $testJar $source (Join-Path $PSScriptRoot 'ListenerTest.kt')
if ($LASTEXITCODE -ne 0) { throw "Listener compilation failed ($LASTEXITCODE)" }
& java -cp ($testJar + [IO.Path]::PathSeparator + $classpath) com.nasmedia.admixerssp.ListenerTestKt
if ($LASTEXITCODE -ne 0) { throw "Listener test failed ($LASTEXITCODE)" }
Write-Output "Test artifacts: $output"
