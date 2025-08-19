package cz.martinzajdlik.recappy_book.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> hello() {
        return Map.of("status", "ok", "app", "recappy-book");
    }
}
