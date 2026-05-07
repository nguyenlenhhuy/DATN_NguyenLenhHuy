package org.example.backend.repository;

import org.example.backend.entity.Role;
import org.example.backend.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Tìm kiếm Role dựa trên Enum RoleType
    Optional<Role> findByRoleType(RoleType roleType);
}