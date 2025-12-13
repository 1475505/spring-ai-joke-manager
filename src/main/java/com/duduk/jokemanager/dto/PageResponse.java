package com.duduk.jokemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应数据
 */
@Schema(description = "分页响应数据")
public class PageResponse<T> {
    
    @Schema(description = "数据列表")
    private List<T> content;
    
    @Schema(description = "当前页码", example = "0")
    private int page;
    
    @Schema(description = "每页大小", example = "10")
    private int size;
    
    @Schema(description = "总记录数", example = "100")
    private long totalElements;
    
    @Schema(description = "总页数", example = "10")
    private int totalPages;
    
    @Schema(description = "是否首页", example = "true")
    private boolean first;
    
    @Schema(description = "是否末页", example = "false")
    private boolean last;
    
    @Schema(description = "是否为空", example = "false")
    private boolean empty;
    
    public PageResponse() {}
    
    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.empty = page.isEmpty();
    }
    
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }
    
    // Getters and Setters
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
    
    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
}