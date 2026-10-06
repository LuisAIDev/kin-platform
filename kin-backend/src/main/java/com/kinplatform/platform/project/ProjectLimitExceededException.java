package com.kinplatform.platform.project;

public class ProjectLimitExceededException extends RuntimeException {
    public ProjectLimitExceededException(String message) {
        super(message);
    }
}

