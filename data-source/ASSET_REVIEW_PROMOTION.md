# Candidate 이미지 · 검토 · 승격

기존 도구와 동일한 Java test classpath에서 아래 main 클래스를 실행한다.
패키지는 모두 `com.jim.summoner.tools`이며 작업 경로는 프로젝트 또는 그 하위 디렉터리다.
각 도구 실행은 사용자가 직접 수행한다. 도구를 동시에 실행하거나 실행 중 candidate/runtime을 편집하지 않는다.

## 역할과 순서

1. `SwarfarmDataPipelineRunner` — raw → stable ID map → Monster/Family → Skill → SkillEffect → LeaderSkill → MonsterSource → KO → validation → diff.
   `--skip-download`는 기존 raw를 재사용한다. 기존 id-map을 삭제하거나 초기화하지 않는다.
2. `SwarfarmAssetCandidateDownloader` — 별도 실행한다. raw의 이미지 파일명과 `bySwarfarmId`를 사용하여 candidate에만 저장한다.
3. `CandidateDiffReporter` — 이미지 다운로드 후 다시 실행하여 최신 JSON·이미지 변경을 검토한다.
4. `CandidatePromotionTool` — 인자 없이 실행하면 검증·diff·복사 계획 보고만 수행한다.
5. 보고서를 사람이 검토한 뒤 `CandidatePromotionTool --apply`를 명시적으로 실행한다.
   이미지를 포함하려면 `--include-assets`를 추가한다. 이 옵션만 사용하면 이미지 포함 dry-run이다.

Pipeline은 이미지 다운로드와 promotion을 호출하지 않는다. 도구는 Git 명령이나 배포를 실행하지 않는다.

## 이미지 후보

출력은 `data-source/candidate/assets/`의 다음 경로다.

- `monsters/monster-{internalId}.png`
- `skills/skill-{internalId}.png`
- `effects/effect-{internalId}.png`

정상 PNG 후보 파일은 기본적으로 건너뛰며 `--force`로 다시 받는다.
순차 요청, 요청 간 지연, 최대 3회 시도를 사용한다. 네트워크 오류·429·5xx는 재시도하고 404는 반복하지 않는다.
HTTP 200과 PNG signature를 확인한 뒤 임시 파일을 교체한다. 같은 URL은 한 번만 다운로드하여 여러 대상에 쓴다.
효과의 빈 아이콘은 `noSourceIcon`(NO_SOURCE_ICON)으로 집계하며 실패가 아니다.
누락된 ID mapping·잘못된 파일명·다운로드 실패는 항목별 보고 후 계속 처리한다.
구조 오류(파싱·중복 raw ID·현재 raw의 internal ID 충돌·출력 디렉터리 오류)는 즉시 중단한다.
항목별 실패가 남으면 보고서를 작성한 뒤 예외로 종료한다. 기존 정상 파일은 실패 때문에 삭제하지 않는다.

보고서: `data-source/reports/assets/asset-download-report.json` 및 `.md`.
candidate normalized와 raw/id-map이 일치해야 하므로 JSON pipeline 이후 실행한다.
기존 후보에 남은 오래된 이미지는 자동 삭제하지 않는다.

## Diff 검토

보고서: `data-source/reports/candidate-diff.json` 및 `.md`.
normalized 6종, EN/KO localization 4종씩을 internal ID 기준으로 비교한다.
객체 필드와 배열 index를 재귀 비교하며 missing/null/0/false/빈 문자열을 구분한다.
Markdown은 dataset당 필드 변경 200개와 값 길이를 제한하고 JSON에는 전체 필드 변경을 남긴다.
이미지는 상대 경로와 SHA-256으로 비교한다. candidate asset 디렉터리가 없으면 이미지 비교를 생략한다.

필수 normalized/EN candidate 누락은 오류다. 아직 생성하지 않은 KO 파일은 생략하며 runtime KO를 보존한다.
존재하는 빈 KO `{}`는 실제 비교 대상이다. runtime에만 있는 ID는 **REMOVAL_CANDIDATE(검토 필요)**이고
runtime에만 있는 이미지는 **RUNTIME_ONLY**다. 둘 다 자동 삭제 지시가 아니다.

## 승격 안전장치

- 기본 dry-run: 필수 파일·무결성 검증, 최신 diff, 대상 파일별 CREATED/REPLACED/UNCHANGED 계획과 보고서를 만든다.
- `--apply`도 validation과 diff를 생략할 수 없다. `--include-assets`가 없으면 이미지 복사를 하지 않는다.
- ID 삭제 후보가 하나라도 있으면 apply를 거부한다. 필요한 runtime 항목/번역을 candidate와 조정하고 재검증한다.
  이 도구에는 삭제 승인 옵션이 없으며 의도적인 데이터 삭제는 별도 검토 작업이다.
- 실제로 덮어쓸 파일만 `data-source/backups/{UTC timestamp + unique suffix}/game-data/` 또는 `game-assets/`에 백업한다.
- 모든 백업을 완료한 후 파일별 임시 복사 → atomic replace를 사용한다. 지원되지 않으면 일반 replace로 대체한다.
- candidate는 복사만 하며 이동하지 않는다. runtime-only 이미지 삭제와 runtime 디렉터리 전체 교체는 하지 않는다.
- JSON의 ID별 내용이 같거나 이미지 SHA-256이 같으면 건너뛴다. 변경이 없으면 backup을 만들지 않는다.
- 입력 hash 변경을 감지하면 중단한다. 중간 실패 시 기존 파일을 백업에서 복원하고 새 runtime 파일을 제거한다.
  롤백 실패는 예외·보고서에 남으므로, 발생하면 해당 backup에서 수동 복구 후 재시도한다.
- 파일별 교체이므로 전체 파일 집합이 하나의 원자적 트랜잭션은 아니다. 서버를 중지한 상태에서 승격한다.
- 도구는 프로젝트 밖으로 이어지는 경로와 심볼릭 링크를 거부한다. asset 디렉터리에는 정해진 PNG 경로만 둔다.

승격 보고서: `data-source/reports/promotion/promotion-report-{timestamp}.json` 및 `.md`.
개수는 계획 기준이며 JSON의 `completedBeforeRollback`, `rollbackPerformed`, `rollbackErrors`에 실제 진행 내역을 남긴다.
validation 실패도 실패 보고서로 기록한다. report 저장 자체가 실패하면 원래 오류와 함께 출력한다.

runtime 위치는 기존 `src/main/resources/game-data/`와 `src/main/resources/static/game-assets/`를 사용한다.
reports/backups/candidate assets는 runtime source가 아니다.

Game images are third-party game assets. Distribution / deployment rights should be reviewed separately.
