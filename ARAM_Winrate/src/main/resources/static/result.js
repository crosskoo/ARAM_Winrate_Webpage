document.addEventListener('DOMContentLoaded', () => {
    const aramData = JSON.parse(sessionStorage.getItem('aram-data'));
    const userData = JSON.parse(sessionStorage.getItem('aram-user'));

    if (!aramData || !userData) {
        window.location.href = 'index.html';
        return;
    }

    const userNameEl = document.getElementById('user-name');
    const userScoreEl = document.getElementById('user-score');
    const updateInfoEl = document.getElementById('update-info');
    const lastMatchInfoEl = document.getElementById('last-match-info');
    const resetButton = document.getElementById('reset-button');
    const loader = document.getElementById('loader');
    const errorMessage = document.getElementById('error-message');

    userNameEl.textContent = `${userData.gameName}#${userData.tagLine}`;

    if (aramData.isInitialized) {
        const { newWins, newLosses, updatedData, lastMatch } = aramData;
        userScoreEl.textContent = updatedData.score;
        updateInfoEl.textContent = `(최근 ${newWins}승 ${newLosses}패 반영)`;

        if (lastMatch) {
            const { championName, kills, deaths, assists, win } = lastMatch;
            const winLossText = win ? '승리' : '패배';
            const winLossClass = win ? 'win' : 'loss';

            lastMatchInfoEl.innerHTML = `
                <h4>최근 경기</h4>
                <div><strong>챔피언:</strong> ${championName}</div>
                <div><strong>KDA:</strong> ${kills} / ${deaths} / ${assists}</div>
                <div><strong>결과:</strong> <span class="result ${winLossClass}">${winLossText}</span></div>
            `;
        }
    } else {
        // 초기 설정 화면 (점수 입력)
        const resultCard = document.getElementById('result-card');
        resultCard.innerHTML = `
            <h2>점수 초기 설정</h2>
            <p>데이터가 없습니다. 현재 점수를 입력해주세요.</p>
            <div class="input-group">
                <input type="number" id="initial-score" placeholder="현재 점수 입력">
            </div>
            <button id="setup-button">설정 완료</button>
        `;

        const setupButton = document.getElementById('setup-button');
        const initialScoreInput = document.getElementById('initial-score');

        setupButton.addEventListener('click', async () => {
            const score = initialScoreInput.value;
            if (!score) {
                errorMessage.textContent = '점수를 입력해주세요.';
                return;
            }

            loader.classList.remove('hidden');
            errorMessage.textContent = '';

            try {
                const response = await fetch('/api/setup', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        gameName: userData.gameName,
                        tagLine: userData.tagLine,
                        score: score
                    }),
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '설정 중 오류 발생');
                }

                // 설정 완료 후 다시 check API를 호출하여 데이터 갱신
                const checkResponse = await fetch(`/api/check?gameName=${encodeURIComponent(userData.gameName)}&tagLine=${encodeURIComponent(userData.tagLine)}`);
                const result = await checkResponse.json();
                sessionStorage.setItem('aram-data', JSON.stringify(result));
                window.location.reload(); // 페이지 새로고침하여 결과 표시

            } catch (error) {
                errorMessage.textContent = error.message;
            } finally {
                loader.classList.add('hidden');
            }
        });
    }

    // 점수 초기화 버튼 이벤트
    resetButton.addEventListener('click', async () => {
        if (!confirm('정말로 점수를 초기화하시겠습니까?')) {
            return;
        }
        loader.classList.remove('hidden');
        errorMessage.textContent = '';
        try {
            const response = await fetch('/api/reset', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    gameName: userData.gameName,
                    tagLine: userData.tagLine,
                }),
            });
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '초기화 중 오류 발생');
            }
            alert('점수가 초기화되었습니다.');
            sessionStorage.removeItem('aram-data');
            window.location.href = 'index.html';
        } catch (error) {
            errorMessage.textContent = error.message;
        } finally {
            loader.classList.add('hidden');
        }
    });
});