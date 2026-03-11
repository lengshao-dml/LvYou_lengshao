package com.textoasis.repository;

import com.textoasis.model.CityPopularity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityPopularityRepository extends JpaRepository<CityPopularity, Long> {

    Optional<CityPopularity> findByCityId(Long cityId);

    List<CityPopularity> findByOrderByScoreDesc(Pageable pageable);
}
