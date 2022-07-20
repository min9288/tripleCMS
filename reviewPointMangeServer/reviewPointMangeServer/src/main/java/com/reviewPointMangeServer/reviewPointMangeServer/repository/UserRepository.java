package com.reviewPointMangeServer.reviewPointMangeServer.repository;

import com.reviewPointMangeServer.reviewPointMangeServer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByUserId(UUID userId);

    Optional<User> findUserByUserId(UUID userId);

}
