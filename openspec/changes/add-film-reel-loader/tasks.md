# Tasks

## 1. Reel geometry and motion rules

- [x] 1.1 Add a pure helper (no Android or Compose imports) in `ui/components/` giving the six hole angles, hole and ring sizes as fractions of the reel size, and the 1600 ms turn duration. Verified by a JVM unit test: six holes, 60° apart, all inside the ring.
- [x] 1.2 Add a named reel size to `ui/theme/Dimens.kt` `Sizes`. Verified by it compiling and being used by the reel (`./gradlew assembleDebug`).

## 2. Film reel composable

- [x] 2.1 Add the reel composable drawn with `Canvas`: ring, six holes, hub, in `MaterialTheme.colorScheme.primary`. Verified by running the app and seeing the reel in light and dark themes (spec: follows the app theme).
- [x] 2.2 Rotate it continuously and linearly, one turn per 1600 ms. Verified by watching several turns with no slowdown or jump (spec: rotates continuously).
- [x] 2.3 Render the reel static when the system animator duration scale is 0. Verified by turning animations off in developer options and seeing a still reel, and by a JVM unit test of the rule that maps the scale to "animate or not".
- [x] 2.4 Announce the reel as "Loading". Verified with TalkBack reading "Loading".

## 3. Wire it in

- [x] 3.1 Replace the circular spinner inside `LoadingState` in `StateViews.kt` with the reel. Verified by Home, Movie Detail and Top TV Shows each showing the reel while loading (spec: full-screen loads show a film reel).
- [x] 3.2 Confirm the login, load-more and chat-bubble spinners are unchanged. Verified by searching for the remaining spinner uses and by opening those screens.

## 4. Quality gates

- [x] 4.1 Run `./gradlew ktlintCheck test` and fix anything it reports. Verified by both passing.
