package uk.gov.dwp.uc.pairtest.exception;

public class InvalidAccountIdException extends RuntimeException{
    public InvalidAccountIdException(String message) {
        super(message);
    }
}