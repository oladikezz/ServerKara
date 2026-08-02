param(
    [Parameter(Mandatory = $true)]
    [int]$TargetProcessId
)

Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class ConsoleSignal
{
    public delegate bool HandlerRoutine(uint signal);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool FreeConsole();

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool AttachConsole(uint processId);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool GenerateConsoleCtrlEvent(uint ctrlEvent, uint processGroupId);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool SetConsoleCtrlHandler(HandlerRoutine handler, bool add);
}
"@

[ConsoleSignal]::SetConsoleCtrlHandler($null, $true) | Out-Null
[ConsoleSignal]::FreeConsole() | Out-Null

if (-not [ConsoleSignal]::AttachConsole([uint32]$TargetProcessId)) {
    exit 2
}

$sent = [ConsoleSignal]::GenerateConsoleCtrlEvent(0, 0)
Start-Sleep -Milliseconds 500
[ConsoleSignal]::FreeConsole() | Out-Null

if (-not $sent) {
    exit 3
}
