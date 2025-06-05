package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum

enum class PagedCourtCaseOrderBy(val orderBy: String) {
  STATUS_APPEARANCE_DATE_DESC("cc1.status_id ASC, lca1.appearance_date DESC"),
  APPEARANCE_DATE_ASC("lca1.appearance_date ASC"),
  APPEARANCE_DATE_DESC("lca1.appearance_date DESC"),
}
