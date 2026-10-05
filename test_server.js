const http = require('http');
const assert = require('assert');

const BASE_URL = 'http://localhost:3000';
let passed = 0;
let failed = 0;

function runTest(name, fn) {
  return new Promise(async (resolve) => {
    try {
      await fn();
      console.log(`  ✅ PASS: ${name}`);
      passed++;
    } catch (err) {
      console.error(`  ❌ FAIL: ${name}`);
      console.error(`     Error: ${err.message}`);
      failed++;
    }
    resolve();
  });
}

function request(options, bodyData) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          resolve({ status: res.statusCode, headers: res.headers, body: json, raw: data });
        } catch (_) {
          resolve({ status: res.statusCode, headers: res.headers, body: null, raw: data });
        }
      });
    });
    req.on('error', reject);
    if (bodyData) {
      req.write(typeof bodyData === 'string' ? bodyData : JSON.stringify(bodyData));
    }
    req.end();
  });
}

async function main() {
  console.log('\n======================================================');
  console.log('🚀 TOMANINA SERVER & API AUTOMATED TEST SUITE');
  console.log('======================================================\n');

  // --- 1. UNIT & INPUT VALIDATION TESTS ---
  console.log('--- 1. Unit & Input Validation Tests ---');

  await runTest('Validation: rejects empty email and password', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: '', password: '' });
    assert.strictEqual(res.status, 400);
    assert.strictEqual(res.body.success, false);
  });

  await runTest('Validation: rejects malformed email format', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: 'not-an-email', password: 'ValidPassword123' });
    assert.strictEqual(res.status, 400);
    assert.ok(res.body.message.includes('البريد'));
  });

  await runTest('Validation: rejects password shorter than 6 characters', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: 'counselor@school.edu.sa', password: '123' });
    assert.strictEqual(res.status, 400);
    assert.ok(res.body.message.includes('خانة'));
  });

  await runTest('Validation: handles malformed JSON without crashing server', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, '{ broken json: true ');
    assert.strictEqual(res.status, 400);
    assert.strictEqual(res.body.success, false);
  });

  // --- 2. INTEGRATION TESTS FOR API ENDPOINTS ---
  console.log('\n--- 2. Integration Tests for API Endpoints ---');

  let validToken = null;

  await runTest('API Endpoint: GET / serves index.html with 200 OK', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/',
      method: 'GET'
    });
    assert.strictEqual(res.status, 200);
    assert.ok(res.raw.includes('طمأنينة'));
  });

  await runTest('API Endpoint: POST /api/counselor/login authenticates with valid credentials', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: 'mazensabaeleh@gmail.com', password: 'mazen@1435522' });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
    assert.strictEqual(res.body.name, 'أ. مازن السبايله');
    assert.strictEqual(res.body.role, 'SUPER_ADMIN');
    assert.ok(res.body.token.startsWith('tmn_sec_'));
    validToken = res.body.token;
  });

  await runTest('API Endpoint: GET /api/counselor/verify validates legitimate session token', async () => {
    assert.ok(validToken, 'Valid token must be available from login test');
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/verify',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${validToken}` }
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.valid, true);
    assert.strictEqual(res.body.email, 'mazensabaeleh@gmail.com');
  });

  await runTest('API Endpoint: GET /api/counselor/verify rejects requests without authorization token', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/verify',
      method: 'GET'
    });
    assert.strictEqual(res.status, 401);
    assert.strictEqual(res.body.valid, false);
  });

  await runTest('API Endpoint: GET /api/counselor/verify rejects forged/tampered session tokens (IDOR)', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/verify',
      method: 'GET',
      headers: { 'Authorization': 'Bearer tmn_sec_forged_fake_token_123456789' }
    });
    assert.strictEqual(res.status, 401);
    assert.strictEqual(res.body.valid, false);
  });

  // --- 3. SECURITY & HARDENING TESTS ---
  console.log('\n--- 3. Security & Hardening Tests ---');

  await runTest('Security: HTTP Security Headers are strictly enforced', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/',
      method: 'GET'
    });
    assert.strictEqual(res.headers['x-content-type-options'], 'nosniff');
    assert.strictEqual(res.headers['x-frame-options'], 'SAMEORIGIN');
    assert.strictEqual(res.headers['referrer-policy'], 'strict-origin-when-cross-origin');
    assert.ok(res.headers['strict-transport-security']);
    assert.ok(res.headers['content-security-policy']);
  });

  await runTest('Security: Path Traversal (../) is blocked with 403 Forbidden', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/../../../../etc/passwd',
      method: 'GET'
    });
    assert.strictEqual(res.status, 403);
    assert.ok(res.raw.includes('محظور'));
  });

  await runTest('Security: SQL Injection payloads in login do not bypass auth', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: "admin' OR '1'='1", password: "' OR '1'='1" });
    assert.strictEqual(res.status, 400); // Caught by email format validator
    assert.strictEqual(res.body.success, false);
  });

  await runTest('Security: XSS payloads in login do not trigger script execution', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: "<script>alert(1)</script>@test.com", password: "<svg/onload=alert(1)>" });
    assert.strictEqual(res.status, 400);
    assert.strictEqual(res.body.success, false);
  });

  await runTest('Security: Account Lockout activates after 5 consecutive failed attempts', async () => {
    const uniqueEmail = `attacker_${Date.now()}@school.edu.sa`;
    for (let i = 1; i <= 5; i++) {
      await request({
        hostname: 'localhost',
        port: 3000,
        path: '/api/counselor/login',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      }, { email: uniqueEmail, password: 'wrong_password_123' });
    }
    // 6th attempt should be locked out
    const lockedRes = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: uniqueEmail, password: 'wrong_password_123' });
    assert.strictEqual(lockedRes.status, 429);
    assert.ok(lockedRes.body.message.includes('إيقاف المحاولات مؤقتاً'));
  });

  // --- 4. END-TO-END FLOW TESTS ---
  console.log('\n--- 4. End-to-End User Flow Tests ---');

  await runTest('E2E Flow: Full Counselor Journey (Login -> Token Receive -> Verify Session -> Reject Tampered)', async () => {
    // 1. Login
    const loginRes = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: 'mazensabaeleh@gmail.com', password: 'mazen@1435522' });
    assert.strictEqual(loginRes.status, 200);
    const token = loginRes.body.token;

    // 2. Access protected endpoint with valid session
    const verifyRes = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/verify',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    assert.strictEqual(verifyRes.status, 200);
    assert.strictEqual(verifyRes.body.valid, true);

    // 3. Attempt access with tampered token
    const tamperedRes = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/verify',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}_tampered` }
    });
    assert.strictEqual(tamperedRes.status, 401);
  });

  await runTest('API Endpoint: GET /api/system/health returns 200 with HEALTHY status', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/system/health',
      method: 'GET'
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.status, 'HEALTHY');
    assert.strictEqual(res.body.securityHeadersActive, true);
  });

  await runTest('API Endpoint: GET /api/system/errors authorizes only authenticated supervisors', async () => {
    // Unauthenticated request
    const unauthRes = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/system/errors',
      method: 'GET'
    });
    assert.strictEqual(unauthRes.status, 401);

    // Authenticated request
    const loginRes = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { email: 'mazensabaeleh@gmail.com', password: 'mazen@1435522' });
    const token = loginRes.body.token;

    const authRes = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/system/errors',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    assert.strictEqual(authRes.status, 200);
    assert.strictEqual(authRes.body.success, true);
    assert.strictEqual(authRes.body.status, 'MONITORING_ACTIVE');
  });

  // --- 5. TEAM MANAGEMENT PERMISSIONS TESTS ---
  console.log('\n--- 5. Team Management Permissions Tests ---');

  let newMemberId = null;
  const testMemberEmail = `member_${Date.now()}@school.edu.sa`;

  await runTest('API Endpoint: GET /api/counselor/team returns members list for authenticated supervisor', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/team',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${validToken}` }
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
    assert.ok(Array.isArray(res.body.members));
    assert.ok(res.body.members.some(m => m.email === 'mazensabaeleh@gmail.com' && m.isOwner === true));
  });

  await runTest('API Endpoint: POST /api/counselor/team adds a new member by email', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/team',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${validToken}`
      }
    }, {
      name: 'أ. طارق الشمري',
      email: testMemberEmail,
      role: 'وكيل المدرسة لشؤون الطلاب'
    });
    assert.strictEqual(res.status, 201);
    assert.strictEqual(res.body.success, true);
    assert.strictEqual(res.body.member.email, testMemberEmail);
    assert.strictEqual(res.body.member.isOwner, false);
    newMemberId = res.body.member.id;
  });

  await runTest('API Endpoint: DELETE /api/counselor/team/:id removes member successfully', async () => {
    assert.ok(newMemberId, 'newMemberId must be valid');
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: `/api/counselor/team/${newMemberId}`,
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${validToken}` }
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
  });

  await runTest('API Endpoint: DELETE /api/counselor/team/1 rejects deletion of Super Admin owner', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/team/1',
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${validToken}` }
    });
    assert.strictEqual(res.status, 403);
    assert.strictEqual(res.body.success, false);
  });

  // --- 6. CONFIDENTIAL STUDENT TICKETS TESTS ---
  console.log('\n--- 6. Confidential Student Tickets Tests ---');

  let testTicketId = null;

  let testTicketCode = null;

  await runTest('API Endpoint: POST /api/tickets allows student to submit confidential message', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/tickets',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, {
      message: 'أحتاج استشارة سرية بخصوص ضغوط الاختبارات',
      contact: 'طالب في الصف الثالث الثانوي',
      category: 'استشارة دراسية واختبارات'
    });
    assert.strictEqual(res.status, 201);
    assert.strictEqual(res.body.success, true);
    assert.ok(res.body.ticket.code.startsWith('TKT-'));
    assert.strictEqual(res.body.ticket.status, 'NEW');
    testTicketId = res.body.ticket.id;
    testTicketCode = res.body.ticket.code;
  });

  await runTest('API Endpoint: GET /api/tickets/track allows student to track ticket by code', async () => {
    assert.ok(testTicketCode, 'testTicketCode must be valid');
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: `/api/tickets/track?code=${testTicketCode}`,
      method: 'GET'
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
    assert.strictEqual(res.body.ticket.code, testTicketCode);
    assert.strictEqual(res.body.ticket.status, 'NEW');
    assert.strictEqual(res.body.ticket.hasReply, false);
  });

  await runTest('API Endpoint: GET /api/counselor/tickets retrieves tickets for authorized Super Admin', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: '/api/counselor/tickets',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${validToken}` }
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
    assert.ok(Array.isArray(res.body.tickets));
    assert.ok(res.body.tickets.some(t => t.id === testTicketId));
  });

  await runTest('API Endpoint: POST /api/counselor/tickets/:id/reply sends guidance reply to student', async () => {
    assert.ok(testTicketId, 'testTicketId must be valid');
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: `/api/counselor/tickets/${testTicketId}/reply`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${validToken}`
      }
    }, {
      reply: 'أهلاً بك يا بني. ضغوط الاختبارات طبيعية ويمكن تنظيمها بجدول مذاكرة هادئ. نحن فخورون بك.',
      status: 'RESOLVED'
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
    assert.strictEqual(res.body.ticket.status, 'RESOLVED');
    assert.ok(res.body.ticket.counselorReply.includes('أهلاً بك'));
  });

  await runTest('API Endpoint: GET /api/tickets/track shows counselor reply to student', async () => {
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: `/api/tickets/track?code=${testTicketCode}`,
      method: 'GET'
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
    assert.strictEqual(res.body.ticket.status, 'RESOLVED');
    assert.strictEqual(res.body.ticket.hasReply, true);
    assert.ok(res.body.ticket.counselorReply.includes('ضغوط الاختبارات'));
  });

  await runTest('API Endpoint: PATCH /api/counselor/tickets/:id updates ticket status', async () => {
    assert.ok(testTicketId, 'testTicketId must be valid');
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: `/api/counselor/tickets/${testTicketId}`,
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${validToken}`
      }
    }, { status: 'IN_PROGRESS' });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
    assert.strictEqual(res.body.ticket.status, 'IN_PROGRESS');
  });

  await runTest('API Endpoint: DELETE /api/counselor/tickets/:id removes ticket cleanly', async () => {
    assert.ok(testTicketId, 'testTicketId must be valid');
    const res = await request({
      hostname: 'localhost',
      port: 3000,
      path: `/api/counselor/tickets/${testTicketId}`,
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${validToken}` }
    });
    assert.strictEqual(res.status, 200);
    assert.strictEqual(res.body.success, true);
  });

  console.log('\n======================================================');
  console.log(`📊 TEST RESULTS: ${passed} PASSED | ${failed} FAILED | TOTAL: ${passed + failed}`);
  console.log('======================================================\n');

  if (failed > 0) {
    process.exit(1);
  }
}

main().catch((err) => {
  console.error('Fatal test runner error:', err);
  process.exit(1);
});
