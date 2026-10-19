package com.placement.portal.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    long countByStudentIdAndIsCurrent(String studentId, String isCurrent);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StudentResume r SET r.isCurrent = 'N' WHERE r.studentId = :studentId")
    int deactivateAllForStudent(@Param("studentId") String studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StudentResume r SET r.isCurrent = 'Y' WHERE r.studentId = :studentId AND r.resumeId = :resumeId")
    int activateResumeForStudent(@Param("studentId") String studentId, @Param("resumeId") String resumeId);
}

