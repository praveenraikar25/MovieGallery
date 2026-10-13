# Spec Delta

## Purpose

The AI Movie Search chat lets users ask a generative AI assistant for film recommendations in conversation, and identify a film from a photo of its poster.

## ADDED Requirements

### Requirement: Chat opens with a local greeting
The chat screen, titled "AI Movie Search", SHALL open with an assistant greeting inviting the user to describe what they're in the mood for or send a poster photo. The greeting SHALL NOT be sent to the assistant as part of the conversation.

#### Scenario: Opening chat
- **WHEN** the user taps "Find Movies with AI" on Profile
- **THEN** the chat shows only the greeting bubble

### Requirement: User can send a text message
The user SHALL be able to send a non-blank text message; it SHALL appear as a user bubble, the input SHALL clear, and the assistant's reply SHALL appear as an assistant bubble. Leading and trailing whitespace SHALL be trimmed, and the send action SHALL be disabled while the input is blank.

#### Scenario: Asking for a recommendation
- **WHEN** the user sends "a feel-good sci-fi movie"
- **THEN** the message appears as a user bubble and the assistant's reply follows it

### Requirement: Replies use the whole conversation as context
Each request SHALL include every earlier user and assistant message in the current conversation, so follow-up questions are answered in context. A poster message SHALL be represented in that history by a placeholder noting a poster photo was sent.

#### Scenario: Follow-up question
- **WHEN** the user asks "something shorter?" after receiving recommendations
- **THEN** the assistant answers with the earlier exchange as context

### Requirement: Only one request is in flight at a time
While a reply is pending, the screen SHALL show a typing indicator and SHALL NOT accept another text message or poster.

#### Scenario: Waiting for a reply
- **WHEN** a message has been sent and no reply has arrived
- **THEN** a typing indicator is shown and the input is disabled

### Requirement: User can identify a movie from a poster image
The attach action SHALL offer "Choose from Gallery" and, on devices with a camera, "Take Photo". The chosen image SHALL appear as a user bubble, the typing indicator SHALL read "Identifying poster…", and the reply SHALL identify the film. No runtime permission SHALL be requested.

#### Scenario: Poster from gallery
- **WHEN** the user picks a poster image from the gallery
- **THEN** the image appears in the chat and the assistant replies with the film's title and year

#### Scenario: Cancelling the camera
- **WHEN** the user opens the camera and backs out without taking a photo
- **THEN** nothing is sent and no capture file is kept

#### Scenario: No capable app
- **WHEN** no app on the device can take a photo or pick an image
- **THEN** the chat shows "No app on this device can take a photo." (or "...pick an image.") with Dismiss and no Retry

### Requirement: Assistant stays within its role
The assistant SHALL recommend two or three specific films per reply with release years, in plain conversational text, SHALL decline non-film topics, and SHALL NOT claim to have changed the user's watchlist or to see in-app activity.

> Note: Enforced by the assistant's system instruction, not by app logic; behavior is best-effort.

#### Scenario: Asking to save a film
- **WHEN** the user asks the assistant to add a film to their watchlist
- **THEN** the reply explains it can only suggest films and points to the film's detail screen

### Requirement: Failures keep the message and offer retry
When a request fails, times out after 60 seconds, or returns no text, the user's message SHALL stay visible and an error row SHALL describe the failure with Retry and Dismiss. Retry SHALL resend the last user message (text or poster) once; Dismiss SHALL hide the error.

#### Scenario: Timeout then retry
- **WHEN** a request times out and the user taps Retry
- **THEN** the same message is sent again and only one copy of it remains in the conversation

#### Scenario: Setup problem
- **WHEN** the AI service rejects the request as unauthorized
- **THEN** the error explains that AI Logic must be enabled and the App Check debug token registered

### Requirement: Conversation is not persisted
The conversation SHALL last only while the chat screen is open; leaving and reopening it SHALL start a new conversation with only the greeting.

> Note: There is no chat history storage.

#### Scenario: Reopening chat
- **WHEN** the user leaves the chat and opens it again from Profile
- **THEN** only the greeting is shown
