package com.group01.learningsupport.infrastructure.persistence.mapper;

import com.group01.learningsupport.domain.aggregate.SavedVideoSegment;
import com.group01.learningsupport.infrastructure.persistence.entity.SavedVideoSegmentJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SavedVideoSegmentMapper {
    void copy(SavedVideoSegment source, @MappingTarget SavedVideoSegmentJpaEntity target);

    SavedVideoSegment toDomain(SavedVideoSegmentJpaEntity entity);
}
