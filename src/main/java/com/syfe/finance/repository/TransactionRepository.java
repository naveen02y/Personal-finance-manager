package com.syfe.finance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syfe.finance.domain.AppUser;
import com.syfe.finance.domain.Category;
import com.syfe.finance.domain.FinancialTransaction;

public interface TransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findByUserAndDeletedFalseOrderByDateDescIdDesc(AppUser user);

    List<FinancialTransaction> findByUserAndDeletedFalseAndDateBetween(AppUser user, LocalDate startDate,
            LocalDate endDate);

    Optional<FinancialTransaction> findByIdAndUserAndDeletedFalse(Long id, AppUser user);

    boolean existsByCategoryAndDeletedFalse(Category category);
}
