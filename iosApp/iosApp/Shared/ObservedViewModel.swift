import Shared
import SwiftUI

/// One shared ViewModel and its observed state, for a SwiftUI view (add-ios-app-shell design,
/// decision 6). It owns the ViewModel through `ViewModelOwner` and watches its `StateFlow` through
/// `StateFlowWatcher`, so `state` changes on the main thread on every emission. When the view goes
/// away, `deinit` stops the watch and clears the ViewModel, which cancels its `viewModelScope`.
@Observable
@MainActor
final class ObservedViewModel<VM: Lifecycle_viewmodelViewModel, State: AnyObject> {
    @ObservationIgnored let viewModel: VM
    private(set) var state: State

    @ObservationIgnored private let owner: ViewModelOwner<VM>
    @ObservationIgnored private var handle: WatchHandle?

    init(_ create: @escaping () -> VM, state flow: (VM) -> any Kotlinx_coroutines_coreStateFlow) {
        let owner = ViewModelOwner(create: create)
        let viewModel = owner.viewModel
        let watcher = StateFlowWatcher<State>(flow: flow(viewModel))
        self.owner = owner
        self.viewModel = viewModel
        state = watcher.value
        // StateFlowWatcher calls back on the main thread (Dispatchers.Main).
        handle = watcher.watch { [weak self] value in
            MainActor.assumeIsolated { self?.state = value }
        }
    }

    deinit {
        handle?.cancel()
        owner.clear()
        #if DEBUG
            print("ObservedViewModel: cleared \(VM.self)")
        #endif
    }
}

/// A second `StateFlow` of a ViewModel that an `ObservedViewModel` already owns, such as Home's
/// `avatarInitial` (add-ios-browse-screens design, decision 3). It only watches: the ViewModel's
/// lifetime stays with its `ObservedViewModel`. `deinit` stops the watch.
@Observable
@MainActor
final class ObservedFlow<Value: AnyObject> {
    private(set) var value: Value

    @ObservationIgnored private var handle: WatchHandle?

    init(_ flow: any Kotlinx_coroutines_coreStateFlow) {
        let watcher = StateFlowWatcher<Value>(flow: flow)
        value = watcher.value
        handle = watcher.watch { [weak self] value in
            MainActor.assumeIsolated { self?.value = value }
        }
    }

    deinit {
        handle?.cancel()
    }
}

/// Creates a screen's `ObservedViewModel` once, when the view first appears, and keeps it for the
/// view's lifetime. Creating it in a view's initializer instead would build (and start loading)
/// a throwaway ViewModel every time SwiftUI re-creates the view struct.
struct ViewModelScope<VM: Lifecycle_viewmodelViewModel, State: AnyObject, Content: View>: View {
    let create: () -> ObservedViewModel<VM, State>
    @ViewBuilder let content: (ObservedViewModel<VM, State>) -> Content

    @SwiftUI.State private var model: ObservedViewModel<VM, State>?

    var body: some View {
        Group {
            if let model {
                content(model)
            } else {
                Color.clear
            }
        }
        .onAppear {
            if model == nil { model = create() }
        }
    }
}
