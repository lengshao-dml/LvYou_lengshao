package com.textoasis.repository;

import com.textoasis.model.UserRecommendLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRecommendLogRepository extends JpaRepository<UserRecommendLog, Long> {
}
