package com.fooddelivery.service;

import com.fooddelivery.dto.ChatMessageResponse;
import com.fooddelivery.entity.ChatMessage;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BusinessException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.NotFoundException;
import com.fooddelivery.repository.ChatMessageRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long orderId, Long userId) {
        Order order = findChatOrder(orderId, userId);
        return chatMessageRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(message -> toResponse(message, userId))
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long orderId, Long userId, String content) {
        Order order = findChatOrder(orderId, userId);
        if (order.getStatus() == Order.OrderStatus.COMPLETED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BusinessException("订单已结束，不能继续发送私信");
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (text.length() > 500) {
            throw new BusinessException("消息最多500字");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        User receiver = order.getCustomer().getId().equals(userId) ? order.getRider() : order.getCustomer();
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .order(order)
                .sender(sender)
                .receiver(receiver)
                .content(text)
                .build());
        return toResponse(message, userId);
    }

    private Order findChatOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));
        if (order.getRider() == null) {
            throw new BusinessException("骑手接单后才能私信聊天");
        }
        boolean isCustomer = order.getCustomer().getId().equals(userId);
        boolean isRider = order.getRider().getId().equals(userId);
        if (!isCustomer && !isRider) {
            throw new ForbiddenException("无权查看此订单聊天");
        }
        return order;
    }

    private ChatMessageResponse toResponse(ChatMessage message, Long currentUserId) {
        return new ChatMessageResponse(
                message.getId(),
                message.getOrder().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getSender().getRole().name(),
                message.getReceiver().getId(),
                message.getReceiver().getUsername(),
                message.getContent(),
                message.getSender().getId().equals(currentUserId),
                message.getCreatedAt()
        );
    }
}
