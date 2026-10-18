package com.ieltspath.content.application.command;

import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.domain.vo.Skill;

import java.util.List;

public record SearchPracticeTestsCommand(Skill skill, String scope, Integer partNo, List<String> sources,
                                         String query, List<PracticeTestCatalogReader.Key> only,
                                         List<PracticeTestCatalogReader.Key> exclude, int page, int size,
                                         List<String> formats) {}
