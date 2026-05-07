package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.PersonDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.AppearanceChargeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.AppearanceChargeSarId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.AppearanceOutcomeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.ChargeLegacyDataSar
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.ChargeOutcomeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.ChargeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.CourtAppearanceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.CourtCaseSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.ImmigrationDetentionSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.NextCourtAppearanceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.PeriodLengthSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.RecallSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.RecallSentenceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.RecallTypeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.SentenceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.SentenceTypeSarEntity
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

    val recallSar = recallSarEntity(prn, recallTypeSarEntity())
    recallSentencesSarEntity(sentenceSar, recallSar)

    return courtCaseSar
  }

  fun constructBaseRecallSarEntity(prn: String): RecallSarEntity = recallSarEntity(prn, recallTypeSarEntity())

  fun constructImmigrationDetentionSarEntity(prn: String): ImmigrationDetentionSarEntity = immigrationDetentionSarEntity(prn)

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
    val courtAppearance = CourtAppearanceSarEntity(
      5,
      LocalDate.of(2026, 2, 3),
      "SENTENCING",
      "WNCHCC",
      LocalDate.of(2026, 5, 5),
      mutableSetOf(courtCaseSarEntity),
      mutableSetOf(),
      courtCaseSarEntity,
      appearanceOutcome,
      nextCourtAppearance,
    )
    courtCaseSarEntity.appearances = mutableSetOf(courtAppearance)
    courtCaseSarEntity.latestCourtAppearance = courtAppearance
    return courtAppearance
  }

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
  ) = RecallSarEntity(
    234,
    prn,
    LocalDate.of(2026, 6, 1),
    LocalDate.of(2026, 7, 2),
    false,
    "ACTIVE",
    mutableSetOf(),
    recallTypeSarEntity,
  )

  fun recallTypeSarEntity() = RecallTypeSarEntity(3, "LR")

  fun immigrationDetentionSarEntity(prn: String) = ImmigrationDetentionSarEntity(
    34,
    "NO_LONGER_OF_INTEREST",
    prn,
    "124222111",
    LocalDate.of(2026, 6, 1),
    "RIGHT_TO_REMAIN",
    "Civilian awarded indefinite leave",
  )
}
