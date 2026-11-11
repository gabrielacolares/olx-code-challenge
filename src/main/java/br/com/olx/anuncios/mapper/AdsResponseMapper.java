package br.com.olx.anuncios.mapper;

import br.com.olx.anuncios.dto.AdsPageResponse;
import br.com.olx.anuncios.dto.AdsResponse;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsPage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdsResponseMapper {
    AdsResponse toResponse(Ads ads);
}
