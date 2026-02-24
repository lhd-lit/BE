package LDHD.project.domain.selfStudy.repository;

import LDHD.project.domain.selfStudy.SelfStudy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SelfStudyRepository extends JpaRepository<SelfStudy,Long> {
    @Query("SELECT s FROM SelfStudy s JOIN FETCH s.uploader WHERE s.uploader.id = :userId")
    Page<SelfStudy> findAllByUser_Id(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM SelfStudy s JOIN FETCH s.uploader")
    Page<SelfStudy> findAll(Pageable pageable);

    // 마지막 조회 시간 기준 내림차순 (미조회 항목 제외)
    @Query("SELECT s FROM SelfStudy s JOIN FETCH s.uploader " + "WHERE s.uploader.id = :userId AND s.lastViewedAt IS NOT NULL " +
            "ORDER BY s.lastViewedAt DESC")

    Page<SelfStudy>findAllByUserIdOrderByLastViewedAt(Long userId, Pageable pageable);
    // 가장 최근 조회한 SelfStudy 1개 반환
    Optional<SelfStudy>findTopByUploader_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(Long userId);
}
