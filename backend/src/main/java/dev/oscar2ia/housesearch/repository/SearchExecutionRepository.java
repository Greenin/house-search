package dev.oscar2ia.housesearch.repository;

import dev.oscar2ia.housesearch.model.SearchExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchExecutionRepository extends JpaRepository<SearchExecution, Long> {
}
