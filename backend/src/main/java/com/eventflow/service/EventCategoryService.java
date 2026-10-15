package com.eventflow.service;

import com.eventflow.dto.CategoryResponse;
import com.eventflow.dto.CreateCategoryRequest;

import java.util.List;

public interface EventCategoryService {
    List<CategoryResponse> getActiveCategories();
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryById(Long id);
    CategoryResponse getCategoryBySlug(String slug);
    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse toggleActive(Long id);
}
