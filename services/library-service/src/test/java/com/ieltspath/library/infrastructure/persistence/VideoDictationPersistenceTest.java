package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.command.RecordVideoDictationAttemptCommand;
import com.ieltspath.library.application.result.DictationVideoDetailResult;
import com.ieltspath.library.application.result.DictationVideoListItemResult;
import com.ieltspath.library.application.result.VideoDictationProgressResult;
import com.ieltspath.library.application.usecase.GetDictationVideoUseCase;
import com.ieltspath.library.application.usecase.GetVideoDictationProgressUseCase;
import com.ieltspath.library.application.usecase.ListDictationVideosUseCase;
import com.ieltspath.library.application.usecase.RecordVideoDictationAttemptUseCase;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.exception.VideoNotFoundException;
import com.ieltspath.library.domain.vo.VideoLevel;
import com.ieltspath.library.infrastructure.adapter.DictationVideoRepositoryAdapter;
import com.ieltspath.library.infrastructure.adapter.VideoDictationAttemptRepositoryAdapter;
import com.ieltspath.library.infrastructure.adapter.WordPronunciationRepositoryAdapter;
import com.ieltspath.library.infrastructure.persistence.mapper.VideoDictationAttemptMapperImpl;
import com.ieltspath.library.infrastructure.persistence.repository.VocabularyItemJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RecordVideoDictationAttemptUseCase.class, GetVideoDictationProgressUseCase.class,
        ListDictationVideosUseCase.class, GetDictationVideoUseCase.class,
        VideoDictationAttemptRepositoryAdapter.class, VideoDictationAttemptMapperImpl.class,
        DictationVideoRepositoryAdapter.class, WordPronunciationRepositoryAdapter.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class VideoDictationPersistenceTest {
    private static final String DEMO_YOUTUBE_ID = "jNQXAC9IVRw";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private VocabularyItemJpaRepository vocabularyItems;

    @Autowired
    private RecordVideoDictationAttemptUseCase record;

    @Autowired
    private GetVideoDictationProgressUseCase progress;

    @Autowired
    private ListDictationVideosUseCase list;

    @Autowired
    private GetDictationVideoUseCase detail;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void everyWordOfTheSeededSentencesHasAnAmericanPronunciation() {
        // Splits each transcript into words the way the player does (see matchDictation / sentenceWords in the frontend).
        String missingWords = """
                select distinct w.word
                from (select btrim(regexp_replace(lower(translate(t.raw, '’‘', '''''')), '[^[:alnum:]'']', '', 'g'), '''') as word
                      from video_segments s
                      cross join lateral regexp_split_to_table(
                          btrim(regexp_replace(s.transcript, '[-–—]', ' ', 'g')), '\\s+') as t(raw)) w
                where w.word <> ''
                  and not exists (select 1 from vocabulary_items p where p.normalized_lemma = w.word and p.ipa is not null)
                order by 1
                """;
        List<String> missing = jdbc.queryForList(missingWords, String.class);
        assertTrue(missing.isEmpty(), "words without a pronunciation, add them in a migration: " + missing);

        assertEquals("/ˈwiˌkɛnd/", jdbc.queryForObject(
                "select ipa from vocabulary_items where normalized_lemma = 'weekend'", String.class));
        assertEquals("/ˈɡoʊɪŋ/", jdbc.queryForObject(
                "select ipa from vocabulary_items where normalized_lemma = 'going'", String.class));
        assertEquals("/doʊnt/", jdbc.queryForObject(
                "select ipa from vocabulary_items where normalized_lemma = 'don''t'", String.class));
    }

    @Test
    void everySentenceComesWithOnePronunciationPerWord() {
        DictationVideoListItemResult item = list.execute(UUID.randomUUID(), VideoLevel.A1).stream()
                .filter(v -> v.title().contains("My Weekend")).findFirst().orElseThrow();
        DictationVideoDetailResult video = detail.execute(item.id());

        var sentence = video.segments().stream()
                .filter(s -> "I am going to tell you about my weekend.".equals(s.transcript())).findFirst().orElseThrow();
        assertEquals(List.of("/aɪ/", "/æm/", "/ˈɡoʊɪŋ/", "/tu/", "/tɛl/", "/ju/", "/əˈbaʊt/", "/maɪ/", "/ˈwiˌkɛnd/"),
                sentence.ipaWords());

        for (var segment : video.segments()) {
            assertEquals(com.ieltspath.library.domain.vo.SentenceWords.normalized(segment.transcript()).size(),
                    segment.ipaWords().size(), "one entry per word: " + segment.transcript());
            assertTrue(segment.ipaWords().stream().noneMatch(java.util.Objects::isNull),
                    "every word of the seeded sentences has a pronunciation: " + segment.transcript());
        }
    }

    @Test
    void videosWithCaptionsCarryTheirWholeTalkAndKeepTheOriginalSentencesInPlace() {
        List<DictationVideoListItemResult> all = list.execute(UUID.randomUUID(), null);
        long whole = all.stream().filter(v -> v.totalSegments() >= 25).count();
        assertTrue(whole >= 26, "videos with a sentence for the whole talk: " + whole);

        DictationVideoListItemResult weekend = all.stream().filter(v -> v.title().contains("My Weekend")).findFirst().orElseThrow();
        assertTrue(weekend.totalSegments() > 100, "My Weekend has " + weekend.totalSegments() + " sentences");
        DictationVideoDetailResult video = detail.execute(weekend.id());
        // The eight sentences the video started with are still there, with their Vietnamese, mixed in by time.
        assertEquals(8, video.segments().stream().filter(s -> s.translationVi() != null).count());
        var ids = video.segments().stream().map(s -> s.id()).distinct().count();
        assertEquals(video.segments().size(), ids, "no sentence twice");
    }

    @Test
    void theCartoonLessonsWithWordsInThePictureAreNotOffered() {
        // The words printed in the picture give the answer of a dictation away and cannot be switched off.
        List<String> offered = list.execute(UUID.randomUUID(), null).stream().map(DictationVideoListItemResult::title).toList();
        for (String title : List.of("English Listening Practice for Beginners: Waking Up",
                "Asking for Directions in English: Easy Conversation Practice for Beginners",
                "My Daily Routine: Easy English Listening Practice (A2)",
                "My Family at Home: Easy English Listening Practice (A2)")) {
            assertFalse(offered.contains(title), title + " has words burned into the picture");
        }
        assertFalse(offered.isEmpty());
    }

    @Test
    void pronunciationRowsDoNotShowInTheVocabularySearch() {
        assertTrue(vocabularyItems.findByNormalizedLemma("weekend").isPresent());
        assertTrue(vocabularyItems.searchByLemma("weekend").isEmpty(), "an INACTIVE pronunciation row is not a vocabulary hit");
        assertFalse(vocabularyItems.searchByLemma("accommodation").isEmpty(), "the IELTS vocabulary is still found");
    }

    @Test
    void seedHasOnePublishedA2VideoWithSevenOrderedSegments() {
        DictationVideoDetailResult video = demoVideo();

        assertEquals(VideoLevel.A2, video.level());
        assertEquals(7, video.segments().size());
        int previousEnd = 0;
        for (int i = 0; i < 7; i++) {
            var segment = video.segments().get(i);
            assertEquals(i + 1, segment.sequenceNo());
            assertTrue(segment.startMs() >= previousEnd, "segments must not overlap");
            assertTrue(segment.endMs() > segment.startMs());
            assertFalse(segment.transcript().isBlank());
            previousEnd = segment.endMs();
        }
    }

    @Test
    void seedCoversEveryLevelWithOrderedSegmentsAndNoPremiumGate() {
        List<DictationVideoListItemResult> all = list.execute(UUID.randomUUID(), null);

        for (VideoLevel level : VideoLevel.values()) {
            List<DictationVideoListItemResult> atLevel = list.execute(UUID.randomUUID(), level);
            assertFalse(atLevel.isEmpty(), "no video seeded for " + level);
            for (DictationVideoListItemResult item : atLevel) {
                assertEquals(level, item.level());
                assertTrue(item.totalSegments() >= 6, item.title() + " has too few sentences");
                DictationVideoDetailResult video = detail.execute(item.id());
                int previousEnd = 0;
                for (int i = 0; i < video.segments().size(); i++) {
                    var segment = video.segments().get(i);
                    assertEquals(i + 1, segment.sequenceNo(), video.title());
                    assertTrue(segment.startMs() >= previousEnd, video.title() + ": segments overlap at " + (i + 1));
                    assertTrue(segment.endMs() > segment.startMs());
                    assertFalse(segment.transcript().isBlank());
                    // The sentences added later, from the whole caption track, have no Vietnamese.
                    assertTrue(segment.translationVi() == null || !segment.translationVi().isBlank());
                    previousEnd = segment.endMs();
                }
            }
        }
        assertEquals(VideoLevel.values().length, all.stream().map(DictationVideoListItemResult::level).distinct().count());
    }

    @Test
    void libraryListsTheVideoWithPassedCountsAndFiltersByLevel() {
        UUID learner = UUID.randomUUID();
        DictationVideoDetailResult video = demoVideo();

        DictationVideoListItemResult before = demoListItem(list.execute(learner, null));
        assertEquals(7, before.totalSegments());
        assertEquals(0, before.passedSegments());
        assertNull(before.lastPracticedAt(), "never practised");

        record.execute(cmd(learner, video.segments().get(0).id(), "All right, so here we are", true, "100"));
        record.execute(cmd(learner, video.segments().get(0).id(), "again", true, "100"));
        record.execute(cmd(learner, video.segments().get(1).id(), "wrong", false, "10"));

        DictationVideoListItemResult after = demoListItem(list.execute(learner, VideoLevel.A2));
        assertEquals(1, after.passedSegments(), "distinct passed segments");
        assertNotNull(after.lastPracticedAt(), "latest answer time");
        assertNull(demoListItem(list.execute(UUID.randomUUID(), null)).lastPracticedAt(), "other learners' answers do not count");
        assertTrue(list.execute(learner, VideoLevel.C2).stream()
                .noneMatch(item -> DEMO_YOUTUBE_ID.equals(item.youtubeVideoId())), "the A2 video must not show at C2");
    }

    @Test
    void attemptsAreNumberedPerLearnerAndSegmentAndDriveProgress() {
        DictationVideoDetailResult video = demoVideo();
        UUID learner = UUID.randomUUID();
        UUID otherLearner = UUID.randomUUID();
        UUID first = video.segments().get(0).id();
        UUID second = video.segments().get(1).id();

        assertEquals(1, record.execute(cmd(learner, first, "all right so here we", false, "83.33")).attemptNo());
        assertEquals(2, record.execute(cmd(learner, first, "All right, so here we are", true, "100")).attemptNo());
        assertEquals(1, record.execute(cmd(learner, second, "in front of the elephant", false, "75")).attemptNo());
        assertEquals(1, record.execute(cmd(otherLearner, first, "x", false, "0")).attemptNo());

        VideoDictationProgressResult result = progress.execute(learner, video.id());

        assertEquals(7, result.totalSegments());
        assertEquals(1, result.passedSegments());
        assertEquals(second, result.nextSegmentId());
        assertEquals(2, result.nextSequenceNo());
        var firstProgress = result.segments().get(0);
        assertEquals(2, firstProgress.attempts());
        assertTrue(firstProgress.passed());
        assertEquals(0, new BigDecimal("100").compareTo(firstProgress.bestAccuracyPercent()));
        var secondProgress = result.segments().get(1);
        assertEquals(1, secondProgress.attempts());
        assertFalse(secondProgress.passed());
        assertEquals(0, result.segments().get(2).attempts());
        assertNull(result.segments().get(2).bestAccuracyPercent());
    }

    @Test
    void progressOfAnUntouchedVideoPointsAtTheFirstSegment() {
        DictationVideoDetailResult video = demoVideo();

        VideoDictationProgressResult result = progress.execute(UUID.randomUUID(), video.id());

        assertEquals(0, result.passedSegments());
        assertEquals(1, result.nextSequenceNo());
        assertEquals(video.segments().get(0).id(), result.nextSegmentId());
    }

    @Test
    void unknownVideoOrSegmentIsNotFound() {
        assertThrows(VideoNotFoundException.class, () -> progress.execute(UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(VideoNotFoundException.class, () -> detail.execute(UUID.randomUUID()));
        assertThrows(ResourceNotFoundException.class,
                () -> record.execute(cmd(UUID.randomUUID(), UUID.randomUUID(), "x", false, "0")));
    }

    private DictationVideoDetailResult demoVideo() {
        UUID id = demoListItem(list.execute(UUID.randomUUID(), null)).id();
        return detail.execute(id);
    }

    private static DictationVideoListItemResult demoListItem(List<DictationVideoListItemResult> items) {
        return items.stream().filter(item -> DEMO_YOUTUBE_ID.equals(item.youtubeVideoId())).findFirst().orElseThrow();
    }

    private static RecordVideoDictationAttemptCommand cmd(UUID user, UUID segment, String typed, boolean ok, String pct) {
        return new RecordVideoDictationAttemptCommand(user, segment, typed, ok, new BigDecimal(pct));
    }
}
