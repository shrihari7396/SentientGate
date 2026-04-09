package edu.pict.loggingservice.controller;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final GatewayLogRepository gatewayLogRepository;

    @GetMapping("/raw")
    public ResponseEntity<Page<GatewayLogEntity>> getRawLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Integer statusCode) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(
                gatewayLogRepository.findWithFilters(path, statusCode, pageRequest));
    }
}
