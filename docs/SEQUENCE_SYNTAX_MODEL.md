# 시퀀스 문법·데이터 구조 설계 제안

담당 영역: 공호찬 — 시퀀스 1. 문법·데이터 구조  
상태: 모델 검토용 제안. 팀 공통 모델로 채택되거나 Java 모델에 통합된 상태가 아니다.

## 1. 이번 제출물과 실행 방법

- `examples/sequence/basic.mmd`: 참여자, 자기 호출, 요청·응답, 명시적 활성화.
- `examples/sequence/nested.mmd`: alt 안의 loop와 else 분기.
- 각 `*.expected.json`: 사람이 작성한 **예상 정규화 모델**. 실제 파서 출력이 아니다.
- `scripts/check-sequence-contract.mjs`: 위 예제의 구조·참조·중첩 규칙을 확인하는 개발용 검증기. 외부 패키지 없이 Node.js 18 이상으로 실행한다.

```bash
node scripts/check-sequence-contract.mjs
```

이 검증은 Mermaid 문법 파싱, 렌더링, Java 코드 통합, 좌표 저장·복원의 성공을 의미하지 않는다. 제품 구현 언어를 JavaScript로 정하는 변경도 아니다. 현재 목적은 파서 담당이 전달할 의미와 예제를 먼저 검토하는 것이다.

## 2. 조사 근거: Mermaid는 무엇을 저장하는가

2026-10-26에 아래 원본 파일을 읽었다. develop 링크는 바뀔 수 있으므로 확인 당시 blob SHA를 함께 기록한다.

| 원본 | 확인 당시 blob SHA | 확인한 부분 |
| --- | --- | --- |
| [sequenceDiagram.jison](https://github.com/mermaid-js/mermaid/blob/develop/packages/mermaid/src/diagrams/sequence/parser/sequenceDiagram.jison) | `f6339d04d140037bac12027d7a25758217dab6f5` | participant_statement, signal, loop/alt, else_sections |
| [sequenceDb.ts](https://github.com/mermaid-js/mermaid/blob/develop/packages/mermaid/src/diagrams/sequence/sequenceDb.ts) | `3d79b9ea6164ed9d2c2de2cb0e49c48e173fea8b` | SequenceState, LINETYPE, apply, addSignal, addNote |

- 참여자는 `actors: Map<string, Actor>`에 저장된다. 코드상의 기호와 표시 설명을 구별한다.
- 메시지 목록은 화살표만 담는 목록이 아니다. 활성화 시작·종료, loop/alt 시작·종료, else, note 같은 이벤트도 타입으로 구분해 담는다.
- 문법 규칙은 loop와 alt 내부의 document를 재귀적으로 처리하고 앞뒤에 경계 이벤트를 붙인다. `apply`는 배열을 재귀적으로 순회한다. 따라서 최종 이벤트 목록이 평평해도 시작·끝 표식을 스택으로 읽어 중첩을 복원할 수 있다.
- `A->>+B`는 메시지 뒤에 B의 활성화 시작을 추가하고, `B-->>-A`는 메시지 뒤에 B의 활성화 종료를 추가한다. +는 수신자, -는 송신자에 적용된다.
- `addSignal`은 현재 메시지 목록 길이를 문자열 ID로 사용한다. 이를 그대로 제품 영구 ID로 쓰면 앞에 이벤트를 삽입할 때 동일성이 깨질 수 있다.
- Mermaid의 내부 객체에는 렌더링 관련 값도 존재한다. 제품 저장 모델로 내부 DB 전체를 그대로 직렬화하지 않고 필요한 의미만 변환하는 편을 제안한다.

위 설명은 소스 확인 결과이고, 아래 저장 형식과 제한은 **우리 프로젝트를 위한 제안**이다.

## 3. 제안 모델

`schemaVersion: 1`, `kind: "sequence"`, `participants`, `events` 네 필드를 가진다. 좌표·색상·폰트는 이 의미 모델에 포함하지 않는다. 이 버전은 예제 계약 버전이며 제품 파일 형식 확정을 의미하지 않는다.

| 항목 | 필드 | 의미 |
| --- | --- | --- |
| 참여자 | id, symbol, label, kind | 영구 ID / Mermaid 기호 / 표시 이름 / participant 또는 actor |
| 메시지 이벤트 | id, kind=message, from, to, text, arrow | 참여자 ID 참조, 라벨, 원래 화살표 종류 |
| 활성화 이벤트 | id, kind=activate 또는 deactivate, participant | 참여자별 스택을 열거나 닫음 |
| 블록 시작 | id, kind=altStart 또는 loopStart, text | 블록 ID와 조건·반복 라벨 |
| 분기 | id, kind=else, frame, text | 현재 alt의 ID와 분기 라벨 |
| 블록 종료 | id, kind=altEnd 또는 loopEnd, frame | 닫는 블록 ID |

`participants` 배열은 가로 표시 순서다. `events` 배열은 원문의 구조적 순서다. 배열 인덱스는 ID가 아니다. 특히 alt의 양쪽 메시지가 배열에 있다고 두 분기를 차례로 실행한다는 뜻은 아니다. loop 내부 역시 실제 반복 횟수만큼 복제하지 않는다.

지원 화살표 표현: `->>`, `-->>`, `->`, `-->`, `-x`, `--x`, `-)`, `--)`. 점선이라는 이유만으로 비즈니스 의미의 '응답'이라고 확정하지 않는다. 자기 호출은 from과 to가 같은 메시지다.

### 중첩 복원 예

`nested.expected.json`에서:

1. `f-auth:altStart`를 스택에 넣는다.
2. 그 안의 `f-items:loopStart`를 넣는다.
3. `e-items:loopEnd`가 f-items를 닫는다.
4. `b-failure:else`는 현재 최상위인 f-auth의 다음 분기를 연다.
5. `e-auth:altEnd`가 f-auth를 닫는다.

종료 이벤트는 최상위 블록의 종류와 ID가 모두 맞아야 한다. else가 loop 안에서 바깥 alt를 직접 가리키는 것도 오류다. 빈 블록과 여러 else는 구조상 허용한다.

### 활성화 규칙

참여자별 LIFO 스택으로 중첩 활성화를 표현한다. 비활성 참여자를 deactivate하면 오류이고 문서 끝에 스택이 남아도 오류다.

이번 제안에서는 각 alt 분기와 loop 본문을 나갈 때 활성화 스택이 진입 시점과 같아야 한다. 분기 밖에서 시작해 전체 블록 뒤에 종료하는 활성화는 허용한다. 분기 사이로 활성화 시작·종료를 나누거나 반복마다 활성화가 누적되는 구조는 거절한다. 이는 **Mermaid 전체 문법의 보편적 제약이 아니라**, 실행 경로에 따라 상태가 달라지는 것을 피하기 위한 초기 모델 제한이다. 팀에서 범위를 합의해야 한다.

## 4. ID와 재파싱 계약

- symbol(U, S), 표시 라벨, 줄 번호, 이벤트 순서를 영구 ID로 사용하지 않는다.
- GUI에서 이름이나 텍스트만 바꾸는 작업은 기존 id를 보존한다.
- 순수 .mmd를 처음 가져올 때는 새 ID를 발급한다. 예제의 읽기 쉬운 ID는 테스트용 고정값이다.
- 코드 재파싱은 임시 모델 작성 → 기존 요소와 대응 → 전체 검증 성공 후 교체 순서로 처리한다.
- 참여자 기호 유지 등 확실한 대응은 ID를 재사용할 수 있다. 동일 문장이 반복되거나 기호·구조가 동시에 바뀌는 경우에는 대응이 모호하다. 추측해서 다른 요소의 좌표를 붙이지 않고 새 ID 및 배치 초기화 안내를 제안한다.
- 정확한 재파싱 대응 알고리즘과 ID 메타데이터 저장 위치는 후속 구현·합의 사항이다. 현재 스크립트는 재파싱 동일성 알고리즘을 구현하지 않는다.
- 실패하면 편집 중 텍스트와 진단을 보여주고 마지막 유효 모델·그림·배치를 유지한다. 실패한 부분 모델은 정상 모델로 교체하지 않는다.

## 5. 팀원 모델과 접점

확인 기준: 세윤 브랜치 `f95961ef298125760cc4bc24d2cdfe8dc6888a9c`, 병주 브랜치 `81a48b6f71d72382474e8adf58c0094393bb339a`.

| 기존 항목 | 연결 제안 / 남은 차이 |
| --- | --- |
| 세윤 `Lifeline`, `Message` | 참여자·메시지 ID를 재사용하고 별도 어댑터로 연결 |
| 세윤 `SequenceDiagramModel` | lifelines/messages/activations/notes 목록은 있으나 alt/loop 경계·분기 필드 없음 |
| 세윤 메시지 `order` | 단순 시퀀스에서는 이벤트 순서에서 계산 가능. 분기를 없앤 평탄한 실행 순서로 해석하면 안 됨 |
| 세윤 `ActivationBar.startOrder/endOrder` | 메시지 order를 요구하므로 메시지 사이의 명시적 활성화 경계는 완전 대응하지 않음. 이벤트 ID 기준 경계 확장 검토 필요 |
| 병주 `LayoutRequest.rows` | 의미 이벤트에서 표시할 행을 선택하고 렌더러가 측정한 크기·앵커와 결합 |
| 병주 배치 계약 | 중첩 프레임·영역 제약은 현재 미지원. alt/loop 예제가 기존 배치기에서 표시된다고 주장하지 않음 |

이번 기여는 별도 Java 모델을 중복 정의하거나 팀원 파일을 덮어쓰지 않는다. 우선 이 이벤트 계약을 검토한 후 기존 Java 의미 모델에 경계 타입을 추가할지, 별도 구조 트리를 도입할지 결정한다. 배치 엔진은 이벤트 순서를 바꾸거나 문법을 해석하지 않는다.

## 6. 지원 범위와 후속 수락 기준

현재 완료: 조사 문서, 두 쌍의 원문/수동 예상 모델, 모델 계약 검증.
현재 미구현: 실제 파서, 기존 Java 모델 어댑터, 배치·렌더링, 저장, 재파싱 ID 대응.

파서의 첫 구현 범위 제안은 participant/actor와 별칭, 메시지·자기 호출, 명시적 activate/deactivate, alt/else/loop다. +와 - 축약은 명시적 이벤트로 정규화한다. note, opt, par, create/destroy, 자동 번호, directive 등은 이번 예제 계약 밖이며, 파서 구현 시 조용히 삭제하지 않고 미지원 진단을 반환해야 한다. 노션 조사 대상으로 나온 alt/loop가 최종 제품 필수 범위인지도 팀에서 확정해야 한다.

후속 구현의 수락 기준:

- 원문 두 예제를 실제 파싱해 예상 모델과 비교한다. 새 ID 발급값은 대응표로 정규화한 뒤 비교한다.
- 존재하지 않는 참조, 중복 ID, 잘못된 블록 종료, 활성화 불균형을 진단한다.
- Mermaid의 암묵적 참여자는 파싱 과정에서 생성하거나, 제한할 경우 명시적 미지원 진단을 낸다. 현재 예상 모델은 모든 참조가 선언된 상태여야 한다.
- 오류 입력이 마지막 정상 모델·사용자 배치를 손상시키지 않는다.
- 이름 변경·앞쪽 메시지 삽입에서 ID를 보존하는 경우와 대응이 불가능한 경우를 분리해 검증한다.
- 실제 Java 모델/배치/렌더러 통합 후 프레임과 활성 구간의 위치를 검증한다.

## 7. 회의에서 결정할 사항

1. 이벤트 목록 + 경계 표식을 공통 모델로 채택할지, 재귀 프레임 모델을 사용할지.
2. alt/loop 지원 시기와 분기를 가로지르는 활성화의 허용 범위.
3. 기존 ActivationBar를 이벤트 경계 ID로 확장할지.
4. 재파싱 ID 대응과 프로젝트 저장 형식을 누가 구현할지.
5. 중첩 프레임의 측정·배치 계약을 어떻게 확장할지.

근거: [노션 조사 분담](https://app.notion.com/p/49c69c9c218382419e898156998d88fb), [요구사항 명세서](https://app.notion.com/p/7e369c9c218382bcad3501e1f223777a), 저장소 README/BRANCHING, 위 Mermaid 원본과 팀원 브랜치.
