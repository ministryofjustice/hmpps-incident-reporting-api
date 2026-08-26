package uk.gov.justice.digital.hmpps.incidentreporting.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.AnalyticalMarkerType
import java.io.File

/**
 * Writes reference-data.csv, the companion to the SchemaSpy report and data-dictionary.csv.
 *
 * Deliberately small. Most code lists in this schema are already queryable as constant_* tables, copied
 * there from the Kotlin enumerations for exactly this reason (see V1_14), and are also served by the
 * constants endpoints. This covers only the two that have no table behind them: InformationSource, and the
 * marker descriptions for AnalyticalMarkerType - analytical_marker holds the codes but nothing anywhere
 * says what they mean.
 *
 * Needs no database: the values come from the enumerations themselves, so the list cannot drift from the
 * code. A new value with no description fails the test rather than exporting a blank row.
 *
 * Excluded from normal test runs; run with `./gradlew -Pinit-db=true test` (see build.gradle.kts).
 */
class ExportReferenceData {

  @Test
  fun `exports reference data`() {
    val rows = mutableListOf<Row>()

    rows += enumRows(
      "report.source / report.modified_in",
      InformationSource.entries,
      mapOf(
        InformationSource.DPS to
          "The DPS incident reporting service. On source, the report was created here; on " +
          "modified_in, it was last changed here.",
        InformationSource.NOMIS to
          "NOMIS. On source, the report was migrated or synchronised in from NOMIS; on modified_in, " +
          "the last change came from NOMIS rather than DPS. The two can differ - a report created in " +
          "one system can be edited in the other.",
      ),
    )

    rows += enumRows(
      "analytical_marker.marker_type",
      AnalyticalMarkerType.entries,
      AnalyticalMarkerType.entries.associateWith { it.markerDescription },
      notes = { "flags an answer code as indicating this kind of serious harm" },
    )

    val output = File(System.getProperty("referenceDataOutput") ?: "reference-data.csv")
    output.bufferedWriter().use { writer ->
      writer.write("column_ref,code,description,notes\n")
      rows.forEach { writer.write("${it.toCsv()}\n") }
    }
    println("Wrote ${rows.size} reference data rows to ${output.absolutePath}")
  }

  /**
   * Every value of the enum, with its description. Fails rather than exporting a blank row when a value
   * has no description - a new enum value is exactly the thing a consumer would otherwise not be able to
   * decode.
   */
  private fun <T : Enum<T>> enumRows(
    columnRef: String,
    values: List<T>,
    descriptions: Map<T, String>,
    notes: (T) -> String = { "" },
  ): List<Row> {
    assertThat(values.filterNot(descriptions::containsKey))
      .describedAs("$columnRef values with no description - add one in ExportReferenceData")
      .isEmpty()

    return values.map { Row(columnRef, it.name, descriptions.getValue(it), notes(it)) }
  }

  private data class Row(
    val columnRef: String,
    val code: String,
    val description: String,
    val notes: String = "",
  ) {
    fun toCsv() = listOf(columnRef, code, description, notes).joinToString(",") { escape(it) }

    private fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""
  }
}
