package com.ieltspath.assessment.domain.repository;
import com.ieltspath.assessment.domain.entity.SkillScore; import java.util.List; import java.util.UUID;
public interface SkillScoreRepository { List<SkillScore> saveAll(List<SkillScore> values); List<SkillScore> findByResultId(UUID resultId); }
