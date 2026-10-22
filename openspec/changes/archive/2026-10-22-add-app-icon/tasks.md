# Tasks

## 1. Icon artwork

- [x] 1.1 Rewrite `drawable/ic_launcher_background.xml` as a flat `#C96442` fill (remove the green and grid lines)
- [x] 1.2 Rewrite `drawable/ic_launcher_foreground.xml` with the 2x2 rounded poster tiles (`#FAF9F6`, `#F3D9CD`, `#F3D9CD`, `#1B1A17`) and a rounded `#E08A65` play triangle, inside the 66dp safe zone
- [x] 1.3 Add `drawable/ic_launcher_monochrome.xml`: the four tiles as solid shapes with the play mark cut out

## 2. Wiring

- [x] 2.1 Point the `<monochrome>` layer in `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` at `@drawable/ic_launcher_monochrome`
- [x] 2.2 Delete the legacy `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher*.webp` files (minSdk 29 uses the adaptive XML)

## 3. Verification

- [x] 3.1 Run `./gradlew assembleDebug` and confirm the build passes
- [ ] 3.2 Install on an emulator and check the icon in the drawer, under circle and squircle masks, and with themed icons on Android 13+
- [ ] 3.3 Confirm the stock green Android icon no longer appears anywhere
