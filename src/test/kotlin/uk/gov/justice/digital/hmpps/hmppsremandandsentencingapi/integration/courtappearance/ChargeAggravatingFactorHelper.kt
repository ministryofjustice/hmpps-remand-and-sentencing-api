package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class ChargeAggravatingFactorHelper(private val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun countAggravatingFactor(chargeUuid: UUID, code: String): Int {
    val sql = """
      SELECT count(*) as count
      FROM charge_aggravating_factor caf
      INNER JOIN charge c on c.id = caf.charge_id
      INNER JOIN aggravating_factor af on af.id = caf.aggravating_factor_id
      WHERE c.charge_uuid = :chargeUuid AND af.code = :code
    """.trimIndent()
    val params = MapSqlParameterSource()
      .addValue("chargeUuid", chargeUuid)
      .addValue("code", code)
    return jdbcTemplate.query(sql, params) { rs, _ -> rs.getInt("count") }.first()
  }

  fun countAggravatingFactorForLatestCharge(code: String): Int {
    val sql = """
      SELECT count(*) as count
      FROM charge_aggravating_factor caf
      INNER JOIN charge c on c.id = caf.charge_id
      INNER JOIN aggravating_factor af on af.id = caf.aggravating_factor_id
      WHERE af.code = :code
      AND c.updated_at = (SELECT max(c2.updated_at) FROM charge c2)
    """.trimIndent()
    val params = MapSqlParameterSource().addValue("code", code)
    return jdbcTemplate.query(sql, params) { rs, _ -> rs.getInt("count") }.first()
  }
}
