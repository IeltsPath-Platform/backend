package com.ieltspath.library.application.usecase;
import com.ieltspath.library.domain.repository.WordlyVocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class GetRandomWordlyWordsUseCase {
    private final WordlyVocabularyRepository words;
    public GetRandomWordlyWordsUseCase(WordlyVocabularyRepository words) { this.words=words; }
    @Transactional(readOnly=true)
    public List<WordlyVocabularyRepository.Word> page(String after,int limit) {
        if(limit<1||limit>500)throw new IllegalArgumentException("Page limit must be 1–500");
        if(after==null||!after.matches("[A-Z]{0,100}"))throw new IllegalArgumentException("Invalid cursor");
        return words.page(after,limit);
    }
    @Transactional(readOnly=true)
    public List<WordlyVocabularyRepository.Word> execute(int count) {
        if(count<1)throw new IllegalArgumentException("Word count must be positive");
        return words.random(count);
    }
}
