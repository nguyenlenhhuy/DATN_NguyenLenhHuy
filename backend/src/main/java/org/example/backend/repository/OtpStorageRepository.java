package org.example.backend.repository;
import org.example.backend.entity.OtpStorage;
import org.example.backend.entity.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpStorageRepository extends JpaRepository<OtpStorage, Long> {
    // Tìm OTP mới nhất theo email và loại OTP
    Optional<OtpStorage> findFirstByEmailAndOtpTypeOrderByExpiryTimeDesc(String email, OtpType otpType);
    Optional<OtpStorage> findFirstByEmailAndOtpCodeAndOtpTypeOrderByExpiryTimeDesc(
            String email, String otpCode, OtpType otpType);
  void deleteByEmail(String email);
}