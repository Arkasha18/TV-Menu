# Signing keys (local only — never commit)

Place the production PKCS12 keystore here as `adminrunet-production.p12`.

Copy `signing.properties.example` to `signing.properties` and fill values.
`signing.properties` and `*.p12` are gitignored.

Build a signed release:

```bat
build.bat assembleRelease
```

APK: `TvQuickMenu\app\build\outputs\apk\release\app-release.apk`
