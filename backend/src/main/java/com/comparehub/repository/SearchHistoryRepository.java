package com.comparehub.repository;

import com.comparehub.model.SearchHistory;
import com.comparehub.model.SearchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SearchHistory> findByUserIdAndSearchTypeOrderByCreatedAtDesc(Long userId, SearchType searchType);

    void deleteByUserId(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
