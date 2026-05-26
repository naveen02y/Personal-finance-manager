package com.syfe.finance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syfe.finance.domain.AppUser;
import com.syfe.finance.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByCustomCategoryFalseAndDeletedFalseOrderBySortOrderAscNameAsc();

    List<Category> findByUserAndDeletedFalseOrderByNameAsc(AppUser user);

    Optional<Category> findByIdAndDeletedFalse(Long id);

    Optional<Category> findByNameIgnoreCaseAndCustomCategoryFalseAndDeletedFalse(String name);

    Optional<Category> findByNameIgnoreCaseAndUserAndDeletedFalse(String name, AppUser user);

    boolean existsByNameIgnoreCaseAndUserAndDeletedFalse(String name, AppUser user);

    boolean existsByNameIgnoreCaseAndCustomCategoryFalseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndCustomCategoryTrueAndDeletedFalseAndUserNot(String name, AppUser user);
}
