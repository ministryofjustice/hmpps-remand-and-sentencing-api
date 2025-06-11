package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum

enum class RecallType {
  LR,
  FTR_14,
  FTR_28,
  FTR_HDC_14,
  FTR_HDC_28,
  CUR_HDC,
  IN_HDC,
  ;

  fun isFixedTermRecall(): Boolean = listOf(FTR_14, FTR_28, FTR_HDC_14, FTR_HDC_28).contains(this)
}
