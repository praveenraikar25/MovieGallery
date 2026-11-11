## MODIFIED Requirements

### Requirement: User can identify a movie from a poster image
The attach action SHALL offer "Choose from Gallery" and, on devices with a camera, "Take Photo". The chosen image SHALL appear as a user bubble, the typing indicator SHALL read "Identifying poster…", and the reply SHALL identify the film. On Android no runtime permission SHALL be requested. On iOS, choosing from the gallery SHALL NOT ask for any permission, and "Take Photo" SHALL ask for camera access the first time, as the system requires.

#### Scenario: Poster from gallery
- **WHEN** the user picks a poster image from the gallery
- **THEN** the image appears in the chat and the assistant replies with the film's title and year

#### Scenario: Cancelling the camera
- **WHEN** the user opens the camera and backs out without taking a photo
- **THEN** nothing is sent and no capture file is kept

#### Scenario: Cancelling the gallery
- **WHEN** the user opens the gallery and closes it without picking an image
- **THEN** nothing is sent and no file is kept

#### Scenario: No capable app
- **WHEN** no app on the device can take a photo or pick an image
- **THEN** the chat shows "No app on this device can take a photo." (or "...pick an image.") with Dismiss and no Retry

#### Scenario: Device without a camera
- **WHEN** the user opens the attach action on a device with no camera
- **THEN** only "Choose from Gallery" is offered

#### Scenario: Camera access denied on iOS
- **WHEN** the user taps "Take Photo" on iOS after declining camera access
- **THEN** the camera does not open
- **AND** the chat shows "Camera access is off. Turn it on in Settings to take a photo." with Dismiss and no Retry
