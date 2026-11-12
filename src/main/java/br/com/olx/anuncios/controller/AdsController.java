package br.com.olx.anuncios.controller;

import br.com.olx.anuncios.dto.AdsRequest;
import br.com.olx.anuncios.dto.AdsPageResponse;
import br.com.olx.anuncios.dto.AdsResponse;
import br.com.olx.anuncios.mapper.AdsResponseMapper;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsPage;
import br.com.olx.anuncios.service.AdsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Anúncios", description = "Operações de anúncios do marketplace")
@RequestMapping("/api/ads")
public class AdsController {
    @Autowired
    private AdsService adsService;

    @Autowired
    private AdsResponseMapper adsResponseMapper;

    @Operation(summary = "Criar anúncio", description = "Valida e envia o anúncio para moderação antes de persistir.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Anúncio criado e moderado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "502", description = "Serviço de moderação indisponível ou resposta invalida")
    })
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

    @Operation(summary = "Consultar anúncio", description = "Busca um anúncio pelo UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Anuncio encontrado"),
            @ApiResponse(responseCode = "400", description = "UUID invalido"),
            @ApiResponse(responseCode = "404", description = "Anuncio nao encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdsResponse> getById(@Parameter(description = "UUID do anuncio", example = "00000000-0000-0000-0000-000000000001") @PathVariable UUID id) {

        Optional<Ads> result = adsService.findById(id);

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AdsResponse response = adsResponseMapper.toResponse(result.get());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar anúncios", description = "Lista anúncios por pagina, opcionalmente filtrados por categoria. A ordenação e por UUID crescente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de anúncios"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos")
    })
    @GetMapping
    public ResponseEntity<AdsPageResponse> list(
            @Parameter(description = "Pagina base zero", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de anúncios por pagina (1 a 100)", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtro opcional por categoria, sem diferenciar maiusculas/minusculas") @RequestParam(required = false) String category) {

        AdsPage result = adsService.list(page, size, category);
        AdsPageResponse response = adsResponseMapper.toPageResponse(result);

        return ResponseEntity.ok(response);
    }
}
