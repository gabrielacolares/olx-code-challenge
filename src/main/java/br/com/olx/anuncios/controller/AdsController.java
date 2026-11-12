package br.com.olx.anuncios.controller;

import br.com.olx.anuncios.dto.AdsRequest;
import br.com.olx.anuncios.dto.AdsPageResponse;
import br.com.olx.anuncios.dto.AdsResponse;
import br.com.olx.anuncios.mapper.AdsResponseMapper;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsPage;
import br.com.olx.anuncios.service.AdsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

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

    @GetMapping("/{id}")
    public ResponseEntity<AdsResponse> getById(@PathVariable UUID id) {
        Optional<Ads> result = adsService.findById(id);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AdsResponse response = adsResponseMapper.toResponse(result.get());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<AdsPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {

        AdsPage result = adsService.list(page, size, category);
        AdsPageResponse response = adsResponseMapper.toPageResponse(result);
        return ResponseEntity.ok(response);
    }
}
