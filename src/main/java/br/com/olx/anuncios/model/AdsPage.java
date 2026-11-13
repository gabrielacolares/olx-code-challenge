package br.com.olx.anuncios.model;

import java.util.List;
import lombok.Getter;

@Getter
public class AdsPage {
    private final List<Ads> adsList;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;

    public AdsPage(List<Ads> list, int page, int size, long totalItems, int totalPages) {
        this.adsList = List.copyOf(list);
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }
}
