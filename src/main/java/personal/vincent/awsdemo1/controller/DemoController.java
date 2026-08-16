package personal.vincent.awsdemo1.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class DemoController {
    
    @GetMapping
    public String sayHello() {
        return "OK";
    }

}
