package com.orderiq.api.model;

import jakarta.validation.constraints.NotBlank;

public record AskOrderRequest(@NotBlank(message = "question must not be blank")
                                      String question) {
}
