package com.placement.portal.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentResumeRepository extends JpaRepository<StudentResume, String> {
    List<StudentResume> findByStudentIdOrderByVersionNoDesc(String studentId);
    Optional<StudentResume> findByStudentIdAndIsCurrent(String studentId, String isCurrent);
    Optional<StudentResume> findFirstByStudentIdAndIsCurrentOrderByVersionNoDesc(String studentId, String isCurrent);
    List<StudentResume> findAllByStudentIdAndIsCurrent(String studentId, String isCurrent);
    Optional<StudentResume> findTopByStudentIdOrderByVersionNoDesc(String studentId);
}
