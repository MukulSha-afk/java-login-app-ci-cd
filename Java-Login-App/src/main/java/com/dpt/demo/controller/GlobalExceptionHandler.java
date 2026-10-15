package com.dpt.demo.controller;

import com.dpt.demo.dto.UserRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Centralized error handling for all controllers.
 *
 * <p>Purpose: unexpected failures (database down, driver errors, unexpected
 * runtime exceptions) must never surface stack traces, SQL fragments or driver
 * messages to the browser. The user receives a stable, friendly message on the
 * {@code fail} page while the full context is logged server-side only (logs are
 * not readable from outside the instance).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Database/driver failures: friendly message, full details logged server-side. */
    @ExceptionHandler(DataAccessException.class)
    public ModelAndView handleDataAccess(DataAccessException ex) {
        log.error("Database access failed: {}", ex.getMessage(), ex);
        ModelAndView mav = new ModelAndView("fail");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorMessage",
                "We could not complete that request because a storage problem occurred. "
                        + "Please try again in a moment.");
        return mav;
    }

    /** Wrong HTTP method (e.g. POST to a GET-only route): honest 405, not a 500. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method {} is not supported: {}", ex.getMethod(), ex.getMessage());
        ModelAndView mav = new ModelAndView("fail");
        mav.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
        mav.addObject("errorMessage",
                "That action is not available on this page. Please go back and try again.");
        return mav;
    }

    /** Anything unexpected: never expose internals to the client. */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex) {
        log.error("Unhandled request failure: {}", ex.getMessage(), ex);
        ModelAndView mav = new ModelAndView("fail");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorMessage",
                "Something went wrong while handling your request. Please try again.");
        return mav;
    }
}
