package com.textoasis.repository;

import com.textoasis.model.User;
import com.textoasis.model.UserRecommendLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRecommendLogRepository extends JpaRepository<UserRecommendLog, Long> {
    List<UserRecommendLog> findByUserOrderByRequestTimeDesc(User user, Pageable pageable);
}
