package com.jobhunt.tracker.common.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public sealed class AppException extends RuntimeException
        permits AppException.NotFoundException,
        AppException.BadRequestException,
        AppException.UnauthorizedException,
        AppException.ForbiddenException,
        AppException.ConflictException,
        AppException.InternalServerException {

    private final HttpStatus status;
    private final String errorCode;

    private AppException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static final class NotFoundException
            extends AppException {
        public NotFoundException(String message) {
            super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
        }
    }

    public static final class BadRequestException
            extends AppException {
        public BadRequestException(String message) {
            super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
        }
    }

    public static final class UnauthorizedException
            extends AppException {
        public UnauthorizedException(String message) {
            super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
        }
    }

    public static final class ForbiddenException
            extends AppException {
        public ForbiddenException(String message) {
            super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
        }
    }

    public static final class ConflictException
            extends AppException {
        public ConflictException(String message) {
            super(HttpStatus.CONFLICT, "CONFLICT", message);
        }
    }

    public static final class InternalServerException
            extends AppException {
        public InternalServerException(String message) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message);
        }
    }

    public static NotFoundException notFound(String message) {
        return new NotFoundException(message);
    }

    public static BadRequestException badRequest(String message) {
        return new BadRequestException(message);
    }

    public static UnauthorizedException unauthorized(String message) {
        return new UnauthorizedException(message);
    }

    public static ForbiddenException forbidden(String message) {
        return new ForbiddenException(message);
    }

    public static ConflictException conflict(String message) {
        return new ConflictException(message);
    }

    public static InternalServerException internalError(String message) {
        return new InternalServerException(message);
    }
}