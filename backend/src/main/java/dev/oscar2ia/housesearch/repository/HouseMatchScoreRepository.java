package dev.oscar2ia.housesearch.repository;

import dev.oscar2ia.housesearch.model.HouseMatchScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseMatchScoreRepository extends JpaRepository<HouseMatchScore, Long> {
}
