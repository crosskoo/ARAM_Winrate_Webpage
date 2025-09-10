package com.koo.ARAM_Winrate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkUser(
            @RequestParam String gameName,
            @RequestParam String tagLine) {
        try {
            Map<String, Object> result = scoreService.checkAndUpdate(gameName, tagLine);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "오류 발생: " + e.getMessage()));
        }
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@RequestBody Map<String, String> payload) {
        try {
            String gameName = payload.get("gameName");
            String tagLine = payload.get("tagLine");
            int score = Integer.parseInt(payload.get("score"));
            int targetScore = Integer.parseInt(payload.get("targetScore"));
            Map<String, Object> result = scoreService.initialSetup(gameName, tagLine, score, targetScore);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "설정 중 오류 발생: " + e.getMessage()));
        }
    }
    
    // targetScore 설정을 위한 새로운 엔드포인트
    @PostMapping("/set-target")
    public ResponseEntity<Map<String, Object>> setTarget(@RequestBody Map<String, String> payload) {
        try {
            String gameName = payload.get("gameName");
            String tagLine = payload.get("tagLine");
            int targetScore = Integer.parseInt(payload.get("targetScore"));
            Map<String, Object> result = scoreService.setTargetScore(gameName, tagLine, targetScore);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "목표 설정 중 오류 발생: " + e.getMessage()));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetScore(@RequestBody Map<String, String> payload) {
        try {
            String gameName = payload.get("gameName");
            String tagLine = payload.get("tagLine");
            Map<String, String> result = scoreService.resetScore(gameName, tagLine);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "초기화 중 오류 발생: " + e.getMessage()));
        }
    }
}