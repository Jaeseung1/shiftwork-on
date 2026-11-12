# 교대온 안드로이드 앱 (갤럭시 위젯)

앱 화면은 웹 앱(레포 루트의 `index.html`)을 그대로 띄우고, 홈 화면 위젯 3종(4x1, 2x1, 1x1)만 안드로이드 코드로 그립니다.

## 구성
- `MainActivity`: WebView. `WEB_APP_URL`이 있으면 그 주소(예: Vercel)를, 없으면 앱 안에 넣은 웹 파일(빌드할 때 레포 루트에서 복사)을 엽니다.
- `ShiftBridge`: 웹 앱(`sync.js`)이 `window.ShiftBridge`로 근무표와 위젯 토큰을 넘깁니다. 서버 없이도 근무표가 넘어오므로 위젯은 서버 없이 동작합니다.
- `RefreshWorker`: 15분마다 위젯을 다시 그립니다. 서버 정보가 있으면 먼저 Supabase `widget_schedule(token)`으로 근무표를 받아 가족이 바꾼 근무도 반영합니다.
- `core/ShiftCalc`: 근무 계산. 웹 앱과 같은 규칙이며 `ShiftCalcTest`로 웹 앱 결과 18개 시각과 대조합니다.
- `widget/`: 위젯 3종. 디자인은 `design/pages/widget.html`(토스 스타일, 라이트·다크).

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
- 정식 APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
- 서명 키: `android/signing/shiftwork-release.jks`와 비밀번호가 든 `android/keystore.properties`. **둘 다 저장소에 넣지 않는다**(.gitignore). 2026-11-12에 만들어 사용자에게 파일로 전달했다(인증서 SHA-256 `1d9fe2ac…e377`).
- 보관: 사용자 요청으로 Supabase 프로젝트 shiftwork-on의 **Vault**에 키 zip을 base64로 넣어 두었다(이름 `shiftwork_android_signing_key_zip_b64`, zip SHA-256 `357492d8…5b97`). anon·authenticated는 읽을 수 없다.
- 새 작업 세션(컨테이너)에서는 키 파일이 없으므로, Supabase 커넥터로 `select decrypted_secret from vault.decrypted_secrets where name = 'shiftwork_android_signing_key_zip_b64'`를 읽어 `base64 -d > shiftwork-signing-key.zip` → SHA-256 확인 → `android/`에 `keystore.properties`, `android/signing/`에 `shiftwork-release.jks`를 둔다. 키가 없으면 release APK가 서명되지 않는다.
- 같은 키로 빌드해야 이미 설치된 앱 위에 덮어 설치된다. 키를 잃어버리면 앱을 지우고 다시 깔아야 하고, 폰에만 저장된 근무표는 사라진다(로그인해서 서버에 저장된 근무표는 남는다).
- 업데이트할 때는 `app/build.gradle`의 `versionCode`를 1씩 올린다.

## 가족 폰에 설치
정식 APK(`app-release.apk`)를 카카오톡·메일 등으로 보내고, 가족 폰에서 열어 설치한다. 처음에는 "출처를 알 수 없는 앱 설치"를 허용해야 한다. 홈 화면을 길게 눌러 위젯 → 교대온을 고르면 위젯이 놓인다. 위젯은 앱을 한 번 연 뒤부터 근무를 보여 준다.

## 시험
```bash
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```
- `ShiftCalcParityTest`: 웹 앱과 같은 결과인지(18개 시각 + 진행률·순환·표기, 30개 확인).
- `WidgetRenderTest`(Robolectric): 위젯 3종을 실제 안드로이드 레이아웃으로 그려 글자 값, 가로 잘림, 세로 찌그러짐·넘침을 확인하고 그림을 `app/build/widget-shots/`에 남긴다. 칸 크기는 어림값(4x1 340×80dp, 2x1 160×80dp, 1x1 75×80dp). 그림 사본: `design/images/android/`.

## 확인한 것과 못 한 것 (2026-11-12)
- 확인: 이 컨테이너에서 Android SDK 34·AGP 8.5.2·Gradle 8.7로 `assembleDebug` 성공, 단위 시험 6개 통과, lint 오류 0(경고만: 작은 글자, 자바스크립트 사용 등).
- 그림 시험으로 찾아 고친 것: 4x1 높이 부족(시간·날짜 숫자 잘림), 2x1 오른쪽 카드 말줄임, 2x1·1x1 넷째 줄 찌그러짐.
- 못 한 것: 실기기·에뮬레이터(이 컨테이너는 가상화 KVM이 없음). 삼성 런처의 실제 칸 크기와 글꼴(One UI)은 다를 수 있다.
- debug APK는 개발용 서명이다. 오래 쓰려면 release 서명 키를 만들어 `assembleRelease`로 빌드한다(키를 잃어버리면 업데이트 설치가 안 된다).
