package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.library.application.port.CatalogMediaStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import com.ieltspath.library.domain.exception.DuplicateCodeException;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.ContentStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * US or UK pronunciation of a word that is not in the library yet, stored as {@code vocabulary/pronunciation/us|uk/<slug>}.
 * The slug is the normalized lemma with each run of characters outside a-z0-9 turned into a dash, the same naming the
 * existing word files use. A word that already exists is refused, so its file is never overwritten.
 */
@Service
public class UploadPronunciationAudioUseCase {
    static final String KEY_PREFIX = "vocabulary/pronunciation";
    private static final Set<String> ACCENTS = Set.of("us", "uk");
    // A pronunciation clip is a few kilobytes; 2 MB is also the multipart limit of library-service.
    private static final UploadPolicy POLICY = UploadPolicy.of(2L * 1024 * 1024,
            "audio/mpeg", "audio/mp4", "audio/wav", "audio/ogg");

    private final CatalogMediaStorage storage;
    private final VocabularyRepository vocabulary;

    public UploadPronunciationAudioUseCase(CatalogMediaStorage storage, VocabularyRepository vocabulary) {
        this.storage = storage;
        this.vocabulary = vocabulary;
    }

    public StoredImageResult execute(String lemma, String accent, String declaredContentType, byte[] content) {
        if (!ACCENTS.contains(accent)) {
            throw new InvalidDataException("accent must be us or uk");
        }
        String normalized = lemma == null ? "" : lemma.trim().toLowerCase(Locale.ROOT);
        String slug = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            throw new InvalidDataException("lemma needs at least one letter or digit to name its audio file");
        }
        // An INACTIVE word (a dictation pronunciation row) may be created again, so only an active one is refused.
        if (vocabulary.findByNormalizedLemma(normalized).filter(item -> item.getStatus() == ContentStatus.ACTIVE).isPresent()) {
            throw new DuplicateCodeException("VocabularyItem", lemma);
        }
        String contentType = POLICY.check(content, declaredContentType);
        String folder = KEY_PREFIX + "/" + accent + "/";
        String extension = "." + ContentTypes.extensionFor(contentType);
        String key = folder + slug + extension;
        // The bucket never overwrites. A file is already there for a dictation word, or from an upload whose word was
        // never saved; the new file then takes a short suffix and the stored one is left alone.
        if (storage.exists(key)) {
            key = folder + slug + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
        }
        ObjectKeys.requireSafe(key);
        return storage.store(key, content, contentType);
    }
}
