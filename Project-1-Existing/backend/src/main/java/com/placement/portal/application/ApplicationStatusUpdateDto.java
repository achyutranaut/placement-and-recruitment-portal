package com.placement.portal.application;

import jakarta.validation.constraints.NotBlank;

public class ApplicationStatusUpdateDto {
    @NotBlank(message = "Status is required")
    private String status;

    public ApplicationStatusUpdateDto() {}

    public ApplicationStatusUpdateDto(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
