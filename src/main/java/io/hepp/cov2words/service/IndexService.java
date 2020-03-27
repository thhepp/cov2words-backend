package io.hepp.cov2words.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hepp.cov2words.common.dto.WordListDTO;
import io.hepp.cov2words.common.exceptions.language.UnknownLanguageException;
import io.hepp.cov2words.common.util.ResourcesUtils;
import io.hepp.cov2words.domain.entity.AnswerEntity;
import io.hepp.cov2words.domain.entity.AnswerWordMappingEntity;
import io.hepp.cov2words.domain.entity.IndexEntity;
import io.hepp.cov2words.domain.entity.WordEntity;
import io.hepp.cov2words.domain.repository.AnswerWordRepository;
import io.hepp.cov2words.domain.repository.IndexRepository;
import io.hepp.cov2words.domain.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j(topic = "IndexService")
public class IndexService {
    private final static String WORDLIST_FILE_PATTERN = "wordlist/%s.json";
    private final IndexRepository indexRepository;
    private final AnswerWordRepository answerWordRepository;
    private final WordRepository wordRepository;
    private final LanguageService languageService;
    private final WordService wordService;
    private ConcurrentHashMap<String, BigInteger> concurrentHashMap = new ConcurrentHashMap<>();

    @Autowired
    public IndexService(
            IndexRepository indexRepository,
            AnswerWordRepository answerWordRepository,
            WordRepository wordRepository,
            LanguageService languageService,
            WordService wordService
    ) {
        this.indexRepository = indexRepository;
        this.answerWordRepository = answerWordRepository;
        this.wordRepository = wordRepository;
        this.languageService = languageService;
        this.wordService = wordService;
    }

    /**
     * Creates a new word pair in the database.
     */
    public List<AnswerWordMappingEntity> getWordPairForIndex(
            String language,
            String answer
    ) {
        BigInteger currentValue = this.concurrentHashMap.getOrDefault(language, BigInteger.ZERO);
        this.concurrentHashMap.put(language, currentValue.add(BigInteger.ONE));

        List<WordEntity> wordEntities = new ArrayList<>();
        // TODO add the selected words to the list above.
        // TODO implement model here that is used for the calculation.

        // Creating a new mapping.
        DateTime now = DateTime.now();
        AnswerEntity answerEntity = new AnswerEntity(
                UUID.randomUUID(),
                now,
                now,
                null,
                answer,
                null
        );

        List<AnswerWordMappingEntity> result = new ArrayList<>();
        for (int i = 0; i < wordEntities.size(); i++) {
            result.add(
                    new AnswerWordMappingEntity(
                            UUID.randomUUID(),
                            now,
                            now,
                            null,
                            i,
                            wordEntities.get(i),
                            answerEntity
                    )
            );
        }

        return this.answerWordRepository.saveAll(result);
    }

    /**
     * Inits the data in the database and performs an initial setup of the application.
     */
    @PostConstruct
    public void initIndex() {
        log.info("Creating index for all available languages");
        // Getting the available languages.
        this.languageService.getLanguages()
                .stream()
                // Importing the data if necessary.
                .map(this::initDataRepository)
                // Skipping failed imports.
                .filter(Objects::nonNull)
                // Building memory index.
                .forEach(this::buildMemoryIndex);

        log.info("Indices created.");
    }

    private void buildMemoryIndex(IndexEntity indexEntity) {
        this.concurrentHashMap.put(
                indexEntity.getLanguage(),
                indexEntity.getPosition()
        );
    }

    private IndexEntity initDataRepository(String language) {
        log.info("Checking if data was imported for language: {}", language);

        // Checking if data was imported for language.
        Optional<IndexEntity> index = this.indexRepository.findFirstByLanguageAndDateInvalidatedIsNull(language);

        if (index.isPresent()) {
            return index.get();
        } else {
            try {
                return this.importWordsForLanguage(language);
            } catch (JsonProcessingException | UnknownLanguageException e) {
                log.error("An unexpected error occurred when importing data for language: {}", language, e);
            }
        }
        return null;
    }

    private IndexEntity importWordsForLanguage(String language) throws JsonProcessingException, UnknownLanguageException {
        log.info("Importing words for language: {}", language);
        // Loading the file from resources.
        this.wordRepository.deleteAllByLanguage(language);

        String resource = ResourcesUtils.getResourceFileAsString(String.format(WORDLIST_FILE_PATTERN, language));
        WordListDTO wordList = this.getWordListDTO(resource);

        this.wordService.importWordsForLanguage(
                wordList.getTerms(),
                language
        );

        // Creating index entry.
        return this.indexRepository.save(
                new IndexEntity(
                        UUID.randomUUID(),
                        DateTime.now(),
                        DateTime.now(),
                        null,
                        BigInteger.ZERO,
                        wordList.getTerms().size(),
                        language
                )
        );
    }

    private WordListDTO getWordListDTO(String content) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(content, WordListDTO.class);
    }
}