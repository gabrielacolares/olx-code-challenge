package br.com.olx.anuncios.model;

import lombok.Getter;
import java.util.List;

@Getter
public class AdsPage {
    private final List<Ads> ads;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;

    public AdsPage(List<Ads> ads, int page, int size, long totalItems, int totalPages) {
        this.ads = List.copyOf(ads);
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }
}
