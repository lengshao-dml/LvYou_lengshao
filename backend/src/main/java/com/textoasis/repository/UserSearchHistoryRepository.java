package com.textoasis.repository;

import com.textoasis.model.User;
import com.textoasis.model.UserSearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, Long> {
    List<UserSearchHistory> findByUser(User user);
}
