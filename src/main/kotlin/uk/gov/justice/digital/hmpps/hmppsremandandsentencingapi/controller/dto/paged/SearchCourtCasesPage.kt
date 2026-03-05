package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class SearchCourtCasesPage<T : Any>(content: List<T>, pageable: Pageable, total: Long, val courtCaseTotal: Long) : PageImpl<T>(content, pageable, total)
