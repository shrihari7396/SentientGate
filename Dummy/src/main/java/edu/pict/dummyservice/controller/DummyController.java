package edu.pict.dummyservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DummyController {
    @GetMapping("/")
    public String index() {
        long temp = 0;
        for (int i = 0; i < 1000; i++) {
            temp += i;
        }
        log.info("{}", temp);
        return "Hello World!";
    }

    @GetMapping("/dummy")
    public String dummy() {
        return "Service Is running Successfully!    ";
    }
}
