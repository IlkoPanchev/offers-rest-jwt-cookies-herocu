package cars.config;

import cars.utils.message.MessageResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.net.ConnectException;


@ControllerAdvice
public class GlobalAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalAdvice.class);

    @ExceptionHandler({EntityNotFoundException.class,
            ConstraintViolationException.class})
    public ResponseEntity<?> handleError(RuntimeException ex){

        String message = ex.getMessage();

        LOGGER.error(message);

        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }
}
