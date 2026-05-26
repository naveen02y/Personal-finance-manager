package com.syfe.finance.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syfe.finance.domain.Category;
import com.syfe.finance.domain.CategoryType;
import com.syfe.finance.repository.CategoryRepository;

@Component
public class DefaultCategoryInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public DefaultCategoryInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Category> defaults = List.of(
                Category.defaultCategory("Salary", CategoryType.INCOME, 1),
                Category.defaultCategory("Food", CategoryType.EXPENSE, 2),
                Category.defaultCategory("Rent", CategoryType.EXPENSE, 3),
                Category.defaultCategory("Transportation", CategoryType.EXPENSE, 4),
                Category.defaultCategory("Entertainment", CategoryType.EXPENSE, 5),
                Category.defaultCategory("Healthcare", CategoryType.EXPENSE, 6),
                Category.defaultCategory("Utilities", CategoryType.EXPENSE, 7));

        for (Category category : defaults) {
            if (!categoryRepository.existsByNameIgnoreCaseAndCustomCategoryFalseAndDeletedFalse(category.getName())) {
                categoryRepository.save(category);
            }
        }
    }
}
