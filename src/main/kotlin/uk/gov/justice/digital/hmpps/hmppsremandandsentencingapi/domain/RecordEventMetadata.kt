package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

data class RecordEventMetadata<T>(
  val record: T,
  val eventMetadata: EventMetadata,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RecordEventMetadata<*>

    return record == other.record
  }

  override fun hashCode(): Int = record?.hashCode() ?: 0

  fun <S> toNewRecord(record: S): RecordEventMetadata<S> = RecordEventMetadata(record, eventMetadata)
}
