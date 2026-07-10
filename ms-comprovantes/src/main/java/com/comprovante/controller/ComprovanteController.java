package com.comprovante.controller;

import com.comprovante.application.service.ComprovanteConsultaService;
import com.comprovante.application.service.ComprovanteService;
import com.comprovante.dto.ComprovanteConsultaResponse;
import com.comprovante.dto.ComprovanteRequest;
import com.comprovante.dto.ComprovanteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comprovantes")
@RequiredArgsConstructor
public class ComprovanteController {

    private final ComprovanteService comprovanteService;
    private final ComprovanteConsultaService comprovanteConsultaService;

    @PostMapping
    public ResponseEntity<ComprovanteResponse> gerarComprovante(@Valid @RequestBody ComprovanteRequest request) {
        ComprovanteResponse response = comprovanteService.solicitarComprovante(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprovanteConsultaResponse> consultarComprovante(@PathVariable("id") String id) {
        ComprovanteConsultaResponse response = comprovanteConsultaService.consultar(id);
        return ResponseEntity.ok(response);
    }
}
