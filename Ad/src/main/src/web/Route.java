package web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class Route {
    @RequestMapping("/")
    public String greetingc() {
        return "asd";
    }

    @ResponseBody
    @RequestMapping("/b")
    public Stu greeting() {
        return new Stu(1,"asd");
    }

}