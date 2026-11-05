package com.ieltspath.library.application.usecase;
import com.ieltspath.library.domain.repository.WordlyVocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
@Service
public class GetRandomWordlyWordsUseCase {
    private final WordlyVocabularyRepository words;
    public GetRandomWordlyWordsUseCase(WordlyVocabularyRepository words) { this.words=words; }
    @Transactional(readOnly=true)
    public List<WordlyVocabularyRepository.Word> page(String after,int limit) {
        return page(after,limit,null,null);
    }
    @Transactional(readOnly=true)
    public List<WordlyVocabularyRepository.Word> page(String after,int limit,String cefrLevel,UUID topicId) {
        if(limit<1||limit>500)throw new IllegalArgumentException("Page limit must be 1-500");
        if(after==null||!after.matches("[A-Z]{0,100}"))throw new IllegalArgumentException("Invalid cursor");
        return words.page(after,limit,cefrLevel,topicId);
    }
    @Transactional(readOnly=true)
    public List<WordlyVocabularyRepository.Topic> topics() {
        return words.topics();
    }
    @Transactional(readOnly=true)
    public List<WordlyVocabularyRepository.Availability> availability() {
        return words.availability();
    }
    @Transactional(readOnly=true)
    public List<WordlyVocabularyRepository.Word> execute(int count) {
        if(count<1||count>100)throw new IllegalArgumentException("Word count must be 1-100");
        return words.random(count);
    }
}
