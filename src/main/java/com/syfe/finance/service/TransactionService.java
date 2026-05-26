package com.syfe.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syfe.finance.domain.AppUser;
import com.syfe.finance.domain.Category;
import com.syfe.finance.domain.CategoryType;
import com.syfe.finance.domain.FinancialTransaction;
import com.syfe.finance.dto.MessageResponse;
import com.syfe.finance.dto.TransactionListResponse;
import com.syfe.finance.dto.TransactionRequest;
import com.syfe.finance.dto.TransactionResponse;
import com.syfe.finance.dto.TransactionUpdateRequest;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.exception.ForbiddenException;
import com.syfe.finance.exception.NotFoundException;
import com.syfe.finance.repository.TransactionRepository;
import com.syfe.finance.security.CurrentUserService;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public TransactionService(TransactionRepository transactionRepository, CategoryService categoryService,
            CurrentUserService currentUserService, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        AppUser user = currentUserService.currentUser();
        validatePastOrToday(request.date());
        Category category = categoryService.resolveAccessibleCategoryByName(user, request.category());
        FinancialTransaction transaction = new FinancialTransaction(user, category, normalizeAmount(request.amount()),
                request.date(), request.description());
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public TransactionListResponse findTransactions(LocalDate startDate, LocalDate endDate, Long categoryId,
            String categoryName, CategoryType type) {
        AppUser user = currentUserService.currentUser();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }
        Category filterCategory = null;
        if (categoryId != null) {
            filterCategory = categoryService.resolveAccessibleCategoryById(user, categoryId);
        }
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            filterCategory = categoryService.resolveAccessibleCategoryByName(user, categoryName);
        }

        Category resolvedCategory = filterCategory;
        List<TransactionResponse> transactions = transactionRepository.findByUserAndDeletedFalseOrderByDateDescIdDesc(user)
            .stream()
            .filter(transaction -> startDate == null || !transaction.getDate().isBefore(startDate))
            .filter(transaction -> endDate == null || !transaction.getDate().isAfter(endDate))
            .filter(transaction -> resolvedCategory == null
                    || Objects.equals(transaction.getCategory().getId(), resolvedCategory.getId()))
            .filter(transaction -> type == null || transaction.getCategory().getType() == type)
            .sorted(Comparator.comparing(FinancialTransaction::getDate).reversed()
                .thenComparing(FinancialTransaction::getId, Comparator.reverseOrder()))
            .map(this::toResponse)
            .toList();
        return new TransactionListResponse(transactions);
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionUpdateRequest request) {
        AppUser user = currentUserService.currentUser();
        FinancialTransaction transaction = ownedTransaction(id, user);
        if (request.date() != null) {
            throw new BadRequestException("Transaction date cannot be updated");
        }
        if (request.amount() != null) {
            transaction.updateAmount(normalizeAmount(request.amount()));
        }
        if (request.category() != null) {
            transaction.updateCategory(categoryService.resolveAccessibleCategoryByName(user, request.category()));
        }
        if (request.description() != null) {
            transaction.updateDescription(request.description());
        }
        return toResponse(transaction);
    }

    @Transactional
    public MessageResponse delete(Long id) {
        AppUser user = currentUserService.currentUser();
        FinancialTransaction transaction = ownedTransaction(id, user);
        transaction.markDeleted();
        return new MessageResponse("Transaction deleted successfully");
    }

    @Transactional(readOnly = true)
    public List<FinancialTransaction> transactionsBetween(AppUser user, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByUserAndDeletedFalseAndDateBetween(user, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<FinancialTransaction> allActiveTransactions(AppUser user) {
        return transactionRepository.findByUserAndDeletedFalseOrderByDateDescIdDesc(user);
    }

    public TransactionResponse toResponse(FinancialTransaction transaction) {
        return new TransactionResponse(transaction.getId(), normalizeAmount(transaction.getAmount()), transaction.getDate(),
                transaction.getCategory().getName(), transaction.getDescription(), transaction.getCategory().getType());
    }

    private FinancialTransaction ownedTransaction(Long id, AppUser user) {
        return transactionRepository.findById(id).map(transaction -> {
            if (transaction.isDeleted()) {
                throw new NotFoundException("Transaction not found");
            }
            if (!transaction.getUser().getId().equals(user.getId())) {
                throw new ForbiddenException("Cannot access another user's transaction");
            }
            return transaction;
        }).orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    private void validatePastOrToday(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Transaction date cannot be in the future");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
