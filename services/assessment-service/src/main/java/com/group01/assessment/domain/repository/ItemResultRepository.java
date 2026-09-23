package com.group01.assessment.domain.repository;
import com.group01.assessment.domain.entity.ItemResult; import java.util.List; import java.util.UUID;
public interface ItemResultRepository { List<ItemResult> saveAll(List<ItemResult> values); List<ItemResult> findByResultId(UUID resultId); }
