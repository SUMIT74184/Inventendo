package com.example.microservice1.Controller;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseStatus(HttpStatus.OK)
public class InventoryController {
    @GetMapping("/")
    public boolean IsStock(){
    return true;
    }
}
