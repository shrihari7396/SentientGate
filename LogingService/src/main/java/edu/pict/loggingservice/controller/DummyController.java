package edu.pict.loggingservice.controller;

import edu.pict.loggingservice.repository.GatewayLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dum")
public class DummyController {
    private final GatewayLogRepository  gatewayLogRepository;

    @GetMapping("/giveAll")
    public ResponseEntity<?> giveAll() {
        return ResponseEntity.ok(gatewayLogRepository.findAll());
    }
}
