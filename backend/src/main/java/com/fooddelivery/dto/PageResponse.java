package com.fooddelivery.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        PageResponse<T> resp = new PageResponse<>();
        resp.content = page.getContent();
        resp.page = page.getNumber();
        resp.size = page.getSize();
        resp.totalElements = page.getTotalElements();
        resp.totalPages = page.getTotalPages();
        resp.first = page.isFirst();
        resp.last = page.isLast();
        return resp;
    }
}
