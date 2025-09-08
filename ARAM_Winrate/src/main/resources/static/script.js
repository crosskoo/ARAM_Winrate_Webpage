// DOM 요소
const loader = document.getElementById('loader');
const errorMessage = document.getElementById('error-message');
const gameNameInput = document.getElementById('game-name');
const tagLineInput = document.getElementById('tag-line');
const checkButton = document.getElementById('check-button');

// '전적 확인하기' 버튼 이벤트
checkButton.addEventListener('click', async () => {
    const gameName = gameNameInput.value;
    const tagLine = tagLineInput.value;

    if (!gameName || !tagLine) {
        errorMessage.textContent = '게임 이름과 태그를 모두 입력해주세요.';
        return;
    }

    errorMessage.textContent = '';
    loader.classList.remove('hidden');

    try {
        const response = await fetch(`/api/check?gameName=${encodeURIComponent(gameName)}&tagLine=${encodeURIComponent(tagLine)}`);
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || '서버와 통신 중 오류가 발생했습니다.');
        }

        const result = await response.json();
        
        // sessionStorage에 검색 결과 저장
        sessionStorage.setItem('aram-data', JSON.stringify(result));
        sessionStorage.setItem('aram-user', JSON.stringify({ gameName, tagLine }));

        // result.html로 이동
        window.location.href = 'result.html';

    } catch (error) {
        errorMessage.textContent = error.message;
    } finally {
        loader.classList.add('hidden');
    }
});