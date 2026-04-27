package com.textoasis.repository;

import com.textoasis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.interestTags uit LEFT JOIN FETCH uit.tag WHERE u.username = :username")
    Optional<User> findByUsernameWithInterests(String username);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
