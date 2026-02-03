package edu.pict.dummyservice.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DummyController {
    @GetMapping("/dummy")
    public String dummy() {
        return "Service Is running Successfully!    ";
    }
}
