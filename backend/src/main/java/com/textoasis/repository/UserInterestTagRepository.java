package com.textoasis.repository;

import com.textoasis.model.Tag;
import com.textoasis.model.User;
import com.textoasis.model.UserInterestTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInterestTagRepository extends JpaRepository<UserInterestTag, Long> {
    Optional<UserInterestTag> findByUserAndTag(User user, Tag tag);
}
