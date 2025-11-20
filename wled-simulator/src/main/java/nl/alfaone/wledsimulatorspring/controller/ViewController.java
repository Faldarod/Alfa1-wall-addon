package nl.alfaone.wledsimulatorspring.controller;

import nl.alfaone.wledsimulatorspring.service.WledService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    private final WledService wledService;

    public ViewController(WledService wledService) {
        this.wledService = wledService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("state", wledService.getCurrentState());
        model.addAttribute("info", wledService.getDeviceInfo());
        return "index";
    }

    @GetMapping("/config")
    public String config(Model model) {
        model.addAttribute("state", wledService.getCurrentState());
        model.addAttribute("info", wledService.getDeviceInfo());
        return "config";
    }
}
