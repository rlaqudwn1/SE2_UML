# 시퀀스 배치 인터페이스: 코드와 선택 이유

김병주의 현재 작업 범위는 **Java 25 시퀀스 배치 인터페이스와 독립 계약 검증기**입니다. 실제 배치 알고리즘은 구현하지 않습니다. 문법과 의미 데이터 구조는 다른 팀원이 담당합니다. [HTML 설명](sequence-layout-interface.html), [상세 계약](SEQUENCE_LAYOUT_CONTRACT.md), [팀 간 통합 흐름](INTERFACE_INTEGRATION.md)과 함께 읽습니다.

[부족한 부분 검토](INTERFACE_GAP_REVIEW.md)의 전체 폭·연결점·진단·검증 문제를 계약에 반영했습니다. 검토 당시의 반례와 현재 보완 상태를 구분합니다. HTML 미리보기는 계약 설명용이며 실제 Java 엔진의 실행 결과가 아닙니다.

## 근거를 읽는 방법

- **사용자 요구사항:** 시퀀스 배치 담당 범위, 다른 팀 모델을 침범하지 않는 것, 이동 좌표 재사용, Java 25, 인터페이스와 보장 범위를 설명하는 것.
- **외부 확인 사실:** 기존 조사에서 확인한 Mermaid 지정 커밋의 처리 흐름과 Java 공식 API 동작. 아래 레퍼런스 절의 primary source를 유지합니다.
- **우리의 설계 선택:** DTO 이름, 측정 연결점·최소 거리, 절대 좌표 pin, 순서 유지와 진단 정책. 외부 도구가 공통으로 제공하는 API라는 주장은 아닙니다.

아래 Java 블록은 현재 소스에서 그대로 발췌한 부분 코드입니다. 좌표 예시는 계약을 설명하는 계산이며 배치 엔진의 실행 결과가 아닙니다.

## 1. 호출 경계를 interface로 정의

[실제 소스 SequenceLayoutEngine.java L34–L36](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L34)

```java
public interface SequenceLayoutEngine {
    /** Computes geometry without mutating input, persisting state, or rendering. */
    LayoutResult layout(LayoutRequest request);
```

**선택 이유:** 호출자가 알고리즘 내부를 몰라도 같은 요청·결과로 연결하도록 경계를 정의했습니다. 파서나 팀원의 의미 모델을 직접 인자로 받지 않습니다. 측정·어댑터·배치·렌더링의 책임을 분리하기 위한 우리 선택입니다.

**현재 상태:** `layout`은 계약만 존재하며 구현체는 없습니다. 검증기는 이미 계산된 요청·결과의 관계를 검사할 뿐 좌표를 생성하지 않습니다.

## 2. 전체 행 폭과 연결점 사이 거리를 구분

[실제 소스 SequenceLayoutEngine.java L78–L79](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L78)

```java
    record LayoutRow(String id, List<AnchorSpec> anchors, double width, double height,
                     List<MinimumSpan> minimumSpans) {
```
[실제 소스 SequenceLayoutEngine.java L62–L62](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L62)

```java
    record MinimumSpan(int firstAnchorIndex, int secondAnchorIndex, double minDistance) {
```

**선택 이유:** 폭 240인 행 안에 두 연결점이 100만큼 떨어져 있어도 전체 폭만 검사하면 통과합니다. 라벨에 필요한 중심 사이 거리는 별도 `MinimumSpan`으로 전달해야 합니다. `width`는 행 전체 영역의 최소 폭, `height`는 최소 높이입니다. span은 참조 순서와 무관한 절대 가로 거리입니다.

인덱스는 같은 행의 연결점 List를 가리키며 서로 다르고 범위 안에 있어야 합니다. 거리는 유한한 0 이상입니다. 서로 다른 인덱스가 같은 참여자를 참조하는 것은 허용합니다. 이때 양의 span을 요구하면 요청 구조는 유효해도 성공 배치는 불가능하므로 향후 엔진이 ERROR로 보고해야 합니다.

**대안과 비용:** 엔진이 메시지 종류로부터 폭을 추정하는 대신 어댑터가 측정값을 전달합니다. 라벨을 두 연결점 사이에 둘지 바깥에 둘지는 측정 담당자의 결정입니다.

## 3. 연결점 높이와 비대칭 점유 영역을 보존

[실제 소스 SequenceLayoutEngine.java L51–L52](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L51)

```java
    record AnchorSpec(String participantId, double offsetY, double leftExtent,
                      double rightExtent) {
```

**선택 이유:** 중복 참여자 ID만으로는 자기 호출의 두 높이나 루프 돌출량을 알 수 없습니다. 출력 x는 참여자 박스의 중심, y는 `row.y + offsetY`입니다. `leftExtent`와 `rightExtent`는 그 중심 기준으로 필요한 좌우 가로 점유량입니다.

같은 참여자의 오프셋을 10·45로 주면 두 연결점은 같은 x와 서로 다른 y를 요구합니다. 왼쪽 130·오른쪽 0과 그 반대도 구분합니다. 이 숫자는 측정된 geometry를 표현하며 `SelfMessage`나 노트 방향 같은 의미 타입을 새로 정의한 것이 아닙니다.

오프셋·점유량은 유한한 0 이상이며 `offsetY <= height`여야 합니다. 성공 행 영역은 모든 연결점의 좌우 점유량과 최소 전체 폭을 포함해야 합니다. 출력 연결점은 입력 순서와 개수를 보존합니다. 글꼴·선·패딩 측정과 실제 도형 렌더링은 어댑터·렌더러 책임입니다.

## 4. 사용자 위치·순서·간격을 별도 제약으로 전달

[실제 소스 SequenceLayoutEngine.java L101–L102](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L101)

```java
    record PositionHint(double coordinate, boolean pinned) {
        public PositionHint { finite(coordinate, "coordinate"); }
```
[실제 소스 SequenceLayoutEngine.java L106–L106](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L106)

```java
    record ManualLayout(Map<String, PositionHint> participantX, Map<String, PositionHint> rowY) {
```

**선택 이유:** 참여자 x는 박스 왼쪽, 행 y는 행 위쪽의 절대 좌표입니다. `pinned=true`는 정확히 보존하고 false는 조정 가능한 선호입니다. ID로 힌트를 연결하되 순서는 요청 List에 둡니다. 저장 장소·파일 형식과 드래그 기본값은 별도 합의 사항입니다.

`LayoutOptions`는 참여자·행·헤더 간격과 캔버스 여백을 유한한 0 이상 값으로 받습니다. 참여자는 앞 박스의 오른쪽 경계 이후, 다음 행은 앞 행의 아래 경계 이후에 해당 간격을 확보해야 합니다. 헤더 간격은 가장 낮은 참여자 박스 아래와 첫 행 위 사이에 적용합니다. 헤더 공통 y와 자동 원점은 아직 인터페이스에서 강제하지 않습니다.

폭 80·간격 20인 A, B, C에서 A x=0과 C x=150을 고정하면 중간 B까지 필요한 거리는 200이므로 성공 결과를 만들 수 없습니다. 반면 음수 pin 자체는 오류가 아닙니다. 향후 엔진은 앞 요소와 후속 pin 사이에 필요한 간격을 함께 고려해야 하며, 순서를 바꾸거나 pin을 몰래 옮겨 해결하면 안 됩니다. 현재는 이 조건을 검증하며 해결 알고리즘을 제공하지 않습니다.

## 5. 진단 대상의 이름 공간을 분리

[실제 소스 SequenceLayoutEngine.java L175–L175](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L175)

```java
    enum ElementKind { PARTICIPANT, ROW }
```
[실제 소스 SequenceLayoutEngine.java L178–L178](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L178)

```java
    record ElementRef(ElementKind kind, String id) {
```
[실제 소스 SequenceLayoutEngine.java L186–L186](../src/main/java/org/se2uml/layout/SequenceLayoutEngine.java#L186)

```java
    record Diagnostic(Severity severity, String code, String message, ElementRef target) {
```

**선택 이유:** 참여자와 행이 같은 문자열 ID를 가질 수 있으므로 문자열만으로는 어느 요소를 강조할지 알 수 없습니다. `ElementRef(kind, id)`로 구분하며 null target은 전체 다이어그램을 뜻합니다.

선호가 허용오차보다 이동했다면 같은 타입·ID를 대상으로 `POSITION_ADJUSTED` WARNING이 필요합니다. 입력 구조 오류는 생성자 예외로, 유효한 요청의 배치 실패는 ERROR 진단으로 구분합니다. 진단 데이터와 검증 조건은 구현됐지만 실제 충돌을 해결하거나 진단을 생성하는 배치 엔진과 UI는 아직 없습니다.

## 6. 생성자 검증과 성공 geometry 검증을 분리

[실제 소스 SequenceLayoutValidator.java L41–L43](../src/main/java/org/se2uml/layout/SequenceLayoutValidator.java#L41)

```java
    public static List<Diagnostic> validate(LayoutRequest request, LayoutResult result) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");
```

**선택 이유:** `LayoutResult` 생성자는 요청을 모르므로 입력 ID 누락이나 잘못된 pin을 발견할 수 없습니다. `hasErrors()`도 진단에 ERROR가 있는지만 확인합니다. 독립적인 `SequenceLayoutValidator.validate(request, result)`가 요청과 결과를 함께 검사하도록 했습니다.

검사 범위는 정확한 ID 집합, 참여자 크기, 행 최소 크기, 연결점 개수·중심 x·로컬 y·좌우 점유량, 최소 span, 순서·간격·캔버스 여백, exact pin과 이동한 선호의 타입 일치 경고입니다. 누락된 결과를 만나도 나머지 검사를 계속하며 진단을 모읍니다. geometry에는 절대 허용오차 `1e-6`을 사용하지만 pin은 정확한 수치 일치를 요구합니다. Java 수치 비교에서 +0과 -0은 같은 값으로 봅니다.

ERROR가 이미 있는 결과에는 부분 geometry를 허용하므로 성공 검사를 건너뛰고 기존 ERROR를 반환합니다. 이 반환은 성공 판정이 아닙니다. 검증기는 요청의 불가능성을 증명하지 않으며 향후 엔진이 가능한 요청을 잘못 거절하는 문제는 별도 엔진 테스트가 필요합니다.

결과 Map은 ID 조회용이며 렌더링 순서는 요청 List에서 읽습니다. `Point`·`Bounds`는 NaN·무한대·넘치는 경계를 거부하고 음수 좌표와 빈 캔버스의 0 크기는 허용합니다. 현재 범위는 값 타입과 그 계약 검사입니다.

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

다음 선택들은 **외부 문서로 정답이 증명된 정책이 아닙니다.** 사용자 요구에 따라 계약에 반영한 설계 선택이며 팀 통합에서는 합의가 필요합니다.

| 우리 선택 | 요구 또는 목적 | 남은 확인 |
| --- | --- | --- |
| 문법·의미 모델과 배치 DTO 분리 | 다른 팀원의 담당 범위 보존 | 어댑터 담당과 실제 모델 연결 방식 |
| ID로 사용자 위치 연결 | 이동 좌표를 다시 적용 | 모델 담당자의 안정적인 ID 유지 정책 |
| 절대 좌표와 고정·선호 구분 | 정확한 위치 보존과 충돌 처리 구분 | 드래그 기본값을 고정으로 둘지 여부 |
| 순서 유지와 충돌 진단 | 배치 이동이 실행 순서를 몰래 변경하지 않도록 함 | 순서 변경 편집과 메시지 간격 편집의 구분 |
| 입력 오류는 예외, 배치 실패는 진단 | 구조 오류와 유효한 제약의 충돌을 구분 | 진단 code별 화면 표시와 복구 동작 |
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

## 구현·검증된 범위와 한계

[계약 검사](../src/test/java/org/se2uml/layout/SequenceLayoutContractCheck.java)는 입력 숫자·참조·컬렉션 검증, 측정 연결점·span·타입별 진단, 성공 geometry의 개별 위반을 확인합니다. 정상 수치 fixture와 결과를 변형한 반례로 ID·크기·간격·연결점·pin·경고·캔버스 조건을 검사합니다. 부분 ERROR 결과도 성공으로 인증되지 않는지 확인합니다.

Java 25의 `--release 25 -Xlint:all -Werror` 컴파일과 계약 검사를 통과했습니다. 이는 DTO와 독립 검증기의 근거이며 실제 배치 알고리즘의 정확성 검증은 아닙니다. HTML은 설명용 미리보기입니다.

실제 엔진, 문법 파서, 의미 모델 변환 어댑터, 텍스트 측정, 자동 줄바꿈 재측정, 활성 구간·중첩 프레임·동높이 행, 렌더링, 저장·복원, 드래그 UI는 구현하지 않았습니다. 현재 계약은 순서가 정해진 참여자와 한 줄씩 쌓이는 행을 대상으로 합니다.

## 팀 회의에서 사용할 설명

“다른 팀이 확정한 순서·ID·측정 geometry를 받는 배치 경계를 정리했습니다. 연결점별 높이·좌우 점유량과 연결점 사이 최소 거리를 구분했고, 사용자 고정 좌표와 이동 가능한 선호를 별도로 받습니다. 결과가 이 조건을 지켰는지 요청과 함께 검사하는 검증기는 구현했습니다. 실제 좌표를 만드는 엔진은 이번 범위가 아닙니다. 이 계약은 외부 도구의 API를 복제한 것이 아니라 담당 경계와 사용자 위치 요구를 반영한 선택이며, 실제 모델·측정·렌더러 연결은 팀 합의가 필요합니다.”
