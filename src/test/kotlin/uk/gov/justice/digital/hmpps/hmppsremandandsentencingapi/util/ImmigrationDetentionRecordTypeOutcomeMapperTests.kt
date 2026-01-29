package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.*
import java.util.stream.Stream

class ImmigrationDetentionRecordTypeOutcomeMapperTests {

  @ParameterizedTest(name = "appearance outcome uuid {0} is immigration record type {1}")
  @MethodSource("appearanceOutcomeToRecordTypeParameters")
  fun `outcome to record type tests`(appearanceOutcomeUuid: UUID, expectedRecordType: ImmigrationDetentionRecordType) {
    val appearanceOutcome = AppearanceOutcomeEntity(
      0, "name", appearanceOutcomeUuid, "1", "IMMIGRATION", "IMMIGRATION", 1,
      UUID.randomUUID(), false,
      ReferenceEntityStatus.ACTIVE, "I",
    )

    val result = ImmigrationDetentionRecordTypeOutcomeMapper.appearanceOutcomeToRecordType(appearanceOutcome)
    Assertions.assertThat(result).isEqualTo(expectedRecordType)
  }

  companion object {
    @JvmStatic
    fun appearanceOutcomeToRecordTypeParameters(): Stream<Arguments> = Stream.of(
      Arguments.of(UUID.fromString("5c670576-ffbf-4005-8d54-4aeba7bf1a22"), ImmigrationDetentionRecordType.IS91),
      Arguments.of(UUID.fromString("d774d9dd-12e8-4b6e-88e8-3e7739dff9e1"), ImmigrationDetentionRecordType.IMMIGRATION_BAIL),
      Arguments.of(UUID.fromString("b28afb19-dd94-4970-8071-e616b33274cb"), ImmigrationDetentionRecordType.DEPORTATION_ORDER),
      Arguments.of(UUID.fromString("15524814-3238-4e4b-86a7-cda31b0221ec"), ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST),
      Arguments.of(UUID.randomUUID(), ImmigrationDetentionRecordType.UNKNOWN),
    )
  }
}
