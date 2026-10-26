package com.ieltspath.library.domain.catalog;

import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.CefrLevel;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.RelationKind;
import com.ieltspath.library.domain.vo.VocabularySetKind;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogDomainRulesTest {
    @Test
    void blankItemTextClearsTheFieldAndTextIsTrimmed() {
        var details = new VocabularyItemDetails("  /ˈwɜːd/ ", "   ", "", null, null, true, "  ");

        assertEquals("/ˈwɜːd/", details.ipaUk());
        assertNull(details.ipaUs());
        assertNull(details.audioUkReference());
        assertNull(details.wordFamily());
    }

    @Test
    void frequencyRankMustBePositive() {
        assertThrows(InvalidDataException.class, () -> new VocabularyItemDetails(null, null, null, null, 0, false, null));
        assertEquals(1, new VocabularyItemDetails(null, null, null, null, 1, false, null).frequencyRank());
    }

    @Test
    void senseRegisterIsBoundedByItsColumn() {
        assertThrows(InvalidDataException.class,
                () -> new VocabularySenseDetails(CefrLevel.B2, null, "x".repeat(31), null, null, null));
        assertEquals("formal", new VocabularySenseDetails(null, null, " formal ", null, null, null).register());
    }

    @Test
    void childEntriesNeedTheirRequiredText() {
        assertThrows(InvalidDataException.class, () -> new Collocation(" ", null, null));
        assertThrows(InvalidDataException.class, () -> new CommonMistake("wrong", " ", null));
        assertThrows(InvalidDataException.class, () -> new UsageExample("A sentence.", ""));
        assertThrows(InvalidDataException.class, () -> new WordRelation("synonym", null, null));
        assertEquals("steady", new WordRelation(" steady ", null, RelationKind.SYNONYM).relatedLemma());
    }

    @Test
    void topicNeedsNamesAndRejectsCoverUrls() {
        assertThrows(InvalidDataException.class, () -> new VocabularyTopicDraft("env", "Environment", " ", null, 0, null));
        assertThrows(InvalidDataException.class,
                () -> new VocabularyTopicDraft("env", "Environment", "Môi trường", "https://cdn.example/x.png", 0, null));
        assertThrows(InvalidDataException.class,
                () -> new VocabularyTopicDraft("env", "Environment", "Môi trường", null, -1, null));

        var topic = new VocabularyTopicDraft("env", "Environment", "Môi trường", " covers/env.png ", null, null);
        assertEquals("covers/env.png", topic.coverKey());
        assertEquals(0, topic.sortOrder());
        assertEquals(ContentStatus.ACTIVE, topic.status());
    }

    @Test
    void bandSetNeedsOnlyACefrLevel() {
        UUID topic = UUID.randomUUID();
        assertThrows(InvalidDataException.class,
                () -> new VocabularySetDraft("B2 words", null, VocabularySetKind.BAND, null, null, null, 0, null));
        assertThrows(InvalidDataException.class,
                () -> new VocabularySetDraft("B2 words", null, VocabularySetKind.BAND, CefrLevel.B2, topic, null, 0, null));
        assertEquals(CefrLevel.B2,
                new VocabularySetDraft("B2 words", null, VocabularySetKind.BAND, CefrLevel.B2, null, null, 0, null).cefrLevel());
    }

    @Test
    void topicSetNeedsOnlyATopic() {
        UUID topic = UUID.randomUUID();
        assertThrows(InvalidDataException.class,
                () -> new VocabularySetDraft("Environment", null, VocabularySetKind.TOPIC, null, null, null, 0, null));
        assertThrows(InvalidDataException.class,
                () -> new VocabularySetDraft("Environment", null, VocabularySetKind.TOPIC, CefrLevel.B1, topic, null, 0, null));
        assertEquals(topic,
                new VocabularySetDraft("Environment", null, VocabularySetKind.TOPIC, null, topic, null, 0, null).topicId());
    }

    @Test
    void setNeedsAKindAndATitle() {
        assertThrows(InvalidDataException.class,
                () -> new VocabularySetDraft("Words", null, null, null, null, null, 0, null));
        assertThrows(InvalidDataException.class,
                () -> new VocabularySetDraft(" ", null, VocabularySetKind.BAND, CefrLevel.A1, null, null, 0, null));
    }

    @Test
    void childListsAreCappedAtThirtyEntries() {
        List<Collocation> thirty = IntStream.range(0, 30).mapToObj(i -> new Collocation("phrase " + i, null, null)).toList();
        List<Collocation> thirtyOne = IntStream.range(0, 31).mapToObj(i -> new Collocation("phrase " + i, null, null)).toList();

        assertEquals(30, CatalogLimits.requireChildren(thirty).size());
        assertThrows(InvalidDataException.class, () -> CatalogLimits.requireChildren(thirtyOne));
        assertThrows(InvalidDataException.class, () -> CatalogLimits.requireChildren(null));
        assertEquals(0, CatalogLimits.requireChildren(List.<Collocation>of()).size());
    }

    @Test
    void setMembershipIsBoundedAndUnique() {
        UUID a = UUID.randomUUID();
        List<UUID> tooMany = IntStream.range(0, 501).mapToObj(i -> UUID.randomUUID()).toList();

        assertThrows(InvalidDataException.class, () -> CatalogLimits.requireSetSenses(List.of(a, a)));
        assertThrows(InvalidDataException.class, () -> CatalogLimits.requireSetSenses(tooMany));
        assertThrows(InvalidDataException.class, () -> CatalogLimits.requireSetSenses(Collections.singletonList(null)));
        assertEquals(List.of(a), CatalogLimits.requireSetSenses(List.of(a)));
    }
}
