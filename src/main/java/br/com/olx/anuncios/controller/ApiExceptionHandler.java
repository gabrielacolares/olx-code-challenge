package br.com.olx.anuncios.controller;

import br.com.olx.anuncios.dto.ErrorResponse;
import br.com.olx.anuncios.exception.ModerationIntegrationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.exc.StreamReadException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(Exception exception) {
        return new ErrorResponse("bad_request", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse unreadableRequest(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof StreamReadException) {
            return new ErrorResponse("invalid_json", "JSON malformado na requisição");
        }
        return new ErrorResponse(
                "validation_error",
                "Corpo da requisição contém valores inválidos",
                List.of("O corpo contém um valor incompatível com o tipo esperado"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validationFailure(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return new ErrorResponse("validation_error", "Erro de validação nos campos", details);
    }

    @ExceptionHandler(ModerationIntegrationException.class)
    public ResponseEntity<Void> moderationFailure(ModerationIntegrationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
}
