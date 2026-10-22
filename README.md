# Roblox Rare Name Finder

A minimal dark-themed Android app that generates short Roblox username candidates and checks them in batches against Roblox's public username lookup endpoint.

## Features
- Generate 4–8 character candidates
- Letters, digits and underscores
- Batch lookup against `https://users.roblox.com/v1/usernames/users`
- Copy candidates to clipboard
- GitHub Actions APK build

## Build
Open this project in Android Studio or run:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Important limitations
This is a candidate finder, not a guarantee of username availability. The lookup endpoint returns existing accounts; a name absent from its response may still be invalid, restricted, reserved, or unavailable at signup. Always confirm directly with Roblox. Requests may be rate-limited; this app deliberately checks small batches and pauses between them. It does not use proxies or attempt to bypass Roblox limits.
