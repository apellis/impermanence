import SwiftUI

struct QuickSitSheet: View {
    @Binding var isPresented: Bool
    @AppStorage("use24HourClock") private var use24HourClock = false

    @State private var minutes: Int = 15
    @State private var secondsRemaining: Int = 0
    @State private var isRunning = false
    @State private var selectedBell = BellCatalog.defaultSound.id
    @State private var startChimes = 1
    @State private var endChimes = 1
    @State private var timer: Timer?
    @State private var sessionStartDate: Date?

    private var totalSeconds: Int { minutes * 60 }
    private var bellPlayer: BellPlayer { .shared }
    private let presetDurations = [5, 10, 15, 20, 30, 45, 60, 75, 90]

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Duration")) {
                    VStack(alignment: .leading, spacing: 12) {
                        Stepper(value: $minutes, in: 1...180, step: 1) {
                            Text("\(minutes) minutes")
                        }
                        .disabled(isRunning)

                        HStack(spacing: 12) {
                            adjustButton(label: "−5", systemImage: "minus.circle.fill", delta: -5)
                            adjustButton(label: "−1", systemImage: "minus.circle", delta: -1)
                            Spacer()
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

                if isRunning {
                    Section(header: Text("Progress")) {
                        VStack(alignment: .leading) {
                            ProgressView(value: progress)
                            Text(remainingText)
                                .font(.headline)
                                .monospacedDigit()
                        }
                    }
                }
            }
            .navigationTitle("Quick Sit")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        stopTimer()
                        isPresented = false
                    }
                    .disabled(isRunning)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isRunning ? "Cancel" : "Start") {
                        if isRunning {
                            stopTimer()
                        } else {
                            startSession()
                        }
                    }
                }
            }
            .onDisappear {
                stopTimer()
            }
            .onAppear {
                selectedBell = BellCatalog.defaultSound.id
                startChimes = 1
                endChimes = 1
            }
        }
    }

    private func adjustMinutes(by delta: Int) {
        minutes = max(1, min(180, minutes + delta))
    }

    private var quickDurationColumns: [GridItem] {
        [GridItem(.adaptive(minimum: 90), spacing: 12)]
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
                .frame(minWidth: 72)
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

    private var remainingText: String {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = [.minute, .second]
        formatter.zeroFormattingBehavior = .pad
        return formatter.string(from: TimeInterval(secondsRemaining)) ?? ""
    }

    private func startSession() {
        secondsRemaining = totalSeconds
        isRunning = true
        sessionStartDate = Date()
        playBell(chimes: startChimes)

        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            tick()
        }
        timer?.tolerance = 0.2
    }

    private func tick() {
        guard secondsRemaining > 0 else {
            finishSession()
            return
        }
        secondsRemaining -= 1
    }

    private func finishSession() {
        stopTimer()
        playBell(chimes: endChimes)
        isPresented = false
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
        isRunning = false
        sessionStartDate = nil
    }

    private func playBell(chimes: Int) {
        let bell = Bell(soundId: selectedBell, numRings: chimes)
        bellPlayer.play(bell: bell)
    }
}

struct QuickSitSheet_Previews: PreviewProvider {
    static var previews: some View {
        QuickSitSheet(isPresented: .constant(true))
    }
}
