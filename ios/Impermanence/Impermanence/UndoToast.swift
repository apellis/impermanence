//
//  UndoToast.swift
//  Impermanence
//
//  "“Sit” deleted · Undo": a short-lived bar at the bottom of the screen
//  after a delete, instead of asking first (docs/BEHAVIOR.md, Editing).
//

import SwiftUI
import UIKit

struct UndoAction: Identifiable {
    let id = UUID()
    let message: String
    let undo: () -> Void

    /// "“Sit” deleted", or `fallback` when the item has no name.
    static func deleted(_ name: String, fallback: String, undo: @escaping () -> Void) -> UndoAction {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        return UndoAction(message: trimmed.isEmpty ? fallback : "\u{201C}\(trimmed)\u{201D} deleted", undo: undo)
    }

    /// How long the toast stays: 5 s, longer with VoiceOver so there is time
    /// to reach the button.
    static var visibleSeconds: Double { UIAccessibility.isVoiceOverRunning ? 10 : 5 }
}

extension View {
    func undoToast(_ action: Binding<UndoAction?>) -> some View {
        modifier(UndoToastModifier(action: action))
    }
}

private struct UndoToastModifier: ViewModifier {
    @Binding var action: UndoAction?

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let current = action {
                    HStack(spacing: 12) {
                        Text(current.message)
                            .lineLimit(2)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button("Undo") {
                            current.undo()
                            withAnimation { action = nil }
                        }
                        .fontWeight(.semibold)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .shadow(color: .black.opacity(0.15), radius: 10, y: 3)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .accessibilityElement(children: .contain)
                    .accessibilityIdentifier("undoToast")
                    .task(id: current.id) {
                        UIAccessibility.post(notification: .announcement, argument: "\(current.message). Undo available.")
                        try? await Task.sleep(nanoseconds: UInt64(UndoAction.visibleSeconds * 1_000_000_000))
                        if action?.id == current.id {
                            withAnimation { action = nil }
                        }
                    }
                }
            }
            .animation(.easeInOut(duration: 0.2), value: action?.id)
    }
}
