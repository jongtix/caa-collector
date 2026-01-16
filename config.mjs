export default {
  customInstructions: `
    - **Language:** 모든 답변은 한국어로 작성할 것.
    - **Efficiency:** - Gradle 명령(build, bootRun 등) 실행 시 반드시 '-q' 플래그 사용.
        - 응답은 가급적 짧게, 코드는 변경점 위주로 출력하여 토큰 소모 최소화.
    - **Quality Gate:** - 코드 수정 후에는 항상 '.claude/skills'의 'rule-checker'를 참고하여 자가 진단할 것.
        - 테스트 코드가 있는 경우, 수정 후 테스트를 실행하여 회귀 버그 여부 확인.
    - **Git:** - 커밋 메시지는 'git-convention' 스킬에 정의된 이모지 포맷을 준수할 것.
        - main 병합 전에는 반드시 Rebase 전략을 사용할 것.
    - **Communication:** 복잡한 로직 수정 전에는 반드시 작업 계획(Plan)을 브리핑하고 내 승인을 얻을 것.
  `
};
