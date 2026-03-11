package com.textoasis.repository;

import com.textoasis.model.UserSearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, Long> {
}
