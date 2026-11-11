import os
import PhotosUI
import Shared
import SwiftUI

/// The AI Movie Search chat, on the shared `AiChatViewModel` (ai-chat; add-ios-chat-and-google-signin
/// design, decisions 1 to 3). Pushed from Profile with the tab bar hidden. The ViewModel lives as
/// long as this screen, so leaving and reopening it starts over with the greeting (ai-chat
/// "Conversation is not persisted").
struct AiChatView: View {
    let graph: MovieGalleryGraph

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<AiChatViewModel, ChatUiState>({ graph.aiChatViewModel() }, state: { $0.uiState })
        }) { model in
            AiChatContent(model: model)
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                HStack(spacing: 8) {
                    Image(systemName: "sparkles").foregroundStyle(.tint)
                    VStack(alignment: .leading, spacing: 0) {
                        Text("AI Movie Search").font(.headline)
                        Text("Ask for a recommendation").font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
        }
        .toolbar(.hidden, for: .tabBar)
    }
}

private struct AiChatContent: View {
    let model: ObservedViewModel<AiChatViewModel, ChatUiState>

    /// Local, forwarded on Send: binding the field to `state` would drop keystrokes (see LoginView).
    @State private var input = ""
    @State private var showsPosterSource = false
    @State private var showsPhotoPicker = false
    @State private var showsCamera = false
    @State private var pickedItem: PhotosPickerItem?
    /// A camera or picker problem is the screen's own, not a failed reply, so it isn't in
    /// `ChatUiState.error`, but it shows in the same row and takes precedence, as on Android.
    @State private var captureError: String?

    private let store = PosterFileStore.standard()
    private static let typingId = "typing"

    private var state: ChatUiState { model.state }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(state.messages, id: \.id) { message in
                        ChatBubble(message: message).id(AnyHashable(message.id))
                    }
                    if state.isSending {
                        TypingBubble(label: ChatCopyKt.chatTypingLabel(isIdentifyingPoster: state.isIdentifyingPoster))
                            .id(AnyHashable(Self.typingId))
                    }
                }
                .padding(16)
            }
            .defaultScrollAnchor(.bottom)
            .scrollDismissesKeyboard(.interactively)
            .onChange(of: lastItemId) { _, id in
                withAnimation { proxy.scrollTo(id, anchor: .bottom) }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardDidShowNotification)) { _ in
                withAnimation { proxy.scrollTo(lastItemId, anchor: .bottom) }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: 0) {
                if let message = captureError ?? state.error.map({ ChatCopyKt.chatErrorMessage($0) }) {
                    ChatErrorRow(
                        message: message,
                        // Nothing to retry when the camera can't be opened in the first place.
                        onRetry: captureError == nil ? { model.viewModel.retry() } : nil,
                        onDismiss: {
                            if captureError != nil { captureError = nil } else { model.viewModel.dismissError() }
                        }
                    )
                }
                Divider()
                ChatInputBar(
                    text: $input,
                    enabled: !state.isSending,
                    showsPosterSource: $showsPosterSource,
                    // Both start a new attempt, which makes an earlier capture problem history.
                    onAttach: {
                        captureError = nil
                        showsPosterSource = true
                    },
                    onTakePhoto: openCamera,
                    onChooseFromGallery: { showsPhotoPicker = true },
                    onSend: {
                        captureError = nil
                        model.viewModel.sendMessage(text: input)
                        input = ""
                    }
                )
            }
            .background(.bar)
        }
        .photosPicker(isPresented: $showsPhotoPicker, selection: $pickedItem, matching: .images)
        .onChange(of: pickedItem) { _, item in
            guard let item else { return }
            pickedItem = nil
            Task {
                guard let data = try? await item.loadTransferable(type: Data.self) else { return }
                send { try store.save(data) }
            }
        }
        .fullScreenCover(isPresented: $showsCamera) {
            CameraPicker { image in
                showsCamera = false
                if let image { send { try store.save(image) } }
            }
            .ignoresSafeArea()
        }
        .task {
            let store = store
            await Task.detached(priority: .utility) { store.prune() }.value
        }
    }

    /// The id of the bottom item, so a new message or the typing bubble scrolls into view.
    private var lastItemId: AnyHashable? {
        if state.isSending { return AnyHashable(Self.typingId) }
        return state.messages.last.map { AnyHashable($0.id) }
    }

    private func openCamera() {
        Task {
            if await CameraAccess.resolve() {
                showsCamera = true
            } else {
                captureError = cameraAccessOffMessage
            }
        }
    }

    /// Writes the image and sends its URL. Caches is always writable, so a failed write is only
    /// logged; there is nothing the user could do about it.
    private func send(_ write: () throws -> String) {
        do {
            model.viewModel.sendPoster(imageUri: try write())
        } catch {
            Logger(subsystem: "com.raikar.moviegallery", category: "Chat").error("Couldn't save the poster: \(String(describing: error), privacy: .public)")
        }
    }
}

private struct ChatBubble: View {
    let message: ChatMessage

    /// Keeps long replies readable without stretching one-line replies to match.
    private static let maxWidth: CGFloat = 280

    var body: some View {
        HStack {
            if message.isUser { Spacer(minLength: 40) }
            if let uri = message.imageUri {
                PosterBubble(imageUri: uri)
            } else {
                Text(message.text)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .foregroundStyle(message.isUser ? Color.white : Color.primary)
                    .background(
                        message.isUser ? AnyShapeStyle(.tint) : AnyShapeStyle(Color(.secondarySystemBackground)),
                        in: RoundedRectangle(cornerRadius: 18)
                    )
                    .frame(maxWidth: Self.maxWidth, alignment: message.isUser ? .trailing : .leading)
            }
            if !message.isUser { Spacer(minLength: 40) }
        }
    }
}

/// The poster the user sent, at Android's 120 pt width and poster proportions.
private struct PosterBubble: View {
    let imageUri: String

    var body: some View {
        let image = URL(string: imageUri).flatMap { UIImage(contentsOfFile: $0.path) }
        ZStack {
            Color(.secondarySystemBackground)
            if let image {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Image(systemName: "photo").foregroundStyle(.secondary)
            }
        }
        .frame(width: 120, height: 180)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityLabel("Poster you sent")
    }
}

private struct TypingBubble: View {
    let label: String?

    var body: some View {
        HStack {
            HStack(spacing: 8) {
                ProgressView()
                if let label {
                    Text(label).font(.subheadline).foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 18))
            Spacer()
        }
    }
}

private struct ChatErrorRow: View {
    let message: String
    let onRetry: (() -> Void)?
    let onDismiss: () -> Void

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(message)
                .font(.footnote)
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let onRetry {
                Button("Retry", action: onRetry).font(.footnote.weight(.semibold))
            }
            Button("Dismiss", action: onDismiss).font(.footnote)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }
}

private struct ChatInputBar: View {
    @Binding var text: String
    let enabled: Bool
    @Binding var showsPosterSource: Bool
    let onAttach: () -> Void
    let onTakePhoto: () -> Void
    let onChooseFromGallery: () -> Void
    let onSend: () -> Void

    var body: some View {
        HStack(alignment: .bottom, spacing: 10) {
            Button(action: onAttach) {
                Image(systemName: "photo.badge.plus").font(.title3)
            }
            .accessibilityLabel("Add a movie poster")
            // On the button, so the iOS 26 popover points at it rather than at the middle of the screen.
            .confirmationDialog("Add a movie poster", isPresented: $showsPosterSource, titleVisibility: .visible) {
                if CameraAccess.isCameraAvailable {
                    Button("Take Photo", action: onTakePhoto)
                }
                Button("Choose from Gallery", action: onChooseFromGallery)
                Button("Cancel", role: .cancel) {}
            }
            TextField("Message AI Movie Search…", text: $text, axis: .vertical)
                .lineLimit(1...4)
                .textFieldStyle(.roundedBorder)
                .submitLabel(.send)
                .onSubmit(onSend)
            Button(action: onSend) {
                Image(systemName: "arrow.up.circle.fill").font(.title2)
            }
            .accessibilityLabel("Send")
        }
        .disabled(!enabled)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }
}
