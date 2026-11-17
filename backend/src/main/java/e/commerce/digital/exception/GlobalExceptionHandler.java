package e.commerce.digital.exception;

import e.commerce.digital.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * Traduce las excepciones del service a respuestas uniformes (ErrorResponse).
 * No se maneja AccessDeniedException a propósito: Spring Security responde 401 o 403 según corresponda.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> autenticacionInvalida(AuthenticationException ex) {
        return responder(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Incluye StockInsuficiente, ProductoNoDisponible, TransicionInvalida y PagoYaProcesado
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({OperacionInvalidaException.class, ImagenInvalidaException.class})
    public ResponseEntity<ErrorResponse> solicitudInvalida(RuntimeException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ServicioImagenesException.class)
    public ResponseEntity<ErrorResponse> servicioImagenes(ServicioImagenesException ex) {
        log.error("Falla al comunicarse con Cloudinary", ex);
        return responder(HttpStatus.BAD_GATEWAY, "El servicio de imágenes no está disponible. Intente más tarde");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Datos inválidos", detalles));
    }

    // JSON mal formado, tipos incorrectos o un valor de enum inexistente en el cuerpo
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido o está mal formado");
    }

    // Un parámetro de la URL con formato incorrecto: id no numérico, estado o rol inexistente, fecha inválida
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El valor del parámetro '" + ex.getName() + "' no es válido");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> archivoDemasiadoGrande(MaxUploadSizeExceededException ex) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (5 MB)");
    }

    // Red de seguridad ante duplicados concurrentes (por ejemplo, dos registros simultáneos con el mismo email)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos", ex);
        return responder(HttpStatus.CONFLICT,
                "La operación viola una restricción de los datos (por ejemplo, un valor duplicado)");
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), mensaje));
    }
}
