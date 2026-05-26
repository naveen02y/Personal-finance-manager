package com.syfe.finance.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syfe.finance.domain.AppUser;
import com.syfe.finance.domain.Category;
import com.syfe.finance.dto.CategoryListResponse;
import com.syfe.finance.dto.CategoryRequest;
import com.syfe.finance.dto.CategoryResponse;
import com.syfe.finance.dto.MessageResponse;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.exception.ConflictException;
import com.syfe.finance.exception.ForbiddenException;
import com.syfe.finance.exception.NotFoundException;
import com.syfe.finance.repository.CategoryRepository;
import com.syfe.finance.repository.TransactionRepository;
import com.syfe.finance.security.CurrentUserService;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository,
            CurrentUserService currentUserService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public CategoryListResponse getCategories() {
        AppUser user = currentUserService.currentUser();
        List<CategoryResponse> responses = new ArrayList<>();
        categoryRepository.findByCustomCategoryFalseAndDeletedFalseOrderBySortOrderAscNameAsc()
            .forEach(category -> responses.add(toResponse(category)));
        categoryRepository.findByUserAndDeletedFalseOrderByNameAsc(user)
            .stream()
            .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
            .forEach(category -> responses.add(toResponse(category)));
        return new CategoryListResponse(responses);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        AppUser user = currentUserService.currentUser();
        String name = normalizeName(request.name());
        if (categoryRepository.existsByNameIgnoreCaseAndCustomCategoryFalseAndDeletedFalse(name)
                || categoryRepository.existsByNameIgnoreCaseAndUserAndDeletedFalse(name, user)) {
            throw new ConflictException("Category name already exists");
        }
        Category saved = categoryRepository.save(Category.customCategory(name, request.type(), user));
        return toResponse(saved);
    }

    @Transactional
    public MessageResponse deleteCategory(String name) {
        AppUser user = currentUserService.currentUser();
        String normalized = normalizeName(name);
        if (categoryRepository.findByNameIgnoreCaseAndCustomCategoryFalseAndDeletedFalse(normalized).isPresent()) {
            throw new BadRequestException("Default categories cannot be deleted");
        }

        Category category = categoryRepository.findByNameIgnoreCaseAndUserAndDeletedFalse(normalized, user)
            .orElseGet(() -> {
                if (categoryRepository.existsByNameIgnoreCaseAndCustomCategoryTrueAndDeletedFalseAndUserNot(normalized,
                        user)) {
                    throw new ForbiddenException("Cannot delete another user's category");
                }
                throw new NotFoundException("Category not found");
            });

        if (transactionRepository.existsByCategoryAndDeletedFalse(category)) {
            throw new BadRequestException("Category is currently referenced by transactions");
        }
        category.markDeleted();
        return new MessageResponse("Category deleted successfully");
    }

    @Transactional(readOnly = true)
    public Category resolveAccessibleCategoryByName(AppUser user, String name) {
        String normalized = normalizeName(name);
        return categoryRepository.findByNameIgnoreCaseAndUserAndDeletedFalse(normalized, user)
            .or(() -> categoryRepository.findByNameIgnoreCaseAndCustomCategoryFalseAndDeletedFalse(normalized))
            .orElseThrow(() -> new BadRequestException("Category not found"));
    }

    @Transactional(readOnly = true)
    public Category resolveAccessibleCategoryById(AppUser user, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("Category not found"));
        if (category.isDeleted()) {
            throw new NotFoundException("Category not found");
        }
        if (!category.isCustomCategory()) {
            return category;
        }
        if (category.getUser().getId().equals(user.getId())) {
            return category;
        }
        throw new ForbiddenException("Cannot access another user's category");
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getType(), category.isCustomCategory());
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Category name is required");
        }
        String normalized = name.trim();
        if (normalized.length() > 80) {
            throw new BadRequestException("Category name must be at most 80 characters");
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
