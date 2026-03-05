package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class SearchCourtCasesPage(content: List<PagedCourtCase>, pageable: Pageable, total: Long, val prisonerCourtCaseTotal: Long) : PageImpl<PagedCourtCase>(content, pageable, total)
