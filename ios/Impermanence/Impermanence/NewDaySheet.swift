//
//  NewDaySheet.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/16/23.
//
//  New Day and Edit share one sheet around DayDetailEditView. Add/Done stay
//  disabled until the day can be saved, and closing with unsaved changes
//  asks first (docs/BEHAVIOR.md, Editing).
//

import SwiftUI

struct NewDaySheet: View {
    @Binding var days: [Day]
    @Binding var isPresentingNewDayView: Bool

    var body: some View {
        DayEditorSheet(title: "New Day", confirmTitle: "Add", original: Day.emptyDay) { newDay in
            days.append(newDay)
            isPresentingNewDayView = false
        } onCancel: {
            isPresentingNewDayView = false
        }
    }
}

/// A sheet that edits a copy of `original` and hands it back on save.
struct DayEditorSheet: View {
    let title: String
    let confirmTitle: String
    let original: Day
    let onSave: (Day) -> Void
    let onCancel: () -> Void

    @State private var draft: Day
    @State private var isConfirmingDiscard = false

    init(title: String, confirmTitle: String, original: Day,
         onSave: @escaping (Day) -> Void, onCancel: @escaping () -> Void) {
        self.title = title
        self.confirmTitle = confirmTitle
        self.original = original
        self.onSave = onSave
        self.onCancel = onCancel
        _draft = State(initialValue: original)
    }

    private var hasChanges: Bool { Self.differs(draft, original) }

    var body: some View {
        NavigationStack {
            DayDetailEditView(day: $draft)
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") {
                            if hasChanges {
                                isConfirmingDiscard = true
                            } else {
                                onCancel()
                            }
                        }
                        .confirmationDialog("Discard changes?", isPresented: $isConfirmingDiscard, titleVisibility: .visible) {
                            Button("Discard Changes", role: .destructive, action: onCancel)
                            Button("Keep Editing", role: .cancel) {}
                        }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button(confirmTitle) {
                            var saved = draft
                            saved.name = saved.name.trimmingCharacters(in: .whitespacesAndNewlines)
                            onSave(saved)
                        }
                        .disabled(!DayDetailEditView.canSave(draft))
                    }
                }
        }
        .interactiveDismissDisabled(hasChanges)
    }

    /// Days aren't Equatable, so compare their encoded form.
    static func differs(_ a: Day, _ b: Day) -> Bool {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        return (try? encoder.encode(a)) != (try? encoder.encode(b))
    }
}

struct NewDaySheet_Previews: PreviewProvider {
    static var previews: some View {
        NewDaySheet(days: .constant([Day.openingDay, Day.fullDay]), isPresentingNewDayView: .constant(true))
    }
}
