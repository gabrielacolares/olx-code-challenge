package br.com.olx.anuncios.controller;

import br.com.olx.anuncios.dto.AdsResponse;
import br.com.olx.anuncios.dto.AdsRequest;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.mapper.AdsResponseMapper;
import br.com.olx.anuncios.service.AdsService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ads")
public class AdsController {
    @Autowired
    private AdsService adsService;

    @Autowired
    private AdsResponseMapper adsResponseMapper;

    @PostMapping
    public ResponseEntity<AdsResponse> create(@Valid @RequestBody AdsRequest request) {
        Ads createdAds = adsService.create(
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                request.getCategory(),
                request.getImages());
        AdsResponse response = adsResponseMapper.toResponse(createdAds);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
