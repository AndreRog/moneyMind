package com.app.exceptions;

/**
 * Application Errors are exposed through our api and follow the Feedzai API guidelines.
 * @param type a URI with the problem description
 * @param title a descriptive title of the problem
 */
public record ApplicationError(String type, String title) {
}
