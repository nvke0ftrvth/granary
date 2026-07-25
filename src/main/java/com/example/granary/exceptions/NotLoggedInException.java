package com.example.granary.exceptions;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;

/**
 * AuthenticationFailedException
 */
public class NotLoggedInException extends AuthenticationException {

    public NotLoggedInException(@Nullable String msg) {
        super(msg);
    }

}
