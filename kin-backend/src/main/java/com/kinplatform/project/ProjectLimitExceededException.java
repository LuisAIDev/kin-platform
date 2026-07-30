package com.kinplatform.project;

public class ProjectLimitExceededException extends RuntimeException {
    public ProjectLimitExceededException(String message) {
        super(message);
    }
}
