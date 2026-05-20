package com.microservicesfinal.exceptions;

public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String resourceName){
        super(resourceName + " not found in the database");
    }
}
