package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyRecall
import java.util.UUID

@Service
class LegacyRecallService(
  private val recallRepository: RecallRepository,
) {

  @Transactional(readOnly = true)
  fun get(recallUuid: UUID): LegacyRecall = LegacyRecall.from(getUnlessDeleted(recallUuid))

  private fun getUnlessDeleted(uuid: UUID): RecallEntity = recallRepository.findOneByRecallUuid(uuid)
    ?.takeUnless { entity -> entity.statusId == RecallEntityStatus.DELETED }
    ?: throw EntityNotFoundException("No recall found at $uuid")
}
