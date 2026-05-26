package com.syfe.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syfe.finance.domain.AppUser;
import com.syfe.finance.domain.CategoryType;
import com.syfe.finance.domain.FinancialTransaction;
import com.syfe.finance.domain.SavingsGoal;
import com.syfe.finance.dto.GoalListResponse;
import com.syfe.finance.dto.GoalRequest;
import com.syfe.finance.dto.GoalResponse;
import com.syfe.finance.dto.GoalUpdateRequest;
import com.syfe.finance.dto.MessageResponse;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.exception.ForbiddenException;
import com.syfe.finance.exception.NotFoundException;
import com.syfe.finance.repository.SavingsGoalRepository;
import com.syfe.finance.security.CurrentUserService;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository, TransactionService transactionService,
            CurrentUserService currentUserService, Clock clock) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional
    public GoalResponse create(GoalRequest request) {
        AppUser user = currentUserService.currentUser();
        LocalDate startDate = request.startDate() == null ? LocalDate.now(clock) : request.startDate();
        validateTargetAmount(request.targetAmount());
        validateTargetDate(request.targetDate(), startDate, request.startDate() != null);
        SavingsGoal saved = savingsGoalRepository.save(new SavingsGoal(user, request.goalName().trim(),
                money(request.targetAmount()), request.targetDate(), startDate));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public GoalListResponse getAll() {
        AppUser user = currentUserService.currentUser();
        List<GoalResponse> goals = savingsGoalRepository.findByUserOrderByIdAsc(user)
            .stream()
            .map(this::toResponse)
            .toList();
        return new GoalListResponse(goals);
    }

    @Transactional(readOnly = true)
    public GoalResponse get(Long id) {
        AppUser user = currentUserService.currentUser();
        return toResponse(ownedGoal(id, user));
    }

    @Transactional
    public GoalResponse update(Long id, GoalUpdateRequest request) {
        AppUser user = currentUserService.currentUser();
        SavingsGoal goal = ownedGoal(id, user);
        if (request.targetAmount() != null) {
            validateTargetAmount(request.targetAmount());
            goal.updateTargetAmount(money(request.targetAmount()));
        }
        if (request.targetDate() != null) {
            validateTargetDate(request.targetDate(), goal.getStartDate(), true);
            goal.updateTargetDate(request.targetDate());
        }
        return toResponse(goal);
    }

    @Transactional
    public MessageResponse delete(Long id) {
        AppUser user = currentUserService.currentUser();
        SavingsGoal goal = ownedGoal(id, user);
        savingsGoalRepository.delete(goal);
        return new MessageResponse("Goal deleted successfully");
    }

    public GoalResponse toResponse(SavingsGoal goal) {
        BigDecimal currentProgress = calculateProgress(goal);
        BigDecimal percentage = currentProgress.multiply(BigDecimal.valueOf(100))
            .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = goal.getTargetAmount().subtract(currentProgress);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        return new GoalResponse(goal.getId(), goal.getGoalName(), money(goal.getTargetAmount()), goal.getTargetDate(),
                goal.getStartDate(), money(currentProgress), percentage, money(remaining));
    }

    private BigDecimal calculateProgress(SavingsGoal goal) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        for (FinancialTransaction transaction : transactionService.allActiveTransactions(goal.getUser())) {
            if (transaction.getDate().isBefore(goal.getStartDate())) {
                continue;
            }
            if (transaction.getCategory().getType() == CategoryType.INCOME) {
                income = income.add(transaction.getAmount());
            }
            else {
                expenses = expenses.add(transaction.getAmount());
            }
        }
        return money(income.subtract(expenses));
    }

    private SavingsGoal ownedGoal(Long id, AppUser user) {
        return savingsGoalRepository.findById(id).map(goal -> {
            if (!goal.getUser().getId().equals(user.getId())) {
                throw new ForbiddenException("Cannot access another user's goal");
            }
            return goal;
        }).orElseThrow(() -> new NotFoundException("Goal not found"));
    }

    private void validateTargetAmount(BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be positive");
        }
    }

    private void validateTargetDate(LocalDate targetDate, LocalDate startDate, boolean explicitStartDate) {
        if (targetDate == null) {
            throw new BadRequestException("Target date is required");
        }
        if (!targetDate.isAfter(startDate)) {
            throw new BadRequestException("Target date must be after start date");
        }
        if (!explicitStartDate && !targetDate.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Target date must be in the future");
        }
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
