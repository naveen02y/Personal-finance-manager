package com.syfe.finance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syfe.finance.domain.AppUser;
import com.syfe.finance.domain.SavingsGoal;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserOrderByIdAsc(AppUser user);

    Optional<SavingsGoal> findByIdAndUser(Long id, AppUser user);
}
