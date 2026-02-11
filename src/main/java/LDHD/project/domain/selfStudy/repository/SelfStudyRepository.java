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
    Page<SelfStudy> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM SelfStudy s JOIN FETCH s.uploader")
    Page<SelfStudy> findAll(Pageable pageable);
}
