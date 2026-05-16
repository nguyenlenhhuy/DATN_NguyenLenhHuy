package org.example.backend.repository;

import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.backend.entity.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false " +
            "AND u.role.roleType = :roleType " +
            "AND (:keyword IS NULL OR u.fullName LIKE %:keyword% OR u.email LIKE %:keyword% OR u.phone LIKE %:keyword%)")
    Page<User> searchUsersForAdmin(@Param("roleType") RoleType roleType,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);
}