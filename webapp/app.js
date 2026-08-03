import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js';
import { getAuth, onAuthStateChanged, signInWithEmailAndPassword, signOut } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js';
import { getDatabase, get, ref, set } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-database.js';

const firebaseConfig = {
  apiKey: 'AIzaSyD5g3A5SqmP7DwNeSxTBBjRGZJuGGhkSrQ',
  authDomain: 'attendanceapp-f3a5a.firebaseapp.com',
  databaseURL: 'https://attendanceapp-f3a5a-default-rtdb.asia-southeast1.firebasedatabase.app',
  projectId: 'attendanceapp-f3a5a'
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getDatabase(app);
const loginView = document.querySelector('#login-view');
const appView = document.querySelector('#app-view');
const content = document.querySelector('#content');
const loginError = document.querySelector('#login-error');
const loginHint = document.querySelector('#login-hint');

const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
const statusClass = status => status === '결석' ? 'absent' : status === '지각' ? 'late' : '';
const attendanceRows = records => records.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)).map(record => `
  <div class="row"><div><strong>${escapeHtml(record.studentName || '이름 없음')}</strong><br><span class="muted">${escapeHtml(record.date)} ${escapeHtml(record.time)} · ${escapeHtml(record.subject || '과목 미지정')}</span></div><span class="status ${statusClass(record.status)}">${escapeHtml(record.status || '출석')}</span></div>`).join('');

function attendanceRecordsFromValue(value, fallbackId = '') {
  const result = [];
  const collect = (node, nodeId) => {
    if (!node || typeof node !== 'object') return;
    if (Object.prototype.hasOwnProperty.call(node, 'date') &&
        (Object.prototype.hasOwnProperty.call(node, 'status') || Object.prototype.hasOwnProperty.call(node, 'studentUid'))) {
      result.push({ id: node.id || nodeId, ...node });
      return;
    }
    Object.entries(node).forEach(([key, child]) => collect(child, key));
  };
  collect(value, fallbackId);
  return result;
}

function flattenAttendance(snapshot) {
  return Object.entries(snapshot.val() || {}).flatMap(([studentId, value]) => attendanceRecordsFromValue(value, studentId));
}

function studentAttendanceRecords(snapshot) {
  return attendanceRecordsFromValue(snapshot.val());
}

function koreanDay(date) {
  const days = ['일', '월', '화', '수', '목', '금', '토'];
  const parts = String(date || '').split('-').map(Number);
  const parsed = parts.length === 3 ? new Date(parts[0], parts[1] - 1, parts[2]) : new Date('');
  return Number.isNaN(parsed.getTime()) ? '' : days[parsed.getUTCDay()];
}

function toMinutes(value) {
  const match = String(value || '').match(/^(\d{1,2}):(\d{2})/);
  return match ? Number(match[1]) * 60 + Number(match[2]) : null;
}

function enrichPersonalAttendance(records, user, timetable) {
  return records.map(record => {
    const day = koreanDay(record.date);
    const recordTime = toMinutes(record.time);
    const matchedLesson = timetable.find(lesson => {
      const lessonDay = String(lesson.day || '').trim().replace('요일', '');
      const start = toMinutes(lesson.startTime);
      const end = toMinutes(lesson.endTime) ?? (start == null ? null : start + 90);
      return lessonDay === day && recordTime != null && start != null && end != null && recordTime >= start && recordTime <= end;
    });
    return {
      ...record,
      studentName: record.studentName || user.name || '이름 없음',
      subject: record.subject || matchedLesson?.subject || '수업'
    };
  });
}

async function loadAttendancePaths(paths) {
  const snapshots = await Promise.all(paths.map(path => get(ref(db, path))));
  return snapshots.flatMap(flattenAttendance);
}

async function renderStudent(user) {
  const path = user.isPreStudent ? `preAttendance/${user.preStudentId || user.uid}` : user.isUnregisteredStudent ? `unregisteredAttendance/${user.unregisteredStudentId || user.uid}` : `attendance/${user.uid}`;
  const [attendanceSnapshot, timetableSnapshot] = await Promise.all([get(ref(db, path)), get(ref(db, `timetable/${user.uid}`))]);
  const timetable = Object.values(timetableSnapshot.val() || {});
  const records = enrichPersonalAttendance(studentAttendanceRecords(attendanceSnapshot), user, timetable);
  content.innerHTML = `<article class="card"><h2>내 출결 기록</h2><p>${escapeHtml(user.grade || '학년 미등록')} · ${escapeHtml(user.className || '반 미등록')}</p><div class="list">${records.length ? attendanceRows(records) : '<p class="empty">출결 기록이 없습니다.</p>'}</div></article>`;
}

async function renderParent(user) {
  const studentId = user.linkedStudentId;
  if (!studentId) { content.innerHTML = '<article class="card"><h2>학부모 연결 필요</h2><p>관리자에게 학생 연결을 요청하세요.</p></article>'; return; }
  const [studentSnapshot, attendanceSnapshot, timetableSnapshot] = await Promise.all([get(ref(db, `users/${studentId}`)), get(ref(db, `attendance/${studentId}`)), get(ref(db, `timetable/${studentId}`))]);
  const student = studentSnapshot.val() || {};
  const records = enrichPersonalAttendance(studentAttendanceRecords(attendanceSnapshot), student, Object.values(timetableSnapshot.val() || {}));
  content.innerHTML = `<article class="card"><h2>${escapeHtml(student.name || '학생')} 출결</h2><p>${escapeHtml(student.grade || '학년 미등록')} · ${escapeHtml(student.className || '반 미등록')}</p><div class="list">${records.length ? attendanceRows(records) : '<p class="empty">출결 기록이 없습니다.</p>'}</div></article>`;
}

async function renderTeacher(user) {
  const snapshot = await get(ref(db, `teacherTimetable/${user.uid}`));
  const lessons = Object.entries(snapshot.val() || {}).map(([id, lesson]) => ({ id, ...lesson })).sort((a, b) => `${a.day}${a.startTime}`.localeCompare(`${b.day}${b.startTime}`));
  content.innerHTML = `<article class="card"><h2>내 시간표</h2><p>수업을 누르면 해당 반의 학생 목록을 확인할 수 있습니다.</p><div class="list">${lessons.length ? lessons.map(lesson => `<button class="lesson-button" data-lesson="${escapeHtml(lesson.id)}">${escapeHtml(lesson.day)} ${escapeHtml(lesson.startTime)}~${escapeHtml(lesson.endTime)}<br>${escapeHtml(lesson.grade)} ${escapeHtml(lesson.className)} · ${escapeHtml(lesson.subject)}</button>`).join('') : '<p class="empty">등록된 수업이 없습니다.</p>'}</div></article>`;
  lessons.forEach(lesson => document.querySelector(`[data-lesson="${CSS.escape(lesson.id)}"]`)?.addEventListener('click', () => renderTeacherLesson(lesson)));
}

async function renderTeacherLesson(lesson) {
  const users = (await get(ref(db, 'users'))).val() || {};
  const normalize = value => String(value || '').replace(/\s|학년/g, '');
  const students = Object.entries(users).map(([uid, value]) => ({ uid, ...value })).filter(student => student.role === 'student' && student.className === lesson.className && normalize(student.grade) === normalize(lesson.grade));
  content.innerHTML = `<article class="card"><button id="back-to-timetable" class="text-button">시간표로 돌아가기</button><h2>${escapeHtml(lesson.grade)} ${escapeHtml(lesson.className)} 출결관리</h2><p>${escapeHtml(lesson.subject)} · ${escapeHtml(lesson.day)} ${escapeHtml(lesson.startTime)}~${escapeHtml(lesson.endTime)}</p><div class="list">${students.length ? students.map(student => `<div class="row"><div><strong>${escapeHtml(student.name)}</strong><br><span class="muted">${escapeHtml(student.grade)} · ${escapeHtml(student.className)}</span></div><select data-status="${escapeHtml(student.uid)}"><option>출석</option><option>지각</option><option>결석</option></select></div>`).join('') : '<p class="empty">이 반에 등록된 학생이 없습니다.</p>'}</div>${students.length ? '<button id="save-attendance">출결 저장</button><p id="save-message" class="hint"></p>' : ''}</article>`;
  document.querySelector('#back-to-timetable').addEventListener('click', () => renderTeacher({ uid: auth.currentUser.uid }));
  document.querySelector('#save-attendance')?.addEventListener('click', () => saveTeacherAttendance(lesson, students));
}

async function saveTeacherAttendance(lesson, students) {
  const button = document.querySelector('#save-attendance');
  const message = document.querySelector('#save-message');
  button.disabled = true;
  message.textContent = '출결을 저장하는 중입니다.';
  const now = new Date();
  const date = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' }).format(now);
  const time = new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', hour12: false }).format(now);
  try {
    await Promise.all(students.map(student => {
      const status = document.querySelector(`[data-status="${CSS.escape(student.uid)}"]`).value;
      const id = `${date}_${student.uid}_${lesson.subject || '수업'}`.replace(/[.#$\[\]/]/g, '_');
      return set(ref(db, `attendance/${student.uid}/${id}`), {
        id, studentUid: student.uid, studentName: student.name || '이름 없음', subject: lesson.subject || '',
        teacher: auth.currentUser.email || '', teacherUid: lesson.teacherUid || auth.currentUser.uid,
        date, time, status, timestamp: Date.now()
      });
    }));
    message.textContent = '출결 저장이 완료되었습니다.';
  } catch (error) {
    message.textContent = `출결 저장 실패: ${error.message || '권한을 확인하세요.'}`;
  } finally { button.disabled = false; }
}

async function renderAdmin() {
  const records = await loadAttendancePaths(['attendance', 'preAttendance', 'unregisteredAttendance']);
  const present = records.filter(record => record.status === '출석').length;
  const late = records.filter(record => record.status === '지각').length;
  const absent = records.filter(record => record.status === '결석').length;
  content.innerHTML = `<article class="card"><h2>전체 출결 조회</h2><p>가입 완료·가입 예정·미가입 학생 기록을 함께 표시합니다.</p><div class="summary"><div><span class="muted">출석</span><strong>${present}</strong></div><div><span class="muted">지각</span><strong>${late}</strong></div><div><span class="muted">결석</span><strong>${absent}</strong></div></div><div class="list">${records.length ? attendanceRows(records) : '<p class="empty">출결 기록이 없습니다.</p>'}</div></article>`;
}

async function renderDashboard(firebaseUser) {
  content.innerHTML = '<article class="card"><p class="empty">데이터를 불러오는 중입니다.</p></article>';
  try {
    const profileSnapshot = await get(ref(db, `users/${firebaseUser.uid}`));
    const profile = profileSnapshot.val();
    if (!profile) throw new Error('사용자 프로필을 찾을 수 없습니다.');
    document.querySelector('#user-name').textContent = profile.name || firebaseUser.email || '사용자';
    document.querySelector('#user-role').textContent = ({ admin: '관리자', teacher: '선생님', parent: '학부모', student: '학생' })[profile.role] || profile.role || '사용자';
    if (profile.role === 'admin') await renderAdmin();
    else if (profile.role === 'teacher') await renderTeacher({ ...profile, uid: firebaseUser.uid });
    else if (profile.role === 'parent') await renderParent(profile);
    else await renderStudent({ ...profile, uid: firebaseUser.uid });
  } catch (error) {
    content.innerHTML = `<article class="card"><h2>데이터를 불러오지 못했습니다</h2><p>${escapeHtml(error.message || 'Firebase 권한과 연결 상태를 확인하세요.')}</p></article>`;
  }
}

document.querySelector('#login-form').addEventListener('submit', async event => {
  event.preventDefault();
  loginError.textContent = '';
  const loginId = document.querySelector('#login-id').value.trim();
  const email = loginId.includes('@') ? loginId : `${loginId}@attendance.com`;
  loginHint.textContent = `로그인 계정: ${email}`;
  try { await signInWithEmailAndPassword(auth, email, document.querySelector('#password').value); }
  catch (error) {
    const messages = {
      'auth/invalid-credential': '아이디 또는 비밀번호가 맞지 않거나 Firebase 로그인 계정이 없습니다.',
      'auth/unauthorized-domain': 'Firebase Authentication의 Authorized domains에 black-c0w.github.io를 추가해야 합니다.',
      'auth/network-request-failed': '네트워크 연결을 확인하세요.',
      'auth/too-many-requests': '로그인 시도가 잠시 제한되었습니다. 잠시 후 다시 시도하세요.',
      'auth/api-key-not-valid': '웹용 Firebase API 키 설정이 필요합니다.'
    };
    loginError.textContent = messages[error.code] || `로그인 실패: ${error.code || error.message}`;
  }
});
document.querySelector('#logout-button').addEventListener('click', () => signOut(auth));
onAuthStateChanged(auth, user => {
  loginView.hidden = Boolean(user);
  appView.hidden = !user;
  if (user) renderDashboard(user);
});
if ('serviceWorker' in navigator) window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js'));
