package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.PersonDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.AppearanceChargeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.AppearanceChargeSarId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.AppearanceOutcomeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ChargeLegacyDataSar
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ChargeOutcomeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ChargeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.CourtAppearanceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.CourtCaseSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ImmigrationDetentionSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.NextCourtAppearanceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.PeriodLengthSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.RecallSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.RecallSentenceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.RecallTypeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.SentenceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.SentenceTypeSarEntity
import java.time.LocalDate
import java.time.ZonedDateTime

object MockedResponseData {

  fun constructBaseCourtCaseSarEntity(prn: String): CourtCaseSarEntity {
    val courtCaseSar = courtCaseSarEntity(prn)
    val courtAppearance = courtAppearanceSarEntity(
      courtCaseSar,
      appearanceOutcomeSarEntity(),
      nextCourtAppearanceSarEntity(),
    )
    val chargeSar = chargeSarEntity(chargeOutcomeEntity())
    setAppearanceCharges(courtAppearance, chargeSar)

    val sentenceSar = sentencesSarEntity(chargeSar, sentenceTypeSarEntity())
    setPeriodLengthEntity(sentenceSar)

    val recallSar = recallSarEntity(
      prn,
      recallTypeSarEntity(),
      LocalDate.of(2026, 6, 1),
      LocalDate.of(2026, 7, 2),
    )
    recallSentencesSarEntity(sentenceSar, recallSar)

    return courtCaseSar
  }

  fun constructBaseRecallSarEntity(prn: String, revocationDate: LocalDate?, returnToCustodyDate: LocalDate?): RecallSarEntity = recallSarEntity(prn, recallTypeSarEntity(), revocationDate, returnToCustodyDate)

  fun constructImmigrationDetentionSarEntity(prn: String, recordDate: LocalDate): ImmigrationDetentionSarEntity = immigrationDetentionSarEntity(prn, recordDate)

  fun constructPrisonerDetails(prn: String) = PersonDetails(prn, "John", "Smith", "WNCHCC", "1-2-3", LocalDate.of(1980, 1, 1), "PNC12345", "ACTIVE")

  fun constructCourtRegister(prn: String): CourtRegister = CourtRegister(
    "WNCHCC",
    "Wandsworth Crown Court",
    "SW18 3HR",
  )

  fun courtAppearanceSarEntity(
    courtCaseSarEntity: CourtCaseSarEntity,
    appearanceOutcome: AppearanceOutcomeSarEntity,
    nextCourtAppearance: NextCourtAppearanceSarEntity,
  ): CourtAppearanceSarEntity {
    val firstCourtAppearance = courtAppearance(5, courtCaseSarEntity, appearanceOutcome, nextCourtAppearance, LocalDate.of(2026, 2, 3))
    val secondCourtAppearance = courtAppearance(12, courtCaseSarEntity, appearanceOutcome, null, LocalDate.of(2026, 3, 1))
    courtCaseSarEntity.appearances = mutableSetOf(firstCourtAppearance, secondCourtAppearance)
    courtCaseSarEntity.latestCourtAppearance = secondCourtAppearance
    return secondCourtAppearance
  }

  private fun courtAppearance(
    id: Int,
    courtCaseSarEntity: CourtCaseSarEntity,
    appearanceOutcome: AppearanceOutcomeSarEntity,
    nextCourtAppearance: NextCourtAppearanceSarEntity?,
    appearanceDate: LocalDate,
  ): CourtAppearanceSarEntity = CourtAppearanceSarEntity(
    id,
    appearanceDate,
    "SENTENCING",
    "WNCHCC",
    appearanceDate.plusDays(5),
    mutableSetOf(),
    courtCaseSarEntity,
    appearanceOutcome,
    nextCourtAppearance,
  )

  fun courtCaseSarEntity(prn: String) = CourtCaseSarEntity(
    34,
    prn,
    mutableSetOf(),
    null,
    "23423423",
    "ACTIVE",
    ZonedDateTime.parse("2026-05-07T10:00:00+00:00[Europe/London]"),
    ZonedDateTime.parse("2026-05-07T10:00:00+00:00[Europe/London]"),
  )

  fun appearanceOutcomeSarEntity() = AppearanceOutcomeSarEntity(
    34,
    "Imprisonment",
  )

  fun nextCourtAppearanceSarEntity() = NextCourtAppearanceSarEntity(
    17,
    LocalDate.of(2026, 2, 3),
  )

  fun setAppearanceCharges(
    courtAppearanceSarEntity: CourtAppearanceSarEntity,
    chargeSarEntity: ChargeSarEntity,
  ) {
    val appearanceCharge = AppearanceChargeSarEntity(
      AppearanceChargeSarId(3, 5),
      courtAppearanceSarEntity,
      chargeSarEntity,
    )
    courtAppearanceSarEntity.appearanceCharges = mutableSetOf(appearanceCharge)
  }

  fun chargeOutcomeEntity() = ChargeOutcomeSarEntity(
    4534,
    "NON_CUSTODIAL",
    "Street Cleaning Order",
  )

  fun chargeSarEntity(chargeOutcomeSarEntity: ChargeOutcomeSarEntity) = ChargeSarEntity(
    2652,
    chargeOutcomeSarEntity,
    mutableSetOf(),
    "RF96124",
    LocalDate.of(2026, 1, 1),
    LocalDate.of(2026, 1, 2),
    false,
    false,
    false,
    ChargeLegacyDataSar("2026-01-01", "Littering"),
    "ACTIVE",
  )

  fun sentencesSarEntity(
    chargeSarEntity: ChargeSarEntity,
    sentenceTypeSarEntity: SentenceTypeSarEntity,
  ): SentenceSarEntity {
    val sentence = SentenceSarEntity(
      6284,
      chargeSarEntity,
      sentenceTypeSarEntity,
      "CONCURRENT",
      "ACTIVE",
      mutableSetOf(),
      mutableSetOf(),
    )
    chargeSarEntity.sentences = mutableSetOf(sentence)
    return sentence
  }

  fun sentenceTypeSarEntity() = SentenceTypeSarEntity(
    55,
    "DTO",
    "Detention and Training Order",
    true,
  )

  fun setPeriodLengthEntity(sentenceSarEntity: SentenceSarEntity) {
    val periodLength = PeriodLengthSarEntity(
      4092,
      sentenceSarEntity,
      0,
      0,
      3,
      0,
      "years,months,weeks,days",
    )
    sentenceSarEntity.periodLengths = mutableSetOf(periodLength)
  }

  fun recallSentencesSarEntity(
    sentenceSarEntity: SentenceSarEntity,
    recallSarEntity: RecallSarEntity,
  ) {
    val recall = RecallSentenceSarEntity(
      435,
      sentenceSarEntity,
      recallSarEntity,
      "ACTIVE",
    )
    sentenceSarEntity.recallSentences = mutableSetOf(recall)
    recallSarEntity.recallSentences = mutableSetOf(recall)
  }

  fun recallSarEntity(
    prn: String,
    recallTypeSarEntity: RecallTypeSarEntity,
    revocationDate: LocalDate?,
    returnToCustodyDate: LocalDate?,
  ) = RecallSarEntity(
    234,
    prn,
    revocationDate,
    returnToCustodyDate,
    false,
    "ACTIVE",
    mutableSetOf(),
    recallTypeSarEntity,
  )

  fun recallTypeSarEntity() = RecallTypeSarEntity(3, "LR")

  fun immigrationDetentionSarEntity(prn: String, recordDate: LocalDate) = ImmigrationDetentionSarEntity(
    34,
    prn,
    recordDate,
    "124222111",
    "RIGHT_TO_REMAIN",
    "Civilian awarded indefinite leave",
    "NO_LONGER_OF_INTEREST",
  )
}
