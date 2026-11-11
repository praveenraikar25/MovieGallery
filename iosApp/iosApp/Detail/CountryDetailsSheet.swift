import Shared
import SwiftUI

/// One production country's details, from the Countries GraphQL API (movie-detail "Tapping a country
/// chip opens its details"). Its failures stay inside the sheet ("Country details failures stay
/// inside the sheet"); a country the source has no record of says so, without a Retry.
struct CountryDetailsSheet: View {
    let state: CountrySheetState
    let onRetry: () -> Void

    var body: some View {
        let details = state.details
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                heading(
                    flag: details?.flag ?? state.country.flag,
                    name: details?.name ?? state.country.name,
                    nativeName: details?.nativeName
                )

                if state.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, minHeight: 196)
                } else if let details {
                    rows(details)
                } else if state.error != nil {
                    VStack(spacing: 16) {
                        Text("Couldn't load country details.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Button("Retry", action: onRetry)
                            .buttonStyle(.bordered)
                            .buttonBorderShape(.capsule)
                    }
                    .frame(maxWidth: .infinity, minHeight: 196)
                } else if state.notFound {
                    Text("No details available for this country.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, minHeight: 120)
                }
            }
            .padding(24)
        }
    }

    private func heading(flag: String?, name: String, nativeName: String?) -> some View {
        HStack(spacing: 14) {
            if let flag {
                Text(flag)
                    .font(.system(size: 36))
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(name).font(.title2.weight(.semibold))
                if let nativeName {
                    Text(nativeName)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    /// The shared rows (Capital, Continent, Languages, Currency), with "—" for anything missing.
    private func rows(_ details: CountryDetails) -> some View {
        let rows = CountryDetailRowsKt.countryDetailRows(details: details)
        return VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                Divider()
                HStack(alignment: .firstTextBaseline, spacing: 16) {
                    Text((row.first as String?) ?? "")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text((row.second as String?) ?? "")
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .padding(.vertical, 14)
            }
        }
        .padding(.top, 20)
    }
}
