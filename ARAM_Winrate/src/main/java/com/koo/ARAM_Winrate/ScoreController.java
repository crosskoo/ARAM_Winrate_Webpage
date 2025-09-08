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

    // 사용자가 이름과 태그를 입력했을 때 호출되는 API
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

    // 신규 사용자가 초기 점수를 설정하는 API
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@RequestBody Map<String, String> payload) {
        try {
            String gameName = payload.get("gameName");
            String tagLine = payload.get("tagLine");
            int score = Integer.parseInt(payload.get("score"));
            Map<String, Object> result = scoreService.initialSetup(gameName, tagLine, score);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "설정 중 오류 발생: " + e.getMessage()));
        }
    }

    // 점수 초기화 API
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