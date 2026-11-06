package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpokenWord;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the speech analysis of a whole submission comes to, in the figures the grader and the learner use: how the learner
 * spoke (rhythm), how well (scores out of 100), the Pronunciation band the table gives for it, and the words to practise.
 * Scores are the recordings' scores weighted by their words, so a long answer counts for more than a one-line one.
 *
 * @param pronunciationBand null when nothing was heard, because then there is nothing to grade
 * @param prosody           null when the speech service gave no intonation score
 * @param weakSounds        the sounds weak across the speech, from the sound scores; empty when none were kept
 */
public record SpeakingAnalysisSummary(SpeakingRhythm rhythm, double accuracy, double fluency, double pronunciation,
                                      Double prosody, BigDecimal pronunciationBand, String bandTableVersion,
                                      List<SpokenWord> weakWords, List<WeakSounds.WeakSound> weakSounds) {

    /** How many weak sounds a result lists: a short list a learner can act on. */
    private static final int WEAK_SOUND_LIMIT = 6;

    public SpeakingAnalysisSummary {
        weakWords = List.copyOf(weakWords);
        weakSounds = List.copyOf(weakSounds);
    }

    public static SpeakingAnalysisSummary of(List<SpeechAnalysis> analyses, PronunciationBandMapper mapper, int weakWordLimit) {
        List<SpeechAnalysis> heard = analyses.stream().filter(analysis -> !analysis.isEmpty()).toList();
        SpeakingRhythm rhythm = SpeakingRhythm.combine(heard.stream().map(SpeakingRhythm::of).toList());
        if (heard.isEmpty()) {
            return new SpeakingAnalysisSummary(rhythm, 0, 0, 0, null, null, mapper.version(), List.of(), List.of());
        }
        double words = 0;
        double accuracy = 0;
        double fluency = 0;
        double pronunciation = 0;
        double prosody = 0;
        double prosodyWords = 0;
        for (SpeechAnalysis analysis : heard) {
            double weight = analysis.words().size();
            words += weight;
            accuracy += analysis.accuracy() * weight;
            fluency += analysis.fluency() * weight;
            pronunciation += analysis.pronunciation() * weight;
            if (analysis.prosody() != null) {
                prosody += analysis.prosody() * weight;
                prosodyWords += weight;
            }
        }
        List<SpokenWord> allWords = heard.stream().flatMap(analysis -> analysis.words().stream()).toList();
        double meanPronunciation = pronunciation / words;
        return new SpeakingAnalysisSummary(rhythm, accuracy / words, fluency / words, meanPronunciation,
                prosodyWords == 0 ? null : prosody / prosodyWords, mapper.band(meanPronunciation), mapper.version(),
                WeakWords.select(allWords, WeakWords.WEAK_BELOW, weakWordLimit), WeakSounds.select(allWords, WEAK_SOUND_LIMIT));
    }
}
