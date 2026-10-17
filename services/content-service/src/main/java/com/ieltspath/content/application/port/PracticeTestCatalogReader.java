package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.PracticeTestCardResult;
import com.ieltspath.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

public interface PracticeTestCatalogReader {
    Page search(Search query);
    /** Question formats that appear in published practice tests of a skill, with the number of sections using each. */
    List<FormatCount> listFormats(Skill skill);
    /** {@code formats} keeps tests that contain at least one of the listed question formats; empty means no filter. */
    record Search(Skill skill, String scope, Integer partNo, List<String> sources, String query,
                  List<Key> only, List<Key> exclude, int page, int size, List<String> formats) {
        public Search(Skill skill, String scope, Integer partNo, List<String> sources, String query,
                      List<Key> only, List<Key> exclude, int page, int size) {
            this(skill, scope, partNo, sources, query, only, exclude, page, size, List.of());
        }
    }
    record FormatCount(String code, long sectionCount) {}
    record Key(UUID packageId, Integer partNo) {}
    record Page(long totalElements, List<PracticeTestCardResult> items) {}
}
