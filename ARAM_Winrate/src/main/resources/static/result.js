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
    
    // 레이아웃 제어를 위해 왼쪽 background-pane 요소 가져오기
    const backgroundPane = document.querySelector('.background-pane');
    const imageBackground = document.getElementById('image-background');
    const videoContainer = document.getElementById('video-background-container');
    const video = document.getElementById('complete-video');

    const updateBackground = (progress) => {
        let imageUrl = '';
        videoContainer.classList.add('hidden');
        imageBackground.style.backgroundImage = '';
        imageBackground.classList.remove('hidden');

        if (progress < 25) {
            imageUrl = 'url("/images/progress_1.png")';
        } else if (progress < 50) {
            imageUrl = 'url("/images/progress_2.png")';
        } else if (progress < 75) {
            imageUrl = 'url("/images/progress_3.png")';
        } else if (progress < 100) {
            imageUrl = 'url("/images/progress_4.png")';
        } else {
            imageBackground.classList.add('hidden');
            videoContainer.classList.remove('hidden');
            video.play();
            return;
        }
        imageBackground.style.backgroundImage = imageUrl;
    };

    if (aramData.isInitialized) {
        if (aramData.needsTargetScore) {
            // ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ 수정된 부분 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
            backgroundPane.style.display = 'none'; // 왼쪽 배경 영역 숨기기
            // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

            const currentScore = aramData.updatedData.score;
            const resultCard = document.getElementById('result-card');
            resultCard.innerHTML = `
                <h2>목표 점수 설정</h2>
                <p>현재 점수: ${currentScore || 0}</p>
                <p>새로운 목표 점수를 설정해주세요.</p>
                <div class="input-group">
                    <input type="number" id="target-score" placeholder="목표 점수 입력">
                </div>
                <button id="set-target-button">목표 설정</button>
            `;

            const setTargetButton = document.getElementById('set-target-button');
            const targetScoreInput = document.getElementById('target-score');

            setTargetButton.addEventListener('click', async () => {
                const targetScore = targetScoreInput.value;
                if (!targetScore) {
                    errorMessage.textContent = '목표 점수를 입력해주세요.';
                    return;
                }
                if (parseInt(currentScore || 0) >= parseInt(targetScore)) {
                    errorMessage.textContent = '목표 점수는 현재 점수보다 높아야 합니다.';
                    return;
                }

                loader.classList.remove('hidden');
                errorMessage.textContent = '';

                try {
                    const response = await fetch('/api/set-target', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            gameName: userData.gameName,
                            tagLine: userData.tagLine,
                            targetScore: targetScore,
                        }),
                    });

                    if (!response.ok) {
                        const errorData = await response.json();
                        throw new Error(errorData.message || '설정 중 오류 발생');
                    }

                    const checkResponse = await fetch(`/api/check?gameName=${encodeURIComponent(userData.gameName)}&tagLine=${encodeURIComponent(userData.tagLine)}`);
                    const result = await checkResponse.json();
                    sessionStorage.setItem('aram-data', JSON.stringify(result));
                    window.location.reload();

                } catch (error) {
                    errorMessage.textContent = error.message;
                } finally {
                    loader.classList.add('hidden');
                }
            });

        } else {
            userNameEl.textContent = `${userData.gameName}#${userData.tagLine}`;
            const { newWins, newLosses, updatedData, lastMatch } = aramData;
            const initialScore = parseFloat(updatedData.initialScore);
            const currentScore = parseFloat(updatedData.score);
            const targetScore = parseFloat(updatedData.targetScore);

            let progress = 0;
            const goalRange = targetScore - initialScore;
            const currentProgress = currentScore - initialScore;

            if (goalRange > 0) {
                progress = (currentProgress / goalRange) * 100;
            } else if (currentScore >= targetScore) {
                progress = 100;
            }

            progress = Math.max(0, Math.min(100, progress));
            
            userScoreEl.textContent = `${currentScore} / ${targetScore}점`;
            
            updateInfoEl.textContent = `(최근 ${newWins}승 ${newLosses}패 반영)`;
            updateBackground(progress);

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
        }
    } else {
        // ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ 수정된 부분 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
        backgroundPane.style.display = 'none'; // 왼쪽 배경 영역 숨기기
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        const resultCard = document.getElementById('result-card');
        resultCard.innerHTML = `
            <h2>점수 초기 설정</h2>
            <p>데이터가 없습니다. 현재 점수와 목표 점수를 입력해주세요.</p>
            <div class="input-group">
                <input type="number" id="current-score" placeholder="현재 점수 입력">
                <input type="number" id="target-score" placeholder="목표 점수 입력">
            </div>
            <button id="setup-button">설정 완료</button>
        `;

        const setupButton = document.getElementById('setup-button');
        const currentScoreInput = document.getElementById('current-score');
        const targetScoreInput = document.getElementById('target-score');

        setupButton.addEventListener('click', async () => {
            const score = currentScoreInput.value;
            const targetScore = targetScoreInput.value;
            if (!score || !targetScore) {
                errorMessage.textContent = '현재 점수와 목표 점수를 모두 입력해주세요.';
                return;
            }
            if (parseInt(score) >= parseInt(targetScore)) {
                errorMessage.textContent = '목표 점수는 현재 점수보다 높아야 합니다.';
                return;
            }

            loader.classList.remove('hidden');
            errorMessage.textContent = '';

            try {
                const response = await fetch('/api/setup', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        gameName: userData.gameName,
                        tagLine: userData.tagLine,
                        score: score,
                        targetScore: targetScore
                    }),
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '설정 중 오류 발생');
                }

                const checkResponse = await fetch(`/api/check?gameName=${encodeURIComponent(userData.gameName)}&tagLine=${encodeURIComponent(userData.tagLine)}`);
                const result = await checkResponse.json();
                sessionStorage.setItem('aram-data', JSON.stringify(result));
                window.location.reload();

            } catch (error) {
                errorMessage.textContent = error.message;
            } finally {
                loader.classList.add('hidden');
            }
        });
    }

    if (resetButton) {
        resetButton.addEventListener('click', async () => {
            if (!confirm('정말로 점수를 초기화하시겠습니까?')) {
                return;
            }
            loader.classList.remove('hidden');
            errorMessage.textContent = '';
            try {
                const response = await fetch('/api/reset', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
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
    }
});