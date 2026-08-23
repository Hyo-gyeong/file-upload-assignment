package com.fileupload.policy.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fileupload.policy.domain.FileExtensionPolicy;
import com.fileupload.policy.domain.PolicyKind;

public interface FileExtensionPolicyRepository
    extends JpaRepository<FileExtensionPolicy, Long> {

    Optional<FileExtensionPolicy> findByExtension(String extension);

    boolean existsByExtension(String extension);

    long countByPolicyKind(PolicyKind policyKind);

    Page<FileExtensionPolicy> findByPolicyKind(
        PolicyKind policyKind,
        Pageable pageable
    );

    List<FileExtensionPolicy> findAllByPolicyKindOrderByIdAsc(
        PolicyKind policyKind
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update FileExtensionPolicy p
           set p.blocked = :blocked,
               p.updatedBy = :actorUserId
         where p.id = :id
           and p.policyKind = :policyKind
           and p.updatedAt = :expectedUpdatedAt
        """) // DB UPDATE 순간까지 동시성 조건을 보장
    int updateBlockedIfUnchanged(
        @Param("id") Long id,
        @Param("policyKind") PolicyKind policyKind,
        @Param("blocked") boolean blocked,
        @Param("actorUserId") Long actorUserId,
        @Param("expectedUpdatedAt") OffsetDateTime expectedUpdatedAt
    );
    
    @Query("""
	    select p
	      from FileExtensionPolicy p
	     where p.policyKind = :policyKind
	       and (:search = '' or p.extension like concat('%', :search, '%'))
	       and (:initial = '' or p.extension like concat(:initial, '%'))
	    """)
	Page<FileExtensionPolicy> searchByPolicyKind(
	    @Param("policyKind") PolicyKind policyKind,
	    @Param("search") String search,
	    @Param("initial") String initial,
	    Pageable pageable
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
	    delete from FileExtensionPolicy p
	     where p.id = :id
	       and p.policyKind = :policyKind
	       and p.updatedAt = :expectedUpdatedAt
	    """)
	int deleteIfUnchanged(
	    @Param("id") Long id,
	    @Param("policyKind") PolicyKind policyKind,
	    @Param("expectedUpdatedAt") OffsetDateTime expectedUpdatedAt
	);
}