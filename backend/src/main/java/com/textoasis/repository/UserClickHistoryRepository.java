package com.textoasis.repository;

import com.textoasis.model.User;
import com.textoasis.model.UserClickHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserClickHistoryRepository extends JpaRepository<UserClickHistory, Long> {
    List<UserClickHistory> findByUser(User user);
}
