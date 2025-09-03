import requests
import time

#25.9.3 API KEY
API_KEY = "RGAPI-099ed99e-b49e-46dd-810b-e7ab6fa033d9"

#TEST용 닉네임
NICKNAME = "KFC닭다리도둑"
TAG = "KFC"

#아시아용 API url
ACCOUNT_API_URL = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id"
MATCH_API_URL = "https://asia.api.riotgames.com/lol/match/v5/matches"

#PUUID 가져오기 함수
def get_puuid(game_name, tag_line):
    url = f"{ACCOUNT_API_URL}/{game_name}/{tag_line}?api_key={API_KEY}"
    response = requests.get(url)
    
    if response.status_code == 200:
        print("success")
        return response.json()['puuid']
    else:
        print(f"PUUID 조회 실패: {response.status_code}, {response.json()}")
        return None

get_puuid(NICKNAME,TAG)