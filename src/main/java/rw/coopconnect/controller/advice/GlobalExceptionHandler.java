package rw.coopconnect.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import rw.coopconnect.exception.DuplicateResourceException;
import rw.coopconnect.exception.InsufficientStockException;
import rw.coopconnect.exception.PaymentException;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.exception.UnauthorizedAccessException;

/**
 * Global exception handler providing user-friendly error views and proper HTTP status codes.
 * NFR-01: OWASP - Prevents leaking stack traces to clients.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model, HttpServletRequest request) {
        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorized(UnauthorizedAccessException ex, Model model, HttpServletRequest request) {
        log.warn("Unauthorized access at {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("status", 403);
        model.addAttribute("error", "Forbidden");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/403";
    }

    @ExceptionHandler({InsufficientStockException.class, PaymentException.class, DuplicateResourceException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessException(RuntimeException ex, Model model, HttpServletRequest request) {
        log.warn("Business rule violation at {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("error", "Bad Request");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/500";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String handleMaxSize(MaxUploadSizeExceededException ex, Model model, HttpServletRequest request) {
        log.warn("Max upload size exceeded at {}", request.getRequestURI());
        model.addAttribute("status", 413);
        model.addAttribute("error", "Payload Too Large");
        model.addAttribute("message", "Uploaded file exceeds the maximum allowed size (5MB).");
        model.addAttribute("path", request.getRequestURI());
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model, HttpServletRequest request) {
        log.error("Unhandled exception at " + request.getRequestURI(), ex);
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", "An unexpected error occurred. Please try again or contact support.");
        model.addAttribute("path", request.getRequestURI());
        return "error/500";
    }
}
