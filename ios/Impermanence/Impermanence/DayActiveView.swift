//
//  DayActiveView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/12/23.
//

import SwiftUI
import UIKit

struct DayActiveView: View {
    @Binding var day: Day
    @StateObject var dayTimer: DayTimer

    @AppStorage("loopDays") private var loopDaysSetting = true

    private let bellPlayer = BellPlayer.shared

    init(day: Binding<Day>) {
        self._day = day
        let storedLoopSetting: Bool = {
            if UserDefaults.standard.object(forKey: "loopDays") == nil {
                return true
            }
            return UserDefaults.standard.bool(forKey: "loopDays")
        }()

        _dayTimer = StateObject(
            wrappedValue: DayTimer(
                startTime: day.wrappedValue.startTime,
                segments: day.wrappedValue.segments,
                startBell: day.wrappedValue.startBell,
                defaultBell: day.wrappedValue.defaultBell,
                loopDays: storedLoopSetting
            )
        )
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16.0)
                .fill(day.theme.mainColor)
            VStack {
                Text("\(dayTimer.segmentText)")
                    .font(.headline)
                    .padding(.top)
                if dayTimer.activeSegmentTimeElapsed >= 0 && dayTimer.activeSegmentTimeRemaining >= 0 {
                    SegmentProgressView(timeElapsed: dayTimer.activeSegmentTimeElapsed, timeRemaining: dayTimer.activeSegmentTimeRemaining, theme: day.theme)
                }
                Divider()
                    .frame(height: 2)
                    .overlay(day.theme.accentColor)
                ScrollView {
                    Text("Agenda")
                        .font(.headline)
                        .padding(.top)
                    ForEach(day.segments.indices, id: \.self) { i in
                        let segment = day.segments[i]
                        SegmentCardView(segment: segment,
                                        startTime: day.segmentStartEndTimes[i].0,
                                        endTime: day.segmentStartEndTimes[i].1,
                                        theme: day.theme,
                                        bell: segment.resolvedEndBell(defaultBell: day.defaultBell),
                                        useTheme: true,
                                        highlighted: i == dayTimer.segmentIndex)
                    }
                    .animation(.easeInOut(duration: 0.25), value: dayTimer.segmentIndex)
                }
                .padding()
            }
        }
        .padding()
        .foregroundColor(day.theme.accentColor)
        .onAppear {
            dayTimer.segmentChangedAction = { bell in
                guard let bell else { return }
                bellPlayer.play(bell: bell)
            }
            dayTimer.loopDays = loopDaysSetting
            dayTimer.startDay()
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            dayTimer.stopDay()
            UIApplication.shared.isIdleTimerDisabled = false
        }
        .onChange(of: loopDaysSetting) { newValue in
            dayTimer.loopDays = newValue
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button {
                        bellPlayer.play(bell: day.manualBell)
                    } label: {
                        Label("Ring now", systemImage: "play.circle")
                    }

                    Section("Manual bell sound") {
                        Picker("Sound", selection: $day.manualBell.soundId) {
                            ForEach(BellCatalog.sounds) { sound in
                                Text(sound.displayName).tag(sound.id)
                            }
                        }
                        .pickerStyle(.automatic)
                    }

                    Section("Chimes") {
                        Stepper(value: $day.manualBell.numRings, in: 1...12) {
                            Label("Chimes: \(day.manualBell.numRings)", systemImage: "bell")
                        }
                    }
                } label: {
                    Image(systemName: "bell")
                }
                .accessibilityLabel("Manual bell controls")
            }
        }
    }
}

struct DayActiveView_Previews: PreviewProvider {
    static var previews: some View {
        DayActiveView(day: .constant(Day.fullDay))
    }
}
