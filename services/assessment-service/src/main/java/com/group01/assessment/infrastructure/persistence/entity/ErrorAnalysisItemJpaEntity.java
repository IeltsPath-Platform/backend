package com.group01.assessment.infrastructure.persistence.entity;
import jakarta.persistence.*; import lombok.*; import java.util.UUID;
@Entity @Table(name="error_analysis_items") @Getter @Setter @NoArgsConstructor public class ErrorAnalysisItemJpaEntity { @Id private UUID id; @Column(name="result_id",nullable=false) private UUID resultId; @Column(name="attempt_item_id") private UUID attemptItemId; @Column(name="taxonomy_code",nullable=false) private String taxonomyCode; @Column(nullable=false) private String severity; @Column(columnDefinition="TEXT") private String note; }
