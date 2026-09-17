package vn.itstar.services;

public class ResourceNotFoundException extends IllegalArgumentException {
    public ResourceNotFoundException(String message) { super(message); }
}
