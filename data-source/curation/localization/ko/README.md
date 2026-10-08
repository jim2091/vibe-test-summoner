# 데이터 갱신과 한국어 검수

프로젝트 또는 하위 디렉터리를 작업 경로로 두고, 기존 Java 도구와 같은 test classpath에서
`com.jim.summoner.tools.SwarfarmDataPipelineRunner`의 main을 실행한다.
기본 실행은 raw 다운로드부터 시작하며 `--skip-download`는 기존 raw를 사용한다.
기존 stable ID map을 이어서 갱신하므로 id-map을 삭제하거나 초기화하지 않는다.

순서: raw → ID map → Monster/Family → Skill → SkillEffect → LeaderSkill → MonsterSource
→ Korean localization → CandidateIntegrityValidator → CandidateDiffReporter.
단계별 START/DONE을 출력하며 실패한 단계에서 예외와 함께 즉시 중단한다.
중단 시 앞 단계 candidate 출력이 남을 수 있으므로 전체 성공 전에는 승격하지 않는다.
결과는 `data-source/candidate/normalized`, `data-source/candidate/localization/en` 및 `ko`에 저장된다.
검증 성공 후에도 사람이 diff와 데이터를 검토하여 수동 승격해야 한다.
Runner와 KO 생성기는 runtime `src/main/resources/game-data/**`를 자동 수정하지 않는다.

## 한국어 입력

이 디렉터리의 네 JSON 파일에 검증된 한국어만 입력한다. 키는 candidate entity의 **internal ID**이며
SWARFARM ID가 아니다. 게임 고유명사나 설명의 자동 번역은 하지 않는다.

- monsters: `name`, `familyName`, `awakeningBonus`
- skills: `name`, `description`, `levelUpDescriptions`
- skill-effects / monster-sources: `name`, `description`

루트는 ID를 키로 하는 object, 각 값은 non-null object다. 텍스트 필드는 생략 또는 null을 허용하며
문자열만 받는다. `levelUpDescriptions`는 있으면 문자열 배열이어야 하고 index 0은 스킬 레벨 2다.
번역하지 않은 레벨의 자리에는 빈 문자열을 넣어 이후 레벨의 순서를 보존한다.
전체 번역은 필수가 아니며 빈 파일 `{}`도 유효하다. 누락된 KO는 화면에서 EN으로 대체한다.
중복 JSON 키, 잘못된 필드/타입, candidate에 없는 ID는 실패한다.

`KoreanLocalizationCandidateGenerator`는 네 입력을 모두 검증한 뒤 candidate/ko에 출력한다.
CandidateIntegrityValidator도 존재하는 KO 파일에 같은 검증을 적용하며 전체 KO coverage는 요구하지 않는다.
기존 EN 전체 coverage 검증은 유지된다. 검증된 runtime KO는 자동으로 변경하지 않는다.
