package com.example.bmi.controller;

import com.example.bmi.bmi.service.BmiService;
import com.example.bmi.bmi.service.GoogleQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class BmiController {

    @Autowired
    private BmiService bmiService;

    @Autowired
    private GoogleQuery googleQuery;

    @PostMapping("/calculate")
    public String calculateBmi(@RequestParam("height") double height,
                               @RequestParam("weight") double weight,
                               Model model) {
        double bmi = bmiService.calculateBmi(height, weight);
        String category = bmiService.getBmiCategory(bmi);
        model.addAttribute("bmi", bmi);
        model.addAttribute("category", category);
        return "result";
    }

    // API 端點 - 回傳 JSON 格式的 BMI 結果
    @GetMapping("/api/bmi")
    @ResponseBody
    public BmiResponse getBmi(@RequestParam("height") double height,
                              @RequestParam("weight") double weight) {
        double bmi = bmiService.calculateBmi(height, weight);
        String category = bmiService.getBmiCategory(bmi);
        return new BmiResponse(bmi, category);
    }

    // ✅ 新增：Google 搜尋 API 端點
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<Map<String, String>> search(@RequestParam("q") String query) {
        Map<String, String> results = googleQuery.queryByApi(query);
        return ResponseEntity.ok(results);
    }

    // BMI 回傳物件
    public static class BmiResponse {
        private double bmi;
        private String category;

        public BmiResponse(double bmi, String category) {
            this.bmi = bmi;
            this.category = category;
        }

        public double getBmi() {
            return bmi;
        }

        public String getCategory() {
            return category;
        }
    }
}
