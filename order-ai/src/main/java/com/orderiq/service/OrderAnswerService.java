package com.orderiq.service;

import com.orderiq.api.model.AskOrderRequest;
import com.orderiq.api.model.AskOrderResponse;

public interface OrderAnswerService {
     AskOrderResponse findAnswer(AskOrderRequest askOrderRequest);
}
