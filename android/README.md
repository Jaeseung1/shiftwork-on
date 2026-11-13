# 교대온 안드로이드 앱 (갤럭시 위젯)

앱 화면은 웹 앱(레포 루트의 `index.html`)을 그대로 띄우고, 홈 화면 위젯 3종(4x1, 2x1, 1x1)만 안드로이드 코드로 그립니다.

## 구성
- `MainActivity`: WebView. `WEB_APP_URL`이 있으면 그 주소(예: Vercel)를, 없으면 앱 안에 넣은 웹 파일(빌드할 때 레포 루트에서 복사)을 엽니다.
- `ShiftBridge`: 웹 앱(`sync.js`)이 `window.ShiftBridge`로 근무표와 위젯 토큰을 넘깁니다. 서버 없이도 근무표가 넘어오므로 위젯은 서버 없이 동작합니다.
- `RefreshWorker`: 15분마다 위젯을 다시 그립니다. 서버 정보가 있으면 먼저 Supabase `widget_schedule(token)`으로 근무표를 받아 다른 기기에서 바꾼 근무도 반영합니다.
- `core/ShiftCalc`: 근무 계산. 웹 앱과 같은 규칙이며 `ShiftCalcTest`로 웹 앱 결과 18개 시각과 대조합니다.
- `widget/`: 위젯 3종(토스 스타일, 라이트·다크). 0.3(2026-11-12)에서 줄 수를 줄여 다시 그림:
  - 4x1: 왼쪽 "오늘 · 날짜", 근무 이름 + 상태 칩(근무 중은 파랑, 근무 전·퇴근 후·쉬는 날은 회색), 시간과 남은 시간. 오른쪽 회색 카드에 내일(내일이 조근이면 오늘 밤 야근이 이어지는 것이라 "08:30 퇴근"만).
  - 2x1: 근무 이름 + 상태 칩, 퇴근·출근 시각, "내일 ○○ · 시각".
  - 1x1: 근무 이름, 그 아래 한 줄(근무 중이면 파란 퇴근 시각, 아니면 출근 시각이나 "내일 ○○").
  - 한글 글꼴은 줄 높이가 글자 크기의 약 1.5배라 1칸 높이(어림 80dp)에 글 세 줄까지만 넣는다. 폰 글꼴 크기를 키우면(10% 넘게) 덜 중요한 줄 하나를 뺀다(4x1 "오늘 · 날짜"와 "내일 · 날짜", 2x1 내일 줄).

## 빌드 (개발자 컴퓨터, 무료)
1. Android Studio를 설치하고 `android/` 폴더를 연다. (처음 열 때 Gradle·안드로이드 SDK를 내려받는다.)
2. 서버를 쓰려면 먼저 레포 루트의 `config.js`에 Supabase 주소와 anon key를 넣는다(`supabase/README.md`).
3. 기기를 USB로 연결하고 실행(▶)하거나, 설치 파일을 만든다:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```
   결과: `android/app/build/outputs/apk/debug/app-debug.apk`
4. 웹 앱 주소를 쓰려면: `./gradlew assembleDebug -PwebAppUrl=https://<내 주소>`

## 정식 빌드와 서명 키 (업데이트하려면 꼭 필요)
- **0.6(2026-11-13)부터 새 키와 새 앱 ID(`app.gyodaeon`)를 쓴다.** 0.4 설치 뒤 Play 프로텍트가 "잠재적으로 유해한 앱"으로 막아, 예전 키(`app.shiftwork.on`)는 그만 쓴다. 새 키는 Supabase Vault `gyodaeon_android_signing_key_zip_b64`(zip SHA-256 `1a5ce6a4…99ef`, 인증서 SHA-256 `bae3d2d2…9641`)에 있고, 꺼내는 법은 아래 예전 키와 같다(이름만 바꿔서). 아래 예전 키 설명은 기록으로 남겨 둔다.
- 정식 APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
- 서명 키: `android/signing/shiftwork-release.jks`와 비밀번호가 든 `android/keystore.properties`. **둘 다 저장소에 넣지 않는다**(.gitignore). 2026-11-12에 만들어 사용자에게 파일로 전달했다(인증서 SHA-256 `1d9fe2ac…e377`).
- 보관: 사용자 요청으로 Supabase 프로젝트 shiftwork-on의 **Vault**에 키 zip을 base64로 넣어 두었다(이름 `shiftwork_android_signing_key_zip_b64`, zip SHA-256 `357492d8…5b97`). anon·authenticated는 읽을 수 없다.
- 새 작업 세션(컨테이너)에서는 키 파일이 없으므로, Supabase 커넥터로 `select decrypted_secret from vault.decrypted_secrets where name = 'shiftwork_android_signing_key_zip_b64'`를 읽어 `base64 -d > shiftwork-signing-key.zip` → SHA-256 확인 → `android/`에 `keystore.properties`, `android/signing/`에 `shiftwork-release.jks`를 둔다. 키가 없으면 release APK가 서명되지 않는다.
- 같은 키로 빌드해야 이미 설치된 앱 위에 덮어 설치된다. 키를 잃어버리면 앱을 지우고 다시 깔아야 하고, 폰에만 저장된 근무표는 사라진다(로그인해서 서버에 저장된 근무표는 남는다).
- 업데이트할 때는 `app/build.gradle`의 `versionCode`를 1씩 올린다.

## 다른 폰에 설치
정식 APK(`app-release.apk`)를 카카오톡·메일 등으로 보내고, 받는 폰에서 열어 설치한다. 처음에는 "출처를 알 수 없는 앱 설치"를 허용해야 한다. 홈 화면을 길게 눌러 위젯 → 교대온을 고르면 위젯이 놓인다. 위젯은 앱을 한 번 연 뒤부터 근무를 보여 준다.

## 시험
```bash
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```
- `ShiftCalcParityTest`: 웹 앱과 같은 결과인지(18개 시각 + 진행률·순환·표기, 30개 확인).
- `WidgetRenderTest`(Robolectric): 위젯 3종을 실제 안드로이드 레이아웃으로 그려 글자 값, 가로 잘림, 세로 찌그러짐·넘침을 확인하고 그림을 `app/build/widget-shots/`에 남긴다. 칸 크기는 작게 잡은 어림값(4x1 340×80dp, 2x1 160×80dp, 1x1 75×80dp)이고, 글꼴 크기 120%도 시험한다. 그림 사본: `design/images/android/`.

## 확인한 것과 못 한 것 (2026-11-12)
- 확인(0.3): 이 컨테이너에서 Android SDK 34·AGP 8.5.2·Gradle 8.7로 정식 서명 `assembleRelease` 성공, 단위 시험 11개 통과(근무 계산 1 + 위젯 그림 10), lint 오류 0(경고 16: 작은 글자 10sp, API 31 이상 속성, 자바스크립트 사용, 새 라이브러리 버전 등).
- 그림 시험으로 찾아 고친 것: 4x1 높이 부족(시간·날짜 숫자 잘림), 2x1 오른쪽 카드 말줄임, 2x1·1x1 넷째 줄 찌그러짐. 0.3: 새 디자인의 세로 넘침(한글 줄 높이), 글꼴 120%에서 1x1 "…" 줄임과 4x1 가로 잘림.
- 못 한 것: 실기기·에뮬레이터(이 컨테이너는 가상화 KVM이 없음). 삼성 런처의 실제 칸 크기와 글꼴(One UI)은 다를 수 있다.
- debug APK는 개발용 서명이다. 오래 쓰려면 release 서명 키를 만들어 `assembleRelease`로 빌드한다(키를 잃어버리면 업데이트 설치가 안 된다).

## 새 버전 배포 (0.7부터, 앱 안 업데이트 기능 없음)
- 0.4에 넣은 "앱 안 새 버전 알림" 뒤로 Play 프로텍트가 계속 "잠재적으로 유해한 앱"으로 막았다(0.4·0.5·0.6, 새 키·새 앱 ID에서도). 0.5·0.6은 자바 코드만 뺐고 화면(sync.js)에 확인 기능이 남아 있었다. 0.7에서 업데이트 기능을 화면까지 모두 지우고, 아이폰 전용 코드(단축어 주소·잠금 화면 위젯 코드 만들기)도 갤럭시 앱에는 넣지 않는다(`copyWebAssets`가 `// <web-only>` 블록과 `data-web-only` 줄을 뺌). 구글이 기준을 공개하지 않아 원인은 짐작이다.
- 새 버전은 받는 곳 페이지 `https://jaeseung1.github.io/shiftwork-on/downloads/`에서 직접 받는다. 올리는 순서: versionCode 올리기 → 새 키로 assembleRelease → `downloads/app-<버전>.apk`(예전 것 지우기) → `downloads/version.json` 고치기 → main에 올리기.
