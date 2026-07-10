package com.comprovante.application.exception;

public class ComprovanteNaoEncontradoException extends RuntimeException {
    public ComprovanteNaoEncontradoException(String identificador) {
        super("Comprovante nao encontrado: " + identificador);
    }
}
