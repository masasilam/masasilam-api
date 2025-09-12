package com.naskah.demo.exception;

import com.naskah.demo.exception.custom.*;
import com.naskah.demo.model.dto.response.DefaultResponse;
import com.naskah.demo.model.dto.response.ResponseMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandler {

    private static final String ERROR = "Error";

    // General exceptions
    @ExceptionHandler(value = DataNotFoundException.class)
    public ResponseEntity<DefaultResponse> handleDataNotFoundException(DataNotFoundException e) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.DATA_NOT_FOUND, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = NullDataException.class)
    public ResponseEntity<DefaultResponse> handleNullDataException(NullDataException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.NULL_DATA, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = OutOfBoundsException.class)
    public ResponseEntity<DefaultResponse> handleOutOfBoundsException(OutOfBoundsException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.OUT_OF_BOUNDS, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = DataAlreadyExistsException.class)
    public ResponseEntity<DefaultResponse> handleDataAlreadyExistsException(DataAlreadyExistsException e) {
        HttpStatus status = HttpStatus.CONFLICT;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.DATA_ALREADY_EXISTS, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = MissingParameterException.class)
    public ResponseEntity<DefaultResponse> handleMissingParameterException(MissingParameterException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.MISSING_PARAMETER, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = InvalidDataException.class)
    public ResponseEntity<DefaultResponse> handleInvalidDataException(InvalidDataException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INVALID_DATA, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    public ResponseEntity<DefaultResponse> handleUnauthorizedException(UnauthorizedException e) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.UNAUTHORIZED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = ForbiddenException.class)
    public ResponseEntity<DefaultResponse> handleForbiddenException(ForbiddenException e) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.FORBIDDEN, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = ValidationException.class)
    public ResponseEntity<DefaultResponse> handleValidationException(ValidationException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.VALIDATION_ERROR, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = InternalServerErrorException.class)
    public ResponseEntity<DefaultResponse> handleInternalServerErrorException(InternalServerErrorException e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INTERNAL_SERVER_ERROR, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = ServiceUnavailableException.class)
    public ResponseEntity<DefaultResponse> handleServiceUnavailableException(ServiceUnavailableException e) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.SERVICE_UNAVAILABLE, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = BadRequestException.class)
    public ResponseEntity<DefaultResponse> handleBadRequestException(BadRequestException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.BAD_REQUEST, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = MethodNotAllowedException.class)
    public ResponseEntity<DefaultResponse> handleMethodNotAllowedException(MethodNotAllowedException e) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.METHOD_NOT_ALLOWED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = InvalidCredentialsException.class)
    public ResponseEntity<DefaultResponse> handleInvalidCredentialsException(InvalidCredentialsException e) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INVALID_CREDENTIALS, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = AccountLockedException.class)
    public ResponseEntity<DefaultResponse> handleAccountLockedException(AccountLockedException e) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.ACCOUNT_LOCKED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = TokenExpiredException.class)
    public ResponseEntity<DefaultResponse> handleTokenExpiredException(TokenExpiredException e) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.TOKEN_EXPIRED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = InvalidTokenException.class)
    public ResponseEntity<DefaultResponse> handleInvalidTokenException(InvalidTokenException e) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INVALID_TOKEN, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = InsufficientPrivilegesException.class)
    public ResponseEntity<DefaultResponse> handleInsufficientPrivilegesException(InsufficientPrivilegesException e) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INSUFFICIENT_PRIVILEGES, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = LimitExceededException.class)
    public ResponseEntity<DefaultResponse> handleLimitExceededException(LimitExceededException e) {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.LIMIT_EXCEEDED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = DataConflictException.class)
    public ResponseEntity<DefaultResponse> handleConflictException(DataConflictException e) {
        HttpStatus status = HttpStatus.CONFLICT;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.CONFLICT, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = PreconditionFailedException.class)
    public ResponseEntity<DefaultResponse> handlePreconditionFailedException(PreconditionFailedException e) {
        HttpStatus status = HttpStatus.PRECONDITION_FAILED;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.PRECONDITION_FAILED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = UnsupportedMediaTypeException.class)
    public ResponseEntity<DefaultResponse> handleUnsupportedMediaTypeException(UnsupportedMediaTypeException e) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.UNSUPPORTED_MEDIA_TYPE, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = RateLimitExceededException.class)
    public ResponseEntity<DefaultResponse> handleRateLimitExceededException(RateLimitExceededException e) {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.RATE_LIMIT_EXCEEDED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = FileUploadFailedException.class)
    public ResponseEntity<DefaultResponse> handleFileUploadFailedException(FileUploadFailedException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.FILE_UPLOAD_FAILED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = InvalidFileTypeException.class)
    public ResponseEntity<DefaultResponse> handleInvalidFileTypeException(InvalidFileTypeException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INVALID_FILE_TYPE, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = FileSizeExceededException.class)
    public ResponseEntity<DefaultResponse> handleFileSizeExceededException(FileSizeExceededException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.FILE_SIZE_EXCEEDED, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = FileNotFoundException.class)
    public ResponseEntity<DefaultResponse> handleFileNotFoundException(FileNotFoundException e) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.FILE_NOT_FOUND, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = DatabaseConnectionException.class)
    public ResponseEntity<DefaultResponse> handleDatabaseConnectionException(DatabaseConnectionException e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.DB_CONNECTION_ERROR, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = DatabaseQueryException.class)
    public ResponseEntity<DefaultResponse> handleDatabaseQueryException(DatabaseQueryException e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.DB_QUERY_ERROR, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = DatabaseTransactionException.class)
    public ResponseEntity<DefaultResponse> handleDatabaseTransactionException(DatabaseTransactionException e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.DB_TRANSACTION_ERROR, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = DatabaseConstraintViolationException.class)
    public ResponseEntity<DefaultResponse> handleDatabaseConstraintViolationException(DatabaseConstraintViolationException e) {
        HttpStatus status = HttpStatus.CONFLICT;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.DB_CONSTRAINT_VIOLATION, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    // Validation exceptions
    @ExceptionHandler(value = InvalidEmailException.class)
    public ResponseEntity<DefaultResponse> handleInvalidEmailException(InvalidEmailException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INVALID_EMAIL, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = InvalidPhoneException.class)
    public ResponseEntity<DefaultResponse> handleInvalidPhoneException(InvalidPhoneException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INVALID_PHONE, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = PasswordRequirementsException.class)
    public ResponseEntity<DefaultResponse> handlePasswordRequirementsException(PasswordRequirementsException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.PASSWORD_REQUIREMENTS, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = RequiredFieldException.class)
    public ResponseEntity<DefaultResponse> handleRequiredFieldException(RequiredFieldException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.REQUIRED_FIELD, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }

    @ExceptionHandler(value = InvalidDateFormatException.class)
    public ResponseEntity<DefaultResponse> handleInvalidDateFormatException(InvalidDateFormatException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        DefaultResponse response = new DefaultResponse(ERROR, ResponseMessage.INVALID_DATE_FORMAT, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }
}