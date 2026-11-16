import SwiftUI

struct QuickSitSheet: View {
    @Binding var isPresented: Bool
    @ObservedObject private var engine = BellEngine.shared

    @State private var minutes: Int = 15
    @State private var secondsRemaining: Int = 0
    @State private var selectedBell = BellCatalog.defaultSound.id
    @State private var startChimes = 1
    @State private var endChimes = 1
    @State private var timer: Timer?
    /// The running sit, if any. Its bells are rung by BellEngine; this view
    /// only shows progress.
    @State private var running: TimingSession.QuickSitSession?
    /// Set when the sit reached its end on its own, so closing the sheet
    /// lets the end chimes finish instead of cancelling them.
    @State private var completed = false

    private var isRunning: Bool { running != nil }
    private var totalSeconds: Int { running?.durationSeconds ?? minutes * 60 }
    private let presetDurations = [5, 10, 15, 20, 30, 45, 60, 75, 90]

    var body: some View {
        NavigationStack {
            Group {
                if isRunning {
                    runningView
                } else {
                    setupForm
                }
            }
            .navigationTitle("Quick Sit")
            .toolbar {
                if isRunning {
                    ToolbarItem(placement: .cancellationAction) {
                        // Ends the sit; its end bell won't ring.
                        Button("Cancel", role: .destructive) {
                            stopTimer()
                        }
                    }
                } else {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") {
                            stopTimer()
                            isPresented = false
                        }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Start") {
                            startSession()
                        }
                    }
                }
            }
            .interactiveDismissDisabled(isRunning)
            .overlay(alignment: .bottomLeading) { BellEngineDebugStatus() }
            .onDisappear {
                // Leaving Quick Sit ends it: no more bells. A sit that just
                // completed keeps its end chimes.
                timer?.invalidate()
                timer = nil
                if isRunning && !completed {
                    engine.endSession()
                }
            }
            .onAppear {
                if case .quickSit(let s)? = engine.session {
                    // Resumed after iOS ended the app mid-sit.
                    attach(to: s)
                } else {
                    selectedBell = BellCatalog.defaultSound.id
                    startChimes = 1
                    endChimes = 1
                }
            }
        }
    }

    /// While the sit runs: a live countdown and progress ring, nothing to fiddle with.
    private var runningView: some View {
        VStack(spacing: 24) {
            BellWarningsView(engine: engine)
                .padding(.horizontal)
            Spacer(minLength: 0)
            ZStack {
                Circle()
                    .stroke(Color.accentColor.opacity(0.15), lineWidth: 14)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.linear(duration: 0.25), value: progress)
                VStack(spacing: 6) {
                    Text("Time remaining")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(TimeFormatting.formattedDuration(from: TimeInterval(secondsRemaining)))
                        .font(.system(size: 48, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)
                }
                .padding(28)
                .accessibilityElement(children: .combine)
            }
            .frame(maxWidth: 300, maxHeight: 300)
            .padding(.horizontal, 32)
            Spacer(minLength: 0)
        }
        .padding(.vertical)
    }

    private var setupForm: some View {
            Form {
                Section(header: Text("Duration")) {
                    VStack(alignment: .leading, spacing: 12) {
                        Stepper(value: $minutes, in: 1...180, step: 1) {
                            Text("^[\(minutes) minute](inflect: true)")
                                .font(.headline)
                        }
                        .disabled(isRunning)

                        LazyVGrid(columns: adjustmentColumns, alignment: .leading, spacing: 8) {
                            adjustButton(label: "−5", systemImage: "minus.circle.fill", delta: -5)
                            adjustButton(label: "−1", systemImage: "minus.circle", delta: -1)
                            adjustButton(label: "+1", systemImage: "plus.circle", delta: 1)
                            adjustButton(label: "+5", systemImage: "plus.circle.fill", delta: 5)
                        }

                        LazyVGrid(columns: quickDurationColumns, alignment: .leading, spacing: 12) {
                            ForEach(presetDurations, id: \.self) { preset in
                                presetButton(for: preset)
                            }
                        }
                    }
                }

                Section(header: Text("Bell")) {
                    Picker("Sound", selection: $selectedBell) {
                        ForEach(BellCatalog.sounds) { sound in
                            Text(sound.displayName).tag(sound.id)
                        }
                    }
                    .pickerStyle(.menu)
                    .disabled(isRunning)

                    Stepper(value: $startChimes, in: 1...12) {
                        Label("Start chimes: \(startChimes)", systemImage: "bell")
                    }
                    .disabled(isRunning)

                    Stepper(value: $endChimes, in: 1...12) {
                        Label("End chimes: \(endChimes)", systemImage: "bell")
                    }
                    .disabled(isRunning)
                }

            }
    }

    private func adjustMinutes(by delta: Int) {
        minutes = max(1, min(180, minutes + delta))
    }

    private var adjustmentColumns: [GridItem] {
        [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)]
    }

    private var quickDurationColumns: [GridItem] {
        [GridItem(.adaptive(minimum: 72), spacing: 8)]
    }

    private func presetButton(for preset: Int) -> some View {
        Button(action: { minutes = preset }) {
            Text("\(preset) min")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(minutes == preset ? .white : .accentColor)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(
                    Capsule()
                        .fill(minutes == preset ? Color.accentColor : Color.accentColor.opacity(0.15))
                )
        }
        .buttonStyle(.plain)
        .disabled(isRunning)
        .accessibilityLabel("Set quick sit to \(preset) minutes")
    }

    private func adjustButton(label: String, systemImage: String, delta: Int) -> some View {
        Button(action: { adjustMinutes(by: delta) }) {
            Label(label, systemImage: systemImage)
                .font(.subheadline)
                .labelStyle(.titleAndIcon)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.bordered)
        .tint(.accentColor)
        .disabled(isRunning)
        .accessibilityLabel(delta > 0 ? "Add \(delta) minutes" : "Subtract \(-delta) minutes")
    }

    private var progress: Double {
        guard totalSeconds > 0 else { return 0 }
        return Double(totalSeconds - secondsRemaining) / Double(totalSeconds)
    }

    private func startSession() {
        completed = false
        let session = engine.startQuickSit(minutes: minutes, soundId: selectedBell,
                                           startChimes: startChimes, endChimes: endChimes)
        attach(to: session)
    }

    private func attach(to session: TimingSession.QuickSitSession) {
        running = session
        minutes = session.durationSeconds / 60
        selectedBell = session.soundId
        startChimes = session.startChimes
        endChimes = session.endChimes
        refreshRemaining()
        timer?.invalidate()
        let t = Timer(timeInterval: 0.25, repeats: true) { _ in
            refreshRemaining()
        }
        t.tolerance = 0.05
        RunLoop.main.add(t, forMode: .common)
        timer = t
    }

    /// Remaining time always comes from the wall clock, so it never drifts
    /// and is right after the app was in the background.
    private func refreshRemaining() {
        guard let running else { return }
        let remaining = running.endsAt.timeIntervalSinceNow
        secondsRemaining = max(0, Int(remaining.rounded(.up)))
        if remaining <= 0 {
            finishSession()
        }
    }

    private func finishSession() {
        // BellEngine rings the end chimes and then ends the session itself.
        completed = true
        timer?.invalidate()
        timer = nil
        running = nil
        isPresented = false
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
        if isRunning {
            engine.endSession()
        }
        running = nil
        completed = false
    }
}

struct QuickSitSheet_Previews: PreviewProvider {
    static var previews: some View {
        QuickSitSheet(isPresented: .constant(true))
    }
}
