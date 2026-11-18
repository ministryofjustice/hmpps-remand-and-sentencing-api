package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.ordertype

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

/**
 * No JPA used here as this table is not part of RaS per se and no Entities are mapped to it.
 * It is an unmaintained copy of the NOMIS ORDER_TYPES table, retained to support
 * downstream consumers that depend on the legacy NOMIS data.
 * See V182__add_legacy_nomis_order_types.sql
 */
class LegacyOrderTypeTests : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

  @Test
  fun `should return order type in create datetime order`() {
    val sql = """
      SELECT order_type
      FROM legacy_order_types
      ORDER BY create_datetime
    """.trimIndent()
    val actual = jdbcTemplate.query(sql) { rs, _ -> rs.getString("order_type") }.toList<String>()
    assertThat(actual).containsExactly(
      "DA", "REQ", "PROB", "REM", "BAIL", "P_COM", "ARR", "COM", "MVA",
      "DOC", "OOS", "BIR", "BRE", "CJF", "CJO", "CJR", "CJS", "CPR", "DEF",
      "AUTO", "SEC 10/3", "TRL", "NAR", "PAPP",
    )
  }

  @Test
  fun `should return description in create datetime order`() {
    val sql = """
      SELECT description
      FROM legacy_order_types
      ORDER BY create_datetime
    """.trimIndent()
    val actual = jdbcTemplate.query(sql) { rs, _ -> rs.getString("description") }.toList<String>()
    assertThat(actual).containsExactly(
      "DISTRICT ATTORNEY",
      "REQUEST",
      "PROBATION",
      "Remanded by Magistrates Court",
      "BAIL INFORMATION REPORT",
      "PAROLE COMMISSION",
      "ARREST",
      "SENTENCE ORDER",
      "MOTOR VEHICLE ADMINISTRATION",
      "DIVISION OF CORRECTION",
      "OUT OF STATE",
      "Bail Information Report",
      "Breach Report",
      "Fast track PSR",
      "Oral report",
      "Progress Report",
      "Standard Delivery PSR",
      "Community Order Completion/Progress Rpt",
      "Deferred Sentence Report",
      "Sentence Imposed",
      "Convicted by Magistrates-Awaiting Report",
      "Committed for Trial",
      "Expedited for Trial - Narey Proceedings",
      "Prosecution Appeal against Grant of Bail",
    )
  }
}
