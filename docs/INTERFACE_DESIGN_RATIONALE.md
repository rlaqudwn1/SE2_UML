# 시퀀스 배치 인터페이스: 코드와 선택 이유

김병주의 작업은 **Java 25 시퀀스 배치 알고리즘 인터페이스**입니다. 문법과 의미 데이터 구조는 다른 팀원이 담당합니다. [HTML 설명](sequence-layout-interface.html), [상세 계약](SEQUENCE_LAYOUT_CONTRACT.md), [팀 간 통합 흐름](INTERFACE_INTEGRATION.md)과 함께 읽습니다.

## 근거를 읽는 방법

- **사용자 요구사항:** 시퀀스 배치 담당 범위, 다른 팀 모델을 침범하지 않는 것, 이동 좌표 재사용, Java 25, dev 통합에서 의도를 시각화하는 것.
- **외부 확인 사실:** Mermaid 지정 커밋의 렌더링 순서·측정 처리와 Java 공식 API 동작. 아래 레퍼런스 절에서 출처를 연결합니다.
- **우리의 설계 제안:** Java interface/DTO 이름, ID 연결, 절대 좌표 제약, pinned·경고·오류·순서 보존 정책. 외부 라이브러리의 공식 설계를 그대로 구현한 것이 아닙니다.

아래 코드는 현재 `SequenceLayoutEngine.java`의 실제 발췌입니다. 전체 클래스가 아니라 부분 코드이며 파일 변경 시 발췌도 함께 갱신해야 합니다. 진입점의 발췌는 interface 본문 시작 부분입니다.

## 1. 알고리즘 진입점은 interface로 분리

[실제 소스 L32–L34](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L32)

```java
public interface SequenceLayoutEngine {
    /** Computes geometry without mutating input, persisting state, or rendering. */
    LayoutResult layout(LayoutRequest request);
```

**선택 이유:** 호출자가 배치 구현체의 내부 알고리즘을 몰라도 같은 요청·결과로 연결할 수 있게 했습니다. 팀 경계를 위한 우리의 설계 선택입니다. Mermaid가 이 Java 인터페이스를 제공하는 것은 아닙니다.

**대안과 비용:** 다른 팀의 문법·의미 모델에 직접 의존하거나 렌더링까지 한 서비스에 넣는 방식도 가능하지만, 담당 범위가 섞이고 구현 교체 시 호출자도 바뀝니다.

**진행 상태:** 메서드 계약만 존재합니다. 실제 구현체는 없습니다.

## 2. 의미 모델 대신 측정된 박스와 행을 받음

[실제 소스 L36–L53](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L36)

```java
    record ParticipantBox(String id, double width, double height) {
        public ParticipantBox {
            requireId(id);
            positive(width, "width");
            positive(height, "height");
        }
    }

    /** Generic measured row; duplicate references support caller-defined self-call shapes. */
    record LayoutRow(String id, List<String> participantIds, double width, double height) {
        public LayoutRow {
            requireId(id);
            participantIds = List.copyOf(participantIds);
            participantIds.forEach(SequenceLayoutEngine::requireId);
            positive(width, "width");
            positive(height, "height");
        }
    }
```

**선택 이유:** 배치에 필요한 ID·크기·참여자 참조만 받습니다. 순서는 List로 전달합니다. LayoutRow는 메시지 종류를 정의하지 않는 배치용 데이터입니다. 측정과 배치를 나눈 것은 다른 팀의 의미 모델을 침범하지 않기 위한 선택입니다.

**대안과 비용:** 의미 모델 자체를 받으면 어댑터는 줄어들지만 해당 모델 변경에 배치 코드가 결합됩니다. 현재 선택은 어댑터를 만들고 ID·크기를 전달하는 비용이 있습니다. 중첩 블록·활성 구간 전체를 표현하는 완성된 모델도 아닙니다.

**진행 상태:** 크기·ID 검증과 참조 List 복사는 구현됐습니다. 실제 텍스트 측정과 어댑터는 없습니다.

## 3. 사용자 위치를 별도 제약으로 전달

[실제 소스 L55–L70](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L55)

```java
    /** A preferred coordinate, or an exact coordinate when pinned is true. */
    record PositionHint(double coordinate, boolean pinned) {
        public PositionHint { finite(coordinate, "coordinate"); }
    }

    /** Persist these hints independently from the teammate-owned diagram model. */
    record ManualLayout(Map<String, PositionHint> participantX, Map<String, PositionHint> rowY) {
        public ManualLayout {
            participantX = Map.copyOf(participantX);
            rowY = Map.copyOf(rowY);
            participantX.keySet().forEach(SequenceLayoutEngine::requireId);
            rowY.keySet().forEach(SequenceLayoutEngine::requireId);
        }

        public static ManualLayout empty() { return new ManualLayout(Map.of(), Map.of()); }
    }
```

**선택 이유:** 사용자가 이동한 위치를 재사용하려고 참여자의 x와 행의 y를 별도 Map으로 받습니다. 이름 대신 ID로 연결합니다. pinned=true는 정확한 위치, false는 조정 가능한 선호 위치입니다. 이 타입과 고정·선호 구분은 우리의 정책 제안입니다.

**대안과 비용:** 자동 좌표에 오프셋을 더하는 방식은 자동 배치가 바뀌면 최종 위치도 바뀔 수 있습니다. 절대 좌표 제약은 원하는 위치를 설명하기 쉽지만 순서·간격과 충돌할 수 있습니다. 파일 저장 형식과 저장 장소는 확정하지 않았습니다.

**진행 상태:** 힌트 데이터와 불변 Map은 있습니다. 좌표 반영 알고리즘, 저장·복원 구현은 없습니다.

## 4. 배치 옵션은 요청의 명시적 입력

[실제 소스 L72–L80](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L72)

```java
    /** Minimum clear space between boxes; margin is required around canvas content. */
    record LayoutOptions(double participantGap, double rowGap, double headerGap, double margin) {
        public LayoutOptions {
            nonNegative(participantGap, "participantGap");
            nonNegative(rowGap, "rowGap");
            nonNegative(headerGap, "headerGap");
            nonNegative(margin, "margin");
        }
    }
```

**선택 이유:** 환경에 따라 간격을 바꿀 수 있도록 네 값을 입력으로 받습니다. 자동 배치와 수동 위치 충돌을 같은 최소 간격 기준으로 판단할 수 있습니다. 이름과 정책은 우리 계약이며 Mermaid 설정을 그대로 복제한 것이 아닙니다.

**대안과 비용:** 상수로 고정하면 처음 구현은 단순해지지만 호출자가 원하는 화면 밀도를 조절하기 어렵습니다. 너무 많은 옵션을 만들지 않고 현재 네 가지로 제한했습니다. 기본값은 아직 합의하지 않았습니다.

**진행 상태:** 0 이상·유한한 값 검증은 있습니다. 간격을 적용하는 구현은 없습니다.

## 5. 요청 생성 시 구조적으로 잘못된 입력을 거부

[실제 소스 L82–L106](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L82)

```java
    /** Caller-owned adapter input, not a sequence diagram semantic model. */
    record LayoutRequest(List<ParticipantBox> participants, List<LayoutRow> rows,
                         ManualLayout manualLayout, LayoutOptions options) {
        public LayoutRequest {
            participants = List.copyOf(participants);
            rows = List.copyOf(rows);
            Objects.requireNonNull(manualLayout, "manualLayout");
            Objects.requireNonNull(options, "options");
            var participantIds = new HashSet<String>();
            for (var participant : participants) {
                if (!participantIds.add(participant.id())) {
                    throw new IllegalArgumentException("Duplicate participant ID: " + participant.id());
                }
            }
            var rowIds = new HashSet<String>();
            for (var row : rows) {
                if (!rowIds.add(row.id())) {
                    throw new IllegalArgumentException("Duplicate row ID: " + row.id());
                }
                requireKnown(row.participantIds(), participantIds, "row participant");
            }
            requireKnown(manualLayout.participantX().keySet(), participantIds, "participant hint");
            requireKnown(manualLayout.rowY().keySet(), rowIds, "row hint");
        }
    }
```

**선택 이유:** 잘못된 ID·누락된 참조는 배치 전에 발견할 수 있으므로 생성자에서 거부합니다. 리스트 순서와 스냅샷을 유지하려고 List.copyOf를 사용했습니다. 복사 API 동작은 Java 공식 문서로 확인했고 검증 정책은 우리의 선택입니다.

**대안과 비용:** 알고리즘 안에서 모든 입력 오류를 처리할 수도 있지만 같은 요청을 여러 구현체가 다르게 해석할 수 있습니다. 반면 생성자 예외는 호출자가 별도로 처리해야 합니다. 유효한 요청의 좌표 충돌은 생성자 오류와 구분합니다.

**진행 상태:** 중복 ID, 없는 참여자 참조·힌트, null과 컬렉션 복사 검증은 구현됐습니다. 순서·간격 충돌 판단은 엔진 책임입니다.

## 6. 좌표와 크기는 명시적인 값 타입

[실제 소스 L108–L125](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L108)

```java
    record Point(double x, double y) {
        public Point {
            finite(x, "x");
            finite(y, "y");
        }
    }

    /** Zero size is allowed for an empty canvas, but all extents must remain finite. */
    record Bounds(double x, double y, double width, double height) {
        public Bounds {
            finite(x, "x");
            finite(y, "y");
            nonNegative(width, "width");
            nonNegative(height, "height");
            finite(x + width, "right edge");
            finite(y + height, "bottom edge");
        }
    }
```

**선택 이유:** Point는 연결점, Bounds는 왼쪽 위 좌표와 영역 크기입니다. NaN·무한대와 넘치는 경계를 거부해 렌더러에 유효한 수를 전달합니다. 원점과 기준점은 팀 사이에서 혼동하지 않도록 우리가 명시한 정책입니다.

**대안과 비용:** 엔진별 도형 객체를 반환하면 호출자가 해당 라이브러리에 묶입니다. 단순 값 타입은 연결하기 쉽지만 좌표 단위와 기준점에 대한 팀 합의가 필요합니다.

**진행 상태:** 수치 유효성 검증은 있습니다. 음수 좌표는 허용하고 빈 캔버스의 0 크기도 허용합니다.

## 7. 진단과 결과를 함께 반환

[실제 소스 L127–L137](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L127)

```java
    enum Severity { WARNING, ERROR }

    /** elementId is optional: null denotes a diagram-wide diagnostic. */
    record Diagnostic(Severity severity, String code, String message, String elementId) {
        public Diagnostic {
            Objects.requireNonNull(severity, "severity");
            requireId(code);
            requireId(message);
            if (elementId != null) requireId(elementId);
        }
    }
```

**선택 이유:** 선호 위치를 조정했으면 WARNING, 고정 위치가 불가능하면 ERROR로 알려야 호출자가 조정과 실패를 구분할 수 있습니다. elementId가 있으면 관련 요소를 표시할 수 있습니다. 이 진단 방식은 우리의 정책 제안입니다.

**대안과 비용:** 모든 문제를 예외로 던지면 간단하지만 여러 요소의 충돌과 경고를 함께 설명하기 어렵습니다. 현재 계약에서는 입력 구조 오류는 예외, 배치 제약 실패는 결과 진단으로 나눕니다. 표준 code 목록은 미정입니다.

**진행 상태:** 진단 타입과 필드 검증은 있습니다. 실제 제약 충돌 탐지는 없습니다.

## 8. 결과를 ID별 영역·연결점·경계로 나눔

[실제 소스 L151–L171](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L151)

```java
    record LayoutResult(Map<String, Bounds> participantBounds, Map<String, Bounds> rowBounds,
                        Map<String, List<Point>> rowAnchors, Bounds canvasBounds,
                        List<Diagnostic> diagnostics) {
        public LayoutResult {
            participantBounds = Map.copyOf(participantBounds);
            rowBounds = Map.copyOf(rowBounds);
            participantBounds.keySet().forEach(SequenceLayoutEngine::requireId);
            rowBounds.keySet().forEach(SequenceLayoutEngine::requireId);
            var copiedAnchors = new java.util.HashMap<String, List<Point>>();
            rowAnchors.forEach((id, points) -> {
                requireId(id);
                copiedAnchors.put(id, List.copyOf(points));
            });
            rowAnchors = Map.copyOf(copiedAnchors);
            Objects.requireNonNull(canvasBounds, "canvasBounds");
            diagnostics = List.copyOf(diagnostics);
        }

        public boolean hasErrors() {
            return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == Severity.ERROR);
        }
```

**선택 이유:** 렌더러는 의미 모델의 ID로 geometry를 조회하고 연결점을 이용할 수 있습니다. canvasBounds는 전체 표시 영역입니다. 결과 Map은 순서를 담는 용도가 아니므로 입력 List 순서로 그립니다. 결과 분리는 우리 설계 선택입니다.

**대안과 비용:** SVG 문자열만 반환하면 화면 출력은 쉽지만 GUI에서 요소를 이동하거나 ID로 다시 연결하기 어렵습니다. 구조화된 결과는 호출자가 실제 그리기를 구현해야 합니다. 현재 결과 생성자는 성공 geometry의 모든 조건을 자동 검증하지 않습니다.

**진행 상태:** Map·중첩 List 복사와 hasErrors는 구현됐습니다. 좌표 생성·순서 유지·영역 포함·최소 간격 보장은 구현체가 검증해야 합니다.

## record와 컬렉션 복사를 함께 사용한 이유

record는 데이터 운반용 필드, 접근자, 동등성 표현을 간결하게 만들고 compact constructor에서 검증할 수 있습니다. 이 용도는 [Java 25 Record Classes](https://docs.oracle.com/en/java/javase/25/language/records.html)를 참고했습니다. record 자체가 참조 대상 컬렉션까지 불변으로 만드는 것은 아닙니다.

입력·결과 컬렉션에는 [List.copyOf](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection))와 [Map.copyOf](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Map.html#copyOf(java.util.Map))를 사용합니다. 결과의 중첩 연결점 List도 각각 복사합니다. 이 API는 수정 불가능한 컬렉션을 제공하지만 원소를 임의로 깊게 복제하는 API가 아닙니다. 현재 원소는 String·enum·기본값 기반 record이므로 원소와 컬렉션을 함께 안정적으로 취급할 수 있습니다.

## 레퍼런스와 설계 선택의 연결

출발점은 [김병주의 조사 노트](https://app.notion.com/p/2-3ebc84b2acec813f9cb5e47eef7760eb)입니다. 노트가 인용한 Mermaid 지정 커밋 `4f6ac96ac95fd0ee0f66c986985680eb98dce1ec`의 원본 코드를 다시 확인했습니다. 아래 링크는 해당 커밋으로 고정돼 있습니다. 코드 확인과 브라우저 실측은 구분하며, 실제 겹침·배치 안정성을 실측했다고 주장하지 않습니다.

| 직접 확인한 레퍼런스 | 확인한 사실 | 우리의 계약에 반영한 판단 |
| --- | --- | --- |
| [Mermaid 참여자 배치 L757–810](https://github.com/mermaid-js/mermaid/blob/4f6ac96ac95fd0ee0f66c986985680eb98dce1ec/packages/mermaid/src/diagrams/sequence/sequenceRenderer.ts#L757-L810) | 참여자 순서를 순회하며 크기·간격으로 x를 계산 | 순서 있는 참여자 List와 측정 크기를 입력으로 받는 방향의 근거 |
| [Mermaid 처리 흐름 L1100–1146](https://github.com/mermaid-js/mermaid/blob/4f6ac96ac95fd0ee0f66c986985680eb98dce1ec/packages/mermaid/src/diagrams/sequence/sequenceRenderer.ts#L1100-L1146) | 참여자 배치 정보를 먼저 만들고 메시지를 순회 | 참여자 배치와 세로 행 배치를 구분하는 방향의 근거 |
| [Mermaid 크기·간격 측정 L1614–1667](https://github.com/mermaid-js/mermaid/blob/4f6ac96ac95fd0ee0f66c986985680eb98dce1ec/packages/mermaid/src/diagrams/sequence/sequenceRenderer.ts#L1614-L1667) | 텍스트·메시지 폭을 참여자 크기·간격에 반영 | 측정값을 받는 입력이 필요하다는 근거. 측정 책임을 어댑터·렌더러에 둔 것은 우리 선택 |
| [Java 25 Record API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Record.html) | record는 데이터 전달 객체이며 얕은 불변성 제공 | 입출력 값 타입은 record로 표현하고 컬렉션은 별도로 복사 |
| [Java 25 List.copyOf](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)) | 수정 불가능한 List, 입력 컬렉션의 반복 순서 유지 | 입력 순서를 보존하는 컬렉션 스냅샷에 사용 |
| [Java 25 Map.copyOf](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Map.html#copyOf(java.util.Map)) | 수정 불가능한 Map 제공 | ID별 힌트·geometry를 보관. 반복 순서를 의미상 순서로 사용하지 않음 |
| [Oracle Java 지원 로드맵](https://www.oracle.com/java/technologies/java-se-support-roadmap.html) | Java 25는 LTS 버전 | 사용자가 선택한 프로젝트 기준을 Java 25 LTS로 명시 |

Mermaid 원본은 렌더링 과정에서 내부 데이터와 SVG를 다루므로, 현재 Java 계약처럼 측정·의미 모델·배치·렌더링이 동일한 경계로 분리돼 있다고 설명하면 안 됩니다. 이름과 구조를 참고해 복사한 API도 아닙니다.

다음 선택들은 **외부 문서로 정답이 증명된 정책이 아닙니다.** 사용자 요구에 따른 설계 제안이며 팀과 합의해야 합니다.

| 우리 선택 | 요구 또는 목적 | 남은 확인 |
| --- | --- | --- |
| 문법·의미 모델과 배치 DTO 분리 | 다른 팀원의 담당 범위 보존 | 어댑터 담당과 실제 모델 연결 방식 |
| ID로 사용자 위치 연결 | 이동 좌표를 다시 적용 | 모델 담당자의 안정적인 ID 유지 정책 |
| 절대 좌표와 고정·선호 구분 | 정확한 위치 보존과 충돌 처리 구분 | 드래그 기본값을 고정으로 둘지 여부 |
| 순서 유지와 충돌 진단 | 배치 이동이 실행 순서를 몰래 변경하지 않도록 함 | 순서 변경 편집과 메시지 간격 편집의 구분 |
| 입력 오류는 예외, 배치 실패는 진단 | 구조 오류와 유효한 제약의 충돌을 구분 | 표준 진단 code 목록과 화면 표시 |
| 영역·연결점 데이터 반환 | GUI에서 요소를 ID로 연결하고 이동 | 렌더러의 선·화살표·특수 요소 해석 |

## 다른 UML 도구도 같은 구조인가?

**역할의 구분은 비슷하지만, API와 좌표 정책까지 같지는 않습니다.** 텍스트에서 자동 배치하는 도구와 geometry를 편집하는 도구를 구분해서 참고했습니다.

| 도구 | 직접 확인한 구조·동작 | 현재 인터페이스와의 관계 |
| --- | --- | --- |
| Mermaid 시퀀스 | 참여자 순서·측정 크기로 x를 계산한 뒤 메시지를 처리하며 SVG 렌더링 수행 | 순서·크기·간격을 쓰는 흐름을 참고. 현재 Java DTO와 pinned 정책은 제공하지 않음 |
| PlantUML 시퀀스 | `SequenceDiagram`이 참여자·이벤트 목록을 가지며 Teoz 배치·렌더링 클래스에 전달 | 의미 모델과 배치 단계의 역할 구분을 참고할 수 있음. 배치와 그리기는 한 클래스에도 함께 존재함 |
| diagrams.net / mxGraph | diagram XML에 도형·연결선 등을 표현하고 geometry는 위치·크기·연결점 정보를 가짐 | ID와 geometry로 화면 요소를 연결하는 비교 사례. 시퀀스 실행 순서 보존 정책의 근거로 사용하지 않음 |
| SE2_UML의 이번 제안 | 측정된 배치 DTO → 순서·위치 제약 계산 → geometry·진단 반환 | 팀 경계를 위해 접점을 명시하고 사용자 좌표 요구를 추가한 계약 |

PlantUML 원본에서 [참여자·이벤트 목록](https://github.com/plantuml/plantuml/blob/8678962c537bfb854073fc15394ac0b5ed5ea83e/src/main/java/net/sourceforge/plantuml/sequencediagram/SequenceDiagram.java#L96-L98)과 [Teoz 위치·제약 구성](https://github.com/plantuml/plantuml/blob/8678962c537bfb854073fc15394ac0b5ed5ea83e/src/main/java/net/sourceforge/plantuml/sequencediagram/teoz/SequenceDiagramFileMakerTeoz.java#L88-L120)을 확인했습니다. [공식 시퀀스 문서](https://plantuml.com/sequence-diagram)는 선언 순서를 기본 표시 순서로 사용하고 `order`로 변경할 수 있으며, 텍스트 명령으로 간격을 조정할 수 있다고 설명합니다. 이것이 GUI의 절대 좌표 고정 계약과 같다는 뜻은 아닙니다.

[draw.io 공식 문서](https://www.drawio.com/docs/manual/advanced/diagram-source-edit/)는 도형·연결선·스타일·메타데이터를 XML로 표현하며 `mxGraphModel`로 감싼다고 설명합니다. [mxGeometry 공식 API](https://jgraph.github.io/mxgraph/docs/js-api/files/model/mxGeometry-js.html)는 위치·크기와 연결선 제어점을 표현합니다. 이는 geometry 표현의 비교 사례이며 현재 diagrams.net 전체 배치 구현을 분석한 것은 아닙니다.

발표에서는 “모델에서 배치·렌더링으로 이어지는 기존 도구의 흐름을 참고하되, 우리는 팀별 책임과 사용자 좌표 보존을 위해 geometry 입출력 계약을 명시했다”고 설명할 수 있습니다. `PositionHint`와 `ERROR/WARNING`을 UML 도구 공통 표준이라고 설명하면 안 됩니다.

## 구현됐다고 말할 수 있는 범위

Java 25 컴파일, 입력 ID·참조·숫자 유효성, 컬렉션 스냅샷·수정 불가, 경고·오류 구분은 실행 검증됐습니다. 실제 엔진이 없으므로 배치 좌표 생성, 고정 좌표 유지, 순서·간격 만족, 렌더링·저장은 아직 동작 검증되지 않았습니다. HTML의 미리보기는 계약을 설명하는 기대 결과입니다.

## 팀 회의에서 사용할 설명

“다른 팀이 확정한 순서·ID·측정 크기를 받아서 배치하는 경계를 만들었습니다. 사용자 좌표는 별도 제약으로 넘깁니다. 고정 위치를 만족할 수 없으면 순서를 바꾸지 않고 오류로 설명하도록 제안했습니다. 이 정책은 Mermaid에서 가져온 API가 아니라 요구사항을 바탕으로 한 팀 합의 초안입니다. 모델 담당자와 ID·순서·크기 변환을 먼저 맞춘 뒤 실제 엔진을 구현합니다.”
