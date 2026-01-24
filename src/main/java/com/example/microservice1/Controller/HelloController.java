package com.example.microservice1.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HelloController {
    @GetMapping("/")

    public String sayHello(){
        return "Hello WolfDire! your controller is now located and nad";

    }

}
