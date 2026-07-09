package com.pagamento.controller;

import com.pagamento.application.service.PagamentoService;
import com.pagamento.dto.PagamentoRequest;
import com.pagamento.dto.PagamentoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    public ResponseEntity<PagamentoResponse> realizarPagamento(@Valid @RequestBody PagamentoRequest request) {
        PagamentoResponse response = pagamentoService.processarPagamento(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
