# Personal settings (offline, single user)

Implemented 2026-11-16.

- Persistent top-right gear/avatar in MainActivity header, accessible from Overview, Devices, and Export; removes the large profile button from Devices.
- Avatar is the initial of the optional local display name (or a neutral smiley), not a photo and not a login/account.
- Scrollable local profile dialog, with editable display name, optional sex/gender selection, birth year, age, height cm, weight kg, daily step goal, wrist side, preferred units, sleep target, manually entered resting HR, and note.
- Storage: app-private Android SharedPreferences `local_personal_profile` (MODE_PRIVATE). No network, account, sync, or profile data in Git.
- Save, cancel, and separately confirmed deletion of profile preferences; deletion does not touch recorded health readings.
- Numeric input validation. Existing height/weight/age/gender preference keys retained for compatibility.
- Preferences are **not yet wired to derived health metrics**, and imperial unit conversions are not implemented. Birth year and age are separate optional values and not auto-synchronized. Do not present estimates as validated medical facts.
- No user photo or cloud login; intentionally privacy-first.
- Implementation: `PersonalSettings.kt`, `MainActivity.kt`.
- CI must pass before distribution; physical device UI verification remains outstanding.
