package com.syfe.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    @Column(nullable = false)
    private boolean customCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser user;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private int sortOrder;

    protected Category() {
    }

    private Category(String name, CategoryType type, boolean customCategory, AppUser user, int sortOrder) {
        this.name = name;
        this.type = type;
        this.customCategory = customCategory;
        this.user = user;
        this.sortOrder = sortOrder;
    }

    public static Category defaultCategory(String name, CategoryType type, int sortOrder) {
        return new Category(name, type, false, null, sortOrder);
    }

    public static Category customCategory(String name, CategoryType type, AppUser user) {
        return new Category(name, type, true, user, 1_000);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CategoryType getType() {
        return type;
    }

    public boolean isCustomCategory() {
        return customCategory;
    }

    public AppUser getUser() {
        return user;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void markDeleted() {
        this.deleted = true;
    }
}
