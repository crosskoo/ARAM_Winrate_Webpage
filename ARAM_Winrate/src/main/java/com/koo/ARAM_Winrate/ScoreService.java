package com.koo.ARAM_Winrate;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class ScoreService {
    @Value("${riot.api.key}")
    private String apiKey;

    private static final String ASIA_ACCOUNT_API_URL = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id";
    private static final String ASIA_MATCH_API_URL = "https://asia.api.riotgames.com/lol/match/v5/matches";
    private static final String USER_DATA_FOLDER = "user_data";

    private Path getUserDataFilePath(String gameName, String tagLine) {
        try {
            Files.createDirectories(Paths.get(USER_DATA_FOLDER));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Paths.get(USER_DATA_FOLDER, "user_data_" + gameName + "_" + tagLine + ".properties");
    }

    public Map<String, Object> checkAndUpdate(String gameName, String tagLine) throws IOException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        Path userDataFile = getUserDataFilePath(gameName, tagLine);

        if (Files.exists(userDataFile)) {
            Properties userData = loadUserData(gameName, tagLine);
            // targetScore가 있는지 확인
            if (userData.getProperty("targetScore") == null) {
                response.put("isInitialized", true);
                response.put("needsTargetScore", true); // targetScore가 필요하다는 플래그
                response.put("updatedData", loadUserDataAsMap(gameName, tagLine));
            } else {
                response = updateScore(gameName, tagLine);
                response.put("isInitialized", true);
                response.put("needsTargetScore", false);
            }
        } else {
            response.put("isInitialized", false);
        }
        return response;
    }

    public Map<String, Object> initialSetup(String gameName, String tagLine, int score, int targetScore) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        Properties userData = new Properties();
        userData.setProperty("gameName", gameName);
        userData.setProperty("tagLine", tagLine);
        userData.setProperty("initialScore", String.valueOf(score));
        userData.setProperty("score", String.valueOf(score));
        userData.setProperty("targetScore", String.valueOf(targetScore));
        userData.setProperty("lastUpdatedTimestamp", String.valueOf(currentTimestamp));
        saveUserData(gameName, tagLine, userData);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "초기 설정 완료");
        response.put("userData", loadUserDataAsMap(gameName, tagLine));
        return response;
    }
    
    // 기존 사용자의 targetScore를 설정하는 메서드
    public Map<String, Object> setTargetScore(String gameName, String tagLine, int targetScore) {
        Properties userData = loadUserData(gameName, tagLine);
        userData.setProperty("targetScore", String.valueOf(targetScore));
        // 목표 점수 설정 시, 현재 점수를 초기 점수로 설정
        userData.setProperty("initialScore", userData.getProperty("score"));
        saveUserData(gameName, tagLine, userData);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "목표 점수 설정 완료");
        response.put("userData", loadUserDataAsMap(gameName, tagLine));
        return response;
    }

    private Map<String, Object> updateScore(String gameName, String tagLine) throws IOException, InterruptedException {
        Properties userData = loadUserData(gameName, tagLine);
        int currentScore = Integer.parseInt(userData.getProperty("score"));
        long lastUpdatedTimestamp = Long.parseLong(userData.getProperty("lastUpdatedTimestamp"));

        String puuid = getPuuid(gameName, tagLine);
        if (puuid == null) throw new RuntimeException("PUUID를 찾을 수 없습니다.");

        List<String> newMatchIds = getAramMatchIdsAfter(puuid, lastUpdatedTimestamp, 0);

        int newWins = 0;
        int newLosses = 0;
        if (!newMatchIds.isEmpty()) {
            int[] newResults = getWinLoss(puuid, newMatchIds);
            newWins = newResults[0];
            newLosses = newResults[1];
            int scoreChange = newWins - newLosses;
            currentScore += scoreChange;

            userData.setProperty("score", String.valueOf(currentScore));
            userData.setProperty("lastUpdatedTimestamp", String.valueOf(System.currentTimeMillis() / 1000));
            saveUserData(gameName, tagLine, userData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("newWins", newWins);
        response.put("newLosses", newLosses);
        response.put("updatedData", loadUserDataAsMap(gameName, tagLine));
        response.put("lastMatch", getLastMatchInfo(puuid));
        return response;
    }

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

    public Map<String, String> resetScore(String gameName, String tagLine) throws IOException {
        Path userDataFile = getUserDataFilePath(gameName, tagLine);
        if(Files.exists(userDataFile)){
            Files.delete(userDataFile);
            return Map.of("message", "점수 초기화 완료");
        }
        return Map.of("message", "저장된 데이터가 없습니다.");
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

    private Properties loadUserData(String gameName, String tagLine) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(getUserDataFilePath(gameName, tagLine).toFile())) {
            properties.load(in);
        } catch (IOException e) { /* Ignore */ }
        return properties;
    }

    private Map<String, String> loadUserDataAsMap(String gameName, String tagLine) {
        Properties props = loadUserData(gameName, tagLine);
        Map<String, String> map = new HashMap<>();
        for (final String name: props.stringPropertyNames()) {
            map.put(name, props.getProperty(name));
        }
        return map;
    }

    private void saveUserData(String gameName, String tagLine, Properties properties) {
        try (FileOutputStream out = new FileOutputStream(getUserDataFilePath(gameName, tagLine).toFile())) {
            properties.store(out, "User ARAM Score Data");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}