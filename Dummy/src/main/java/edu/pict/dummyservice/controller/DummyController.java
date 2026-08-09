package edu.pict.dummyservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DummyController {
    @GetMapping("/")
    public ResponseEntity<String> index() {
        long temp = 0;
        for (int i = 0; i < 1000; i++) {
            temp += i;
        }
        log.info("{}", temp);
        return ResponseEntity.status(HttpStatus.OK).body("Hello World!");
    }

    @GetMapping("/temp")
    public ResponseEntity<String> dummy() {
        return ResponseEntity.status(HttpStatus.OK).body("Service Is running Successfully! ");
    }
}
