package UpLearn.eci.edu.co.config;

public class UserServiceException extends RuntimeException {
    /**
     * Constructs a new UserServiceException with the specified error message.
     *
     * @param message the detail message describing the specific user service error
     */
    public UserServiceException(String message) {
        super(message);
    }
}

