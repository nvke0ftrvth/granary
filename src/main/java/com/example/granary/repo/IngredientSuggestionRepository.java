package com.example.granary.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.example.granary.model.IngredientSuggestion;

@RepositoryRestResource(exported = false)
public interface IngredientSuggestionRepository extends JpaRepository<IngredientSuggestion, Long> {

    Optional<IngredientSuggestion> findFirstByNameIgnoreCase(String name);

    /**
     * Case-insensitive substring search ranked: name starts with the term, then a word starts
     * with it, then any other match. {@code term} must already be lowercased and LIKE-escaped.
     */
    @Query("""
            SELECT i FROM IngredientSuggestion i
            WHERE LOWER(i.name) LIKE CONCAT('%', :term, '%') ESCAPE '!'
            ORDER BY
                CASE
                    WHEN LOWER(i.name) LIKE CONCAT(:term, '%') ESCAPE '!' THEN 0
                    WHEN LOWER(i.name) LIKE CONCAT('% ', :term, '%') ESCAPE '!' THEN 1
                    ELSE 2
                END,
                LENGTH(i.name),
                i.name
            """)
    List<IngredientSuggestion> search(@Param("term") String term, Pageable pageable);
}
