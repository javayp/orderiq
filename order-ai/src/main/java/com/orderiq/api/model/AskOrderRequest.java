package com.orderiq.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskOrderRequest(@NotBlank(message = "question must not be blank")
@Size(max = 1000, message = "Question cannot exceed more than 1000 characters")
                              String question) {
}
