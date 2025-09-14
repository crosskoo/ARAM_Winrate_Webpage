package com.koo.ARAM_Winrate;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ScoreService {

    // 환경 변수에서 Riot API 키를 가져옵니다.
    private String apiKey = System.getenv("RIOT_API_KEY");

    private static final String ASIA_ACCOUNT_API_URL = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id";
    private static final String ASIA_MATCH_API_URL = "https://asia.api.riotgames.com/lol/match/v5/matches";
    
    // Spring Data JPA Repository를 주입받습니다.
    @Autowired
    private UserDataRepository userDataRepository;

    public Map<String, Object> checkAndUpdate(String gameName, String tagLine) throws IOException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        // DB에서 사용자 정보를 조회합니다.
        Optional<UserData> userDataOptional = userDataRepository.findByGameNameAndTagLine(gameName, tagLine);

        if (userDataOptional.isPresent()) {
            UserData userData = userDataOptional.get();
            // 목표 점수가 설정되지 않은 경우
            if (userData.getTargetScore() == 0) {
                response.put("isInitialized", true);
                response.put("needsTargetScore", true);
                response.put("updatedData", userData);
            } else {
                response = updateScore(gameName, tagLine);
                response.put("isInitialized", true);
                response.put("needsTargetScore", false);
            }
        } else {
            // 사용자가 존재하지 않으면 초기화되지 않은 상태로 응답
            response.put("isInitialized", false);
        }
        return response;
    }

    // 신규 사용자용: 점수와 목표를 DB에 저장합니다.
    public Map<String, Object> initialSetup(String gameName, String tagLine, int score, int targetScore) {
        UserData userData = new UserData();
        userData.setGameName(gameName);
        userData.setTagLine(tagLine);
        userData.setInitialScore(0); // 시작 점수는 항상 0
        userData.setScore(score);    // 현재 점수는 사용자가 입력한 값
        userData.setTargetScore(targetScore);
        userData.setLastUpdatedTimestamp(System.currentTimeMillis() / 1000);
        
        // JpaRepository를 통해 DB에 저장
        userDataRepository.save(userData);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "초기 설정 완료");
        response.put("userData", userData);
        return response;
    }
    
    // 기존 사용자용: 목표 점수를 설정합니다.
    public Map<String, Object> setTargetScore(String gameName, String tagLine, int targetScore) {
        // orElseThrow를 사용하여 사용자가 없을 경우 예외를 발생시킵니다.
        UserData userData = userDataRepository.findByGameNameAndTagLine(gameName, tagLine)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        userData.setTargetScore(targetScore);
        if (userData.getInitialScore() == 0) {
            userData.setInitialScore(0);
        }
        
        userDataRepository.save(userData);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "목표 점수 설정 완료");
        response.put("userData", userData);
        return response;
    }

    private Map<String, Object> updateScore(String gameName, String tagLine) throws IOException, InterruptedException {
        UserData userData = userDataRepository.findByGameNameAndTagLine(gameName, tagLine)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        int currentScore = userData.getScore();
        long lastUpdatedTimestamp = userData.getLastUpdatedTimestamp();

        String puuid = getPuuid(gameName, tagLine);
        if (puuid == null) throw new RuntimeException("PUUID를 찾을 수 없습니다.");

        List<String> newMatchIds = getAramMatchIdsAfter(puuid, lastUpdatedTimestamp, 0);

        int newWins = 0;
        int newLosses = 0;
        if (!newMatchIds.isEmpty()) {
            int[] newResults = getWinLoss(puuid, newMatchIds);
            newWins = newResults[0];
            newLosses = newResults[1];
            currentScore += (newWins - newLosses);

            userData.setScore(currentScore);
            userData.setLastUpdatedTimestamp(System.currentTimeMillis() / 1000);
            userDataRepository.save(userData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("newWins", newWins);
        response.put("newLosses", newLosses);
        response.put("updatedData", userData);
        response.put("lastMatch", getLastMatchInfo(puuid));
        return response;
    }
    
    public Map<String, String> resetScore(String gameName, String tagLine) {
        Optional<UserData> userDataOptional = userDataRepository.findByGameNameAndTagLine(gameName, tagLine);
        if (userDataOptional.isPresent()) {
            // DB에서 해당 사용자 데이터를 삭제합니다.
            userDataRepository.delete(userDataOptional.get());
            return Map.of("message", "점수 초기화 완료");
        }
        return Map.of("message", "저장된 데이터가 없습니다.");
    }
    
    // ▼▼▼ Riot API와 통신하는 private 메서드들은 수정할 필요가 없습니다. ▼▼▼

    private Map<String, Object> getLastMatchInfo(String puuid) throws IOException, InterruptedException {
        List<String> lastMatchIdList = getAramMatchIdsAfter(puuid, 0, 1);
        if (lastMatchIdList.isEmpty()) {
            return null;
        }
        String lastMatchId = lastMatchIdList.get(0);
        String url = String.format("%s/%s?api_key=%s", ASIA_MATCH_API_URL, lastMatchId, apiKey);
        HttpResponse<String> response = sendRequest(url);

        if (response.statusCode() == 200) {
            JSONObject matchData = new JSONObject(response.body());
            JSONArray participants = matchData.getJSONObject("info").getJSONArray("participants");

            for (int i = 0; i < participants.length(); i++) {
                JSONObject p = participants.getJSONObject(i);
                if (puuid.equals(p.getString("puuid"))) {
                    Map<String, Object> matchInfo = new HashMap<>();
                    matchInfo.put("championName", p.getString("championName"));
                    matchInfo.put("kills", p.getInt("kills"));
                    matchInfo.put("deaths", p.getInt("deaths"));
                    matchInfo.put("assists", p.getInt("assists"));
                    matchInfo.put("win", p.getBoolean("win"));
                    return matchInfo;
                }
            }
        }
        return null;
    }

    private String getPuuid(String gameName, String tagLine) throws IOException, InterruptedException {
        String encodedGameName = URLEncoder.encode(gameName, StandardCharsets.UTF_8);
        String encodedTagLine = URLEncoder.encode(tagLine, StandardCharsets.UTF_8);

        String url = String.format("%s/%s/%s?api_key=%s", ASIA_ACCOUNT_API_URL, encodedGameName, encodedTagLine, apiKey);
        HttpResponse<String> response = sendRequest(url);
        return (response.statusCode() == 200) ? new JSONObject(response.body()).getString("puuid") : null;
    }

    private List<String> getAramMatchIdsAfter(String puuid, long startTimeSeconds, int count) throws IOException, InterruptedException {
        List<String> allMatchIds = new ArrayList<>();
        int start = 0;
        int totalCount = (count == 0) ? 100 : count;

        while (true) {
            String url = String.format("%s/by-puuid/%s/ids?queue=450&startTime=%d&start=%d&count=%d&api_key=%s",
                    ASIA_MATCH_API_URL, puuid, startTimeSeconds, start, totalCount, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JSONArray arr = new JSONArray(response.body());
                if (arr.length() == 0) break;
                for (int i = 0; i < arr.length(); i++) allMatchIds.add(arr.getString(i));

                if (count != 0 || arr.length() < 100) break;
                start += 100;

            } else {
                break;
            }
            // Riot API의 Rate Limits를 준수하기 위한 대기 시간
            TimeUnit.MILLISECONDS.sleep(1200);
        }
        return allMatchIds;
    }

    private int[] getWinLoss(String puuid, List<String> matchIds) throws IOException, InterruptedException {
        int wins = 0, losses = 0;
        for (String matchId : matchIds) {
            TimeUnit.MILLISECONDS.sleep(1200);
            String url = String.format("%s/%s?api_key=%s", ASIA_MATCH_API_URL, matchId, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JSONArray participants = new JSONObject(response.body()).getJSONObject("info").getJSONArray("participants");
                for (int i = 0; i < participants.length(); i++) {
                    JSONObject p = participants.getJSONObject(i);
                    if (puuid.equals(p.getString("puuid"))) {
                        if (p.getBoolean("win")) wins++; else losses++;
                        break;
                    }
                }
            }
        }
        return new int[]{wins, losses};
    }

    private HttpResponse<String> sendRequest(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}