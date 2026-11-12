package br.com.olx.anuncios.mapper;

import br.com.olx.anuncios.dto.AdsPageResponse;
import br.com.olx.anuncios.dto.AdsResponse;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsPage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdsResponseMapper {

    AdsResponse toResponse(Ads ads);

    default AdsPageResponse toPageResponse(AdsPage adsPage) {

        List<AdsResponse> items = adsPage.getAdsList()
                .stream()
                .map(this::toResponse)
                .toList();

        AdsPageResponse response = new AdsPageResponse(
                items,
                adsPage.getPage(),
                adsPage.getSize(),
                adsPage.getTotalItems(),
                adsPage.getTotalPages());

        return response;
    }
}
