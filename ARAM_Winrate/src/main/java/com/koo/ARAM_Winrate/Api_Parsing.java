package com.koo.ARAM_Winrate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Api_Parsing {

    // 1. 설정 변수
    
    //이거 application.properties에서 가져오는 것으로 변경해야 할듯.
    private static final String API_KEY = "RGAPI-099ed99e-b49e-46dd-810b-e7ab6fa033d9"; 
    private static final String GAME_NAME = "KFC닭다리도둑";
    private static final String TAG_LINE = "KFC";

    private static final String ASIA_ACCOUNT_API_URL = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id";
    private static final String ASIA_MATCH_API_URL = "https://asia.api.riotgames.com/lol/match/v5/matches";
    
    // main 메서드: 프로그램의 시작점
    public static void main(String[] args) {
        System.out.println("'" + GAME_NAME + "#" + TAG_LINE + "' 님의 전적 조회를 시작합니다.");

        try {
            // 1단계: PUUID 조회
            String puuid = getPuuid(GAME_NAME, TAG_LINE);
            if (puuid == null) {
                System.out.println("PUUID를 찾을 수 없습니다. 닉네임과 태그를 확인해주세요.");
                return;
            }
            System.out.println("PUUID 조회 성공: " + puuid);

            // 2단계: 칼바람 나락 경기 ID 목록 조회 (최신 5경기)
            List<String> matchIds = getAramMatchIds(puuid);
            if (matchIds.isEmpty()) {
                System.out.println("최근 칼바람 나락 경기가 없습니다.");
                return;
            }

            // 3단계: 각 경기의 승패 분석
            getAndPrintWinLoss(puuid, matchIds);

        } catch (IOException | InterruptedException e) {
            System.err.println("API 요청 중 에러가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // PUUID를 가져오는 메서드
    public static String getPuuid(String gameName, String tagLine) throws IOException, InterruptedException {
        String url = String.format("%s/%s/%s?api_key=%s", ASIA_ACCOUNT_API_URL, gameName, tagLine, API_KEY);
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject obj = new JSONObject(response.body());
            return obj.getString("puuid");
        }
        return null;
    }

    // 칼바람 나락 경기 ID 목록을 가져오는 메서드
    public static List<String> getAramMatchIds(String puuid) throws IOException, InterruptedException {
        String url = String.format("%s/by-puuid/%s/ids?queue=450&start=0&count=5&api_key=%s", ASIA_MATCH_API_URL, puuid, API_KEY);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        List<String> matchIdList = new ArrayList<>();
        if (response.statusCode() == 200) {
            JSONArray arr = new JSONArray(response.body());
            for (int i = 0; i < arr.length(); i++) {
                matchIdList.add(arr.getString(i));
            }
        }
        return matchIdList;
    }

    // 승패를 분석하고 출력하는 메서드
    public static void getAndPrintWinLoss(String puuid, List<String> matchIds) throws IOException, InterruptedException {
        int wins = 0;
        int losses = 0;
        System.out.println("총 " + matchIds.size() + "개의 경기 데이터를 분석합니다...");

        for (int i = 0; i < matchIds.size(); i++) {
            String matchId = matchIds.get(i);
            // API 호출 제한을 준수하기 위해 1.2초 대기
            TimeUnit.MILLISECONDS.sleep(1200);

            String url = String.format("%s/%s?api_key=%s", ASIA_MATCH_API_URL, matchId, API_KEY);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject matchData = new JSONObject(response.body());
                JSONArray participants = matchData.getJSONObject("info").getJSONArray("participants");

                for (int j = 0; j < participants.length(); j++) {
                    JSONObject participant = participants.getJSONObject(j);
                    if (puuid.equals(participant.getString("puuid"))) {
                        boolean isWin = participant.getBoolean("win");
                        if (isWin) {
                            wins++;
                        } else {
                            losses++;
                        }
                        System.out.printf("[%d/%d] 경기 분석 완료: %s -> %s%n", i + 1, matchIds.size(), matchId, isWin ? "승리" : "패배");
                        break;
                    }
                }
            } else {
                 System.out.printf("[%d/%d] 경기 %s 정보 조회 실패: %d%n", i + 1, matchIds.size(), matchId, response.statusCode());
            }
        }

        System.out.println("\n--- 최종 결과 ---");
        System.out.println("분석한 경기 수: " + matchIds.size() + " 게임");
        System.out.println("승리: " + wins + "회");
        System.out.println("패배: " + losses + "회");
    }
}