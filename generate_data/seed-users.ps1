# Seeds users through the backend /auth/register endpoint so passwords are
# properly BCrypt-hashed and the accounts can actually log in.
# Requires the backend to be running.
# Usage:  .\seed-users.ps1 -ApiUrl http://localhost:8080 -Users 10
param(
    [string]$ApiUrl = "http://localhost:8080",
    [int]$Users = 10
)

function Register($user) {
    try {
        Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/register" `
            -ContentType "application/json" -Body ($user | ConvertTo-Json) | Out-Null
        Write-Output "created: $($user.username) ($($user.role))"
    }
    catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq 409) { Write-Output "exists:  $($user.username)" }
        else { Write-Output "FAILED:  $($user.username) -> $code" }
    }
}

Register @{ username = "admin";   password = "admin123";    firstName = "Admin";   lastName = "Admin";   email = "admin@example.com";   role = "admin" }
Register @{ username = "courier"; password = "courier123";  firstName = "Courier"; lastName = "One";     email = "courier@example.com"; role = "courier" }

for ($i = 1; $i -le $Users; $i++) {
    Register @{ username = "user$i"; password = "password123"; firstName = "Test"; lastName = "User$i"; email = "user$i@example.com"; role = "user" }
}
