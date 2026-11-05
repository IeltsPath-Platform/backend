package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient.Rubric;
import com.ieltspath.learning.application.port.LearningContentClient.RubricBandDescriptor;
import com.ieltspath.learning.application.port.LearningContentClient.RubricCriterion;
import com.ieltspath.learning.application.service.SpeakingHistorySummarizer.CatalogPoint;
import com.ieltspath.learning.application.service.SpeakingHistorySummarizer.LearnerContext;
import com.ieltspath.learning.domain.service.PronunciationBandMapper;
import com.ieltspath.learning.domain.service.SpeakingAnalysisSummary;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpeechSegment;
import com.ieltspath.learning.domain.vo.SpokenPhoneme;
import com.ieltspath.learning.domain.vo.SpokenWord;
import com.ieltspath.learning.domain.vo.TranscriptQuality;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Builders shared by the Speaking grading tests. */
final class SpeakingTestData {
    static final UUID FC_KP = UUID.fromString("00000000-0000-4000-8000-0000000000f1");
    static final UUID PR_KP = UUID.fromString("00000000-0000-4000-8000-0000000000f4");
    static final UUID GRAMMAR_KP = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    static final String SENTENCE = "I think my teacher was very kind because she taught me English every day";

    private SpeakingTestData() { }

    static Rubric rubric() {
        return new Rubric("IELTS_SPEAKING", "SPEAKING", 1, "test", "Be a careful examiner.",
                List.of(criterion("FC", "Fluency and Coherence", FC_KP), criterion("LR", "Lexical Resource", null),
                        criterion("GRA", "Grammatical Range and Accuracy", null), criterion("PR", "Pronunciation", PR_KP)),
                List.of());
    }

    static RubricCriterion criterion(String code, String name, UUID knowledgePointId) {
        return new RubricCriterion(code, name, name + " description", 1,
                List.of(new RubricBandDescriptor(new BigDecimal("4.0"), "weak"),
                        new RubricBandDescriptor(new BigDecimal("6.0"), "fair")), knowledgePointId, List.of());
    }

    /** {@code count} words spoken one after another with the given pronunciation accuracy. */
    static SpeechAnalysis analysis(String text, double accuracy, Double confidence) {
        String[] tokens = text.split(" ");
        List<SpokenWord> words = new ArrayList<>();
        for (int index = 0; index < tokens.length; index++) {
            words.add(new SpokenWord(tokens[index], index * 400L, 350, accuracy, "None"));
        }
        return new SpeechAnalysis(List.of(new SpeechSegment(text, accuracy, accuracy, null, 100, accuracy, confidence, words)));
    }

    /** "I think my teacher was very kind thanks": two words with weak sounds, /θ/ heard as /s/ in both. */
    static SpeechAnalysis analysisWithSounds() {
        List<SpokenWord> words = List.of(
                new SpokenWord("I", 0, 200, 97, "None", List.of(new SpokenPhoneme("aɪ", 97))),
                new SpokenWord("think", 300, 400, 52, "Mispronunciation", List.of(new SpokenPhoneme("θ", 25, "s"),
                        new SpokenPhoneme("ɪ", 90), new SpokenPhoneme("ŋ", 95), new SpokenPhoneme("k", 90))),
                new SpokenWord("my", 800, 200, 95, "None", List.of(new SpokenPhoneme("m", 95), new SpokenPhoneme("aɪ", 95))),
                new SpokenWord("thanks", 1100, 500, 48, "Mispronunciation", List.of(new SpokenPhoneme("θ", 20, "s"),
                        new SpokenPhoneme("æ", 80), new SpokenPhoneme("ŋ", 85), new SpokenPhoneme("k", 90), new SpokenPhoneme("s", 90))));
        return new SpeechAnalysis(List.of(new SpeechSegment("I think my thanks", 74, 90, null, 100, 74, 0.95, words)));
    }

    static SpeakingGradingInput input(SpeakingScope scope, int[] parts, SpeechAnalysis analysis) {
        List<SpeakingGradingInput.Answer> answers = new ArrayList<>();
        for (int part : parts) answers.add(new SpeakingGradingInput.Answer(part, 1, "Tell me about a teacher.", analysis));
        return input(scope, answers, analysis, LearnerContext.empty());
    }

    static SpeakingGradingInput input(SpeakingScope scope, List<SpeakingGradingInput.Answer> answers, SpeechAnalysis analysis,
                                      LearnerContext learner) {
        List<SpeechAnalysis> all = answers.stream().map(SpeakingGradingInput.Answer::analysis).toList();
        return new SpeakingGradingInput(scope, answers, TranscriptQuality.of(all),
                SpeakingAnalysisSummary.of(all, PronunciationBandMapper.INTERIM, 10), learner,
                List.of(new SpeakingGradingInput.ErrorOption("SPK_NO_LINKER", "No linking words")), 60_000);
    }

    static LearnerContext learnerWithGrammarPoint() {
        return new LearnerContext(List.of(new CatalogPoint(GRAMMAR_KP, "S65_GRAMMAR_RANGE", "Use complex sentences")),
                List.of());
    }

    /** A model answer that is valid for {@link #SENTENCE}. */
    static String goodAnswer(String fc, String lr, String gra) {
        return """
                {"criteria":[
                 {"code":"FC","band":%s,"comment":"ok","strengths":["a"],"weaknesses":["b"],
                  "evidence":[{"quote":"my teacher was very kind","note":"clear"},{"quote":"never said this","note":"x"}]},
                 {"code":"LR","band":%s,"comment":"ok"},
                 {"code":"GRA","band":%s,"comment":"ok"},
                 {"code":"PR","band":9,"comment":"ignored"}],
                 "flags":{"onTopic":true,"englishOnly":true,"tooShort":false},
                 "transcriptReliability":{"level":"HIGH","reason":"clean"},
                 "summary":"Good.","focus":["Use linkers"],
                 "errors":[{"code":"SPK_NO_LINKER","partNo":1,"excerpt":"she taught me English","suggestion":"add because"},
                           {"code":"SPK_MADE_UP","partNo":1,"excerpt":"she taught me English","suggestion":"x"}],
                 "kpObservations":[{"kpCode":"S65_GRAMMAR_RANGE","stance":"WEAKNESS","partNo":1,
                                    "excerpt":"she taught me English","reason":"simple"}]}
                """.formatted(fc, lr, gra);
    }
}
