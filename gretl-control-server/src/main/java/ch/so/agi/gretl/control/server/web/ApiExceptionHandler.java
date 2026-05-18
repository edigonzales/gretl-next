package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.manifest.ManifestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({ManifestException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setDetail(exception.getMessage());
        return problem;
    }
}
