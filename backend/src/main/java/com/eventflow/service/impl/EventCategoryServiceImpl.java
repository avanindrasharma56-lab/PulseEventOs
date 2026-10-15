package com.eventflow.service.impl;

import com.eventflow.dto.CategoryResponse;
import com.eventflow.dto.CreateCategoryRequest;
import com.eventflow.entity.EventCategory;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.EventCategoryRepository;
import com.eventflow.service.EventCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventCategoryServiceImpl implements EventCategoryService {

    private final EventCategoryRepository categoryRepository;

    public EventCategoryServiceImpl(EventCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        EventCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        EventCategory category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return mapToResponse(category);
    }

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        String baseSlug = generateSlug(request.getName());
        String slug = baseSlug;
        int count = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + count++;
        }

        EventCategory category = new EventCategory(
                request.getName(),
                slug,
                request.getDescription(),
                request.getIconName()
        );
        category.setActive(true);

        EventCategory saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Override
    public CategoryResponse toggleActive(Long id) {
        EventCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        category.setActive(!category.isActive());
        return mapToResponse(categoryRepository.save(category));
    }

    private String generateSlug(String input) {
        if (input == null) return "category";
        String normalized = input.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return normalized.isEmpty() ? "category" : normalized;
    }

    private CategoryResponse mapToResponse(EventCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIconName(),
                category.isActive()
        );
    }
}
