package com.eventflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 80, message = "Category name must be between 2 and 80 characters")
    private String name;

    private String description;
    private String iconName;

    public CreateCategoryRequest() {}

    public CreateCategoryRequest(String name, String description, String iconName) {
        this.name = name;
        this.description = description;
        this.iconName = iconName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
}
