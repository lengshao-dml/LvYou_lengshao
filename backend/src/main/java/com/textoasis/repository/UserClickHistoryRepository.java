package com.textoasis.repository;

import com.textoasis.model.UserClickHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserClickHistoryRepository extends JpaRepository<UserClickHistory, Long> {
}
