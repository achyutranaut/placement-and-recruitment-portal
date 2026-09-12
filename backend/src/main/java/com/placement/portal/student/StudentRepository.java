package com.placement.portal.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByRegistrationNo(String registrationNo);
    boolean existsByEmail(String email);
    boolean existsByRegistrationNo(String registrationNo);
    List<Student> findByCgpaGreaterThanEqual(BigDecimal minCgpa);
    List<Student> findByCityIgnoreCase(String city);
}
