package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.ChatMessageResponse;
import com.fooddelivery.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders/{orderId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping
    public ApiResponse<List<ChatMessageResponse>> getMessages(@PathVariable Long orderId,
                                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(chatMessageService.getMessages(orderId, userId));
    }

    @PostMapping
    public ApiResponse<ChatMessageResponse> sendMessage(@PathVariable Long orderId,
                                                        @RequestBody Map<String, String> body,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(chatMessageService.sendMessage(orderId, userId, body.get("content")));
    }
}
