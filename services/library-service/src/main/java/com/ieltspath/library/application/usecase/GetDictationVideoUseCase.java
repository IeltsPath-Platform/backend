package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.DictationVideoDetailResult;
import com.ieltspath.library.domain.aggregate.LearningVideo;
import com.ieltspath.library.domain.exception.VideoNotFoundException;
import com.ieltspath.library.domain.repository.DictationVideoRepository;
import com.ieltspath.library.domain.repository.WordPronunciationRepository;
import com.ieltspath.library.domain.vo.SentenceWords;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDictationVideoUseCase {
    private final DictationVideoRepository videoRepository;
    private final WordPronunciationRepository pronunciationRepository;

    /**
     * A published video with its sentences. Each sentence carries the pronunciation of each of its words; the words of
     * all the sentences are looked up together, in one query.
     */
    public DictationVideoDetailResult execute(UUID videoId) {
        LearningVideo video = videoRepository.findPublishedById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));

        Set<String> words = new LinkedHashSet<>();
        video.getSegments().forEach(s -> words.addAll(SentenceWords.normalized(s.getTranscript())));
        Map<String, String> ipaByWord = pronunciationRepository.findIpaByWords(words);

        return new DictationVideoDetailResult(video.getId(), video.getYoutubeVideoId(), video.getYoutubeUrl(),
                video.getTitle(), video.getDescription(), video.getThumbnailUrl(), video.getDurationSeconds(),
                video.getLevel(),
                video.getSegments().stream()
                        .map(s -> new DictationVideoDetailResult.Segment(s.getId(), s.getSequenceNo(), s.getStartMs(),
                                s.getEndMs(), s.getTranscript(), s.getTranslationVi(),
                                ipaOf(s.getTranscript(), ipaByWord)))
                        .toList());
    }

    /** One entry per word of the sentence, in order; null where the word has no pronunciation. */
    private static List<String> ipaOf(String transcript, Map<String, String> ipaByWord) {
        List<String> ipa = new ArrayList<>();
        for (String word : SentenceWords.normalized(transcript)) {
            ipa.add(ipaByWord.get(word));
        }
        return ipa;
    }
}
