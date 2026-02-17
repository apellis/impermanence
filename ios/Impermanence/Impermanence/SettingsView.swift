//
//  SettingsView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/15/23.
//

import SwiftUI
import UniformTypeIdentifiers

struct DayPlanExportDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }

    var days: [Day]

    init(days: [Day]) {
        self.days = days
    }

    init(configuration: ReadConfiguration) throws {
        let data = configuration.file.regularFileContents ?? Data()
        self.days = try PortableDayPlanCodec.importDays(from: data)
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        let data = try PortableDayPlanCodec.exportData(days: days)
        return FileWrapper(regularFileWithContents: data)
    }
}

struct SettingsView: View {
    @Binding var days: [Day]
    @Binding var isPresentingSettingsView: Bool

    @AppStorage("use24HourClock") private var use24HourClock = false
    @AppStorage("loopDays") private var loopDays = true
    @AppStorage("keepScreenAwakeDuringActiveDay") private var keepScreenAwakeDuringActiveDay = true
    @State private var isPresentingImporter = false
    @State private var isPresentingExporter = false
    @State private var transferMessage: String?
    @State private var transferTitle = "Data Transfer"

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(isOn: $use24HourClock) {
                        Text("Use 24-hour clock")
                    }
                    Toggle(isOn: $loopDays) {
                        Text("Loop days at midnight")
                    }
                    Toggle(isOn: $keepScreenAwakeDuringActiveDay) {
                        Text("Keep screen awake during active day")
                    }
                }
                Section {
                    Button("Import Days") {
                        isPresentingImporter = true
                    }
                    Button("Export Days") {
                        isPresentingExporter = true
                    }
                } header: {
                    Text("Data")
                } footer: {
                    Text("Import or export day plans as JSON.")
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        isPresentingSettingsView = false
                    }
                }
            }
            .fileImporter(
                isPresented: $isPresentingImporter,
                allowedContentTypes: [.json],
                allowsMultipleSelection: false
            ) { result in
                do {
                    guard let url = try result.get().first else { return }
                    let hasAccess = url.startAccessingSecurityScopedResource()
                    defer {
                        if hasAccess {
                            url.stopAccessingSecurityScopedResource()
                        }
                    }
                    let data = try Data(contentsOf: url)
                    let importedDays = try PortableDayPlanCodec.importDays(from: data)
                    days = importedDays
                    transferTitle = "Import Complete"
                    transferMessage = "Imported \(importedDays.count) day plans."
                } catch {
                    transferTitle = "Import Failed"
                    transferMessage = error.localizedDescription
                }
            }
            .fileExporter(
                isPresented: $isPresentingExporter,
                document: DayPlanExportDocument(days: days),
                contentType: .json,
                defaultFilename: exportFileName
            ) { result in
                do {
                    _ = try result.get()
                    transferTitle = "Export Complete"
                    transferMessage = "Exported \(days.count) day plans."
                } catch {
                    transferTitle = "Export Failed"
                    transferMessage = error.localizedDescription
                }
            }
            .alert(transferTitle, isPresented: transferAlertPresented) {
                Button("OK", role: .cancel) {
                    transferMessage = nil
                }
            } message: {
                Text(transferMessage ?? "An unknown data transfer error occurred.")
            }
        }
    }

    private var transferAlertPresented: Binding<Bool> {
        Binding(
            get: { transferMessage != nil },
            set: { isPresented in
                if !isPresented {
                    transferMessage = nil
                }
            }
        )
    }

    private var exportFileName: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return "impermanence-days-\(formatter.string(from: Date()))"
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(days: .constant([Day.openingDay]), isPresentingSettingsView: .constant(true))
    }
}
