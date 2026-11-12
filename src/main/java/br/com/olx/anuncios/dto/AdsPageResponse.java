package br.com.olx.anuncios.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class AdsPageResponse {
    private final List<AdsResponse> items;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;

    public AdsPageResponse(List<AdsResponse> items, int page, int size, long totalItems, int totalPages) {
        this.items = List.copyOf(items);
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }
}
