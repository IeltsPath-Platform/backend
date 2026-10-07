package com.ieltspath.assessment.domain.repository;
import com.ieltspath.assessment.domain.entity.ItemResult; import java.util.List; import java.util.UUID;
public interface ItemResultRepository { List<ItemResult> saveAll(List<ItemResult> values); List<ItemResult> findByResultId(UUID resultId); }
