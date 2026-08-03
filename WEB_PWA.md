# iPhone용 웹앱(PWA)

웹앱은 `webapp/` 폴더에 있습니다. GitHub Pages에 이 폴더가 배포되면 다음 주소로 접속합니다.

`https://black-c0w.github.io/attendance-app-download/webapp/`

## iPhone 설치

1. Safari로 위 주소를 엽니다.
2. 공유 버튼을 누릅니다.
3. **홈 화면에 추가**를 선택합니다.
4. 홈 화면의 `출결관리` 아이콘으로 실행합니다.

## Firebase에서 한 번 설정할 것

Firebase Console → Authentication → Settings → Authorized domains에 `black-c0w.github.io`를 추가합니다.

웹앱은 기존 Firebase Authentication과 Realtime Database를 그대로 사용합니다. 따라서 현재 Android 앱에서 사용하는 Firebase Rules에 `users`, `attendance`, `preAttendance`, `unregisteredAttendance`, `teacherTimetable`의 역할별 읽기 권한이 있어야 합니다.
