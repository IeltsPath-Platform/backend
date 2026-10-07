package com.ieltspath.library.infrastructure.persistence.mapper;

import com.ieltspath.library.domain.aggregate.SavedVideoSegment;
import com.ieltspath.library.infrastructure.persistence.entity.SavedVideoSegmentJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SavedVideoSegmentMapper {
    void copy(SavedVideoSegment source, @MappingTarget SavedVideoSegmentJpaEntity target);

    SavedVideoSegment toDomain(SavedVideoSegmentJpaEntity entity);
}
