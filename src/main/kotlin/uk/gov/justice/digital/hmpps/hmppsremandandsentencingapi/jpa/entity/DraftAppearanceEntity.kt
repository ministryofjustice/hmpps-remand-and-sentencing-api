package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtAppearance
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "draft_appearance")
class DraftAppearanceEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  val draftUuid: UUID,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdByUsername: String,
  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  var sessionBlob: JsonNode,
  @ManyToOne
  @JoinColumn(name = "court_case_id")
  val courtCase: CourtCaseEntity,
) {
  companion object {
    fun from(draftAppearance: DraftCreateCourtAppearance, createdByUsername: String, courtCase: CourtCaseEntity): DraftAppearanceEntity {
      return DraftAppearanceEntity(draftUuid = UUID.randomUUID(), createdByUsername = createdByUsername, sessionBlob = draftAppearance.sessionBlob, courtCase = courtCase)
    }
  }
}
