# Firebase Realtime Database security rules

`database.rules.json` is a reviewable starter policy for this project. It is **not deployed automatically**.

Before applying it in Firebase Console, verify the current registration flow with a test account. The policy expects authenticated users to have `users/{uid}/role` set to `admin`, `teacher`, or `student`.

Apply it only from Firebase Console > Realtime Database > Rules after making a database backup. Test administrator, teacher, registered student, pre-registered student, and NFC attendance flows separately.
