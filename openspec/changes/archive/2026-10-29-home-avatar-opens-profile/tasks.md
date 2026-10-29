## 1. UI

- [x] 1.1 `ui/components/InitialAvatar`: add optional `onClick: (() -> Unit)? = null`; when set, clip to circle and make clickable with `Role.Button` and an "Open profile" click label, with a 48dp touch target
- [x] 1.2 `ui/screens/home/HomeScreen`: add `onProfileClick: () -> Unit` and pass it to the header avatar
- [x] 1.3 `ui/navigation/MainScaffold`: extract the bottom bar's tab-switch block into `navigateToTab(route)`, use it for the bar, and pass `onProfileClick = { navigateToTab(Routes.PROFILE) }` to `HomeScreen`

## 2. Verification

- [x] 2.1 Run `./gradlew ktlintFormat` then `./gradlew ktlintCheck`
- [x] 2.2 Run `./gradlew test` (no new JVM tests: the scenarios are Compose/navigation behaviour, see design Risks)
- [x] 2.3 Run `./gradlew assembleDebug`
- [x] 2.4 Manually verify both scenarios on a device: avatar tap shows Profile with the Profile tab selected; system back then returns to Home; the bottom Profile tab still works
