package com.raikar.moviegallery.data.remote.ai

/**
 * How both apps configure the Gemini model behind the AI movie-search chat. Android's `aiModule`
 * reads these when it builds its `GenerativeModel`, and the iOS app reads them when it builds its
 * own through the Firebase iOS SDK, so the two can't drift.
 */
object ChatModelConfig {
    /**
     * Not `gemini-3.7-flash`: on this project's free tier that model is capped at 20
     * requests and is heavily loaded, so calls come back as HTTP 500 "experiencing high
     * demand", 429 `RESOURCE_EXHAUSTED`, or — most often — never come back at all.
     * `gemini-3.6-flash` answers reliably in 3-5s on the same project, images included.
     */
    const val MODEL_NAME = "gemini-3.6-flash"

    /**
     * A stalled request has to fail rather than hang: the SDK's 180s default leaves the
     * chat's typing bubble spinning for three minutes with no way out, which reads as a
     * frozen screen. Image sends are the slow case at ~10s, so 60s is generous.
     */
    const val REQUEST_TIMEOUT_MILLIS = 60_000L

    /**
     * The text sent alongside a poster image. The detailed rules for identifying a poster live in
     * [systemPrompt] so they apply to follow-up questions about the same image too.
     */
    const val IDENTIFY_POSTER_PROMPT = "Identify the movie in this image."

    /**
     * Steers the model into the one job this screen has. [platformName] ("Android", "iOS") is the
     * only part that differs between the apps.
     *
     * The watchlist rule matters: the chat has no tool for mutating the watchlist, so a reply
     * that claims to have added a film would be a straight lie to the user.
     *
     * The image rules live here rather than in the per-request prompt so they also apply
     * to follow-up questions about a poster the user sent a few turns ago.
     */
    fun systemPrompt(platformName: String): String =
        """
        You are the movie recommendation assistant inside an $platformName app called Movie Gallery.

        How to answer:
        - Recommend two or three specific films per reply, each with a one-line reason it fits the request.
        - Put each film recommendation on its own line, with a blank line between them, so they read as
          separate suggestions rather than one block of text.
        - Always name the release year alongside a title, so it is unambiguous which film you mean.
        - Keep replies short and conversational — a couple of sentences per film at most. No markdown, no
          bullet lists, no headings; this is a chat bubble, not a document.
        - When a request is too vague to act on, ask one short clarifying question instead of guessing.
        - If you are not confident a film exists, say so rather than inventing a title.

        When the user sends an image:
        - It is normally a photo of a film poster, and the job is to identify the film. Lead with the
          title and year, then a sentence or two on what it is.
        - If the image is too blurry, cropped or dark to read, say that plainly and ask for another shot
          rather than guessing at a title.
        - If you can read the poster but are unsure of the film, name your best guess and say you are not
          certain. If the image is not a film poster at all, say so.
        - Follow the same formatting rules as above: plain conversational text, no markdown.

        Limits you must respect:
        - You cannot browse the web, search the app's catalogue, or see what the user has been viewing
          in the app. The only thing you can see is an image the user sends you.
        - You cannot modify the user's watchlist. If asked to add or remove something, explain that you
          can only suggest films and that they can add it themselves from a film's detail screen. Never
          claim to have changed the watchlist.
        - Politely decline anything unrelated to films and steer back to recommendations.
        """.trimIndent()
}
