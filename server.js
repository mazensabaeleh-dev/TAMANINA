const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const errorLogRingBuffer = [];

// --- Structured Logger ---
const logger = {
  formatMeta(meta) {
    return Object.keys(meta).length ? ' ' + JSON.stringify(meta) : '';
  },
  info(msg, meta = {}) {
    console.log(`[${new Date().toISOString()}] [INFO] ${msg}${this.formatMeta(meta)}`);
  },
  warn(msg, meta = {}) {
    console.warn(`[${new Date().toISOString()}] [WARN] ${msg}${this.formatMeta(meta)}`);
  },
  error(msg, meta = {}) {
    console.error(`[${new Date().toISOString()}] [ERROR] ${msg}${this.formatMeta(meta)}`);
    errorLogRingBuffer.unshift({ timestamp: new Date().toISOString(), message: msg, ...meta });
    if (errorLogRingBuffer.length > 50) errorLogRingBuffer.pop();
  }
};

// Global error handlers to prevent unhandled promise rejections or uncaught errors from crashing process
process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Promise Rejection detected', { reason: String(reason) });
});

process.on('uncaughtException', (err) => {
  logger.error('Uncaught Exception detected in server process', { error: err.message, stack: err.stack });
});

// Load environment variables from .env file securely
function loadEnv() {
  try {
    const envPath = path.join(__dirname, '.env');
    if (fs.existsSync(envPath)) {
      const content = fs.readFileSync(envPath, 'utf8');
      content.split('\n').forEach(line => {
        const trimmed = line.trim();
        if (trimmed && !trimmed.startsWith('#')) {
          const idx = trimmed.indexOf('=');
          if (idx !== -1) {
            const key = trimmed.substring(0, idx).trim();
            const val = trimmed.substring(idx + 1).trim();
            if (!process.env[key]) {
              process.env[key] = val;
            }
          }
        }
      });
      logger.info('.env configuration loaded successfully');
    }
  } catch (err) {
    logger.warn('Could not read .env file, continuing with default runtime environment', { error: err.message });
  }
}
loadEnv();

// The application server runs behind Nginx reverse proxy on DEFAULT_APP_PORT (3000)
// Never bind to 8080 as Nginx owns port 8080 in this container architecture
const PORT = parseInt(process.env.DEFAULT_APP_PORT || '3000', 10);
const WEB_DIR = path.resolve(__dirname, 'app/src/main/assets/web');
const HTML_FILE = path.join(WEB_DIR, 'index.html');

// Environment secrets loaded from .env
const ADMIN_EMAIL = (process.env.ADMIN_EMAIL || 'mazensabaeleh@gmail.com').trim().toLowerCase();
const ADMIN_PASSWORD = (process.env.ADMIN_PASSWORD || 'mazen@1435522').trim();
const ALLOWED_ORIGIN = process.env.ALLOWED_ORIGIN || 'https://ais-dev-edmcimlo5luskkwbjlby34-355668188961.europe-west3.run.app';

// In-memory Rate Limiting & Account Lockout Store
const loginAttempts = new Map(); // ip_email -> { count, lockedUntil, lastAttempt }
const MAX_ATTEMPTS = 5;
const LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes lockout

// In-memory Cryptographic Session Store
const activeSessions = new Map(); // token -> { email, expiresAt, role }
const SESSION_TTL_MS = 8 * 60 * 60 * 1000; // 8 hours

// Team Members Storage & Persistence
const TEAM_MEMBERS_FILE = path.join(__dirname, 'tomanina_team.json');
let serverTeamMembers = [
  {
    id: 1,
    name: 'أ. مازن السبايله',
    email: 'mazensabaeleh@gmail.com',
    role: 'المشرف العام',
    permissions: 'كامل الصلاحيات (الوصول الحصري للرسائل السرية وإدارة الفريق)',
    isOwner: true
  },
  {
    id: 2,
    name: 'د. عبد الرحمن القرني',
    email: 'counselor.amal@edu.sa',
    role: 'مرشد طلابي مساعد',
    permissions: 'اطلاع على بيانات واستجابات النبضات فقط (محظور من الرسائل السرية)',
    isOwner: false
  },
  {
    id: 3,
    name: 'أ. سارة المنصور',
    email: 'sarah.psych@edu.sa',
    role: 'أخصائية استجابات وتوجيه',
    permissions: 'اطلاع على بيانات واستجابات النبضات فقط (محظور من الرسائل السرية)',
    isOwner: false
  }
];

function loadTeamMembers() {
  try {
    if (fs.existsSync(TEAM_MEMBERS_FILE)) {
      const data = JSON.parse(fs.readFileSync(TEAM_MEMBERS_FILE, 'utf8'));
      if (Array.isArray(data) && data.length > 0) {
        serverTeamMembers = data;
        logger.info('Team members loaded from file', { count: serverTeamMembers.length });
        return;
      }
    }
    saveTeamMembers();
  } catch (e) {
    logger.warn('Failed to load team members file, using defaults', { error: e.message });
  }
}

function saveTeamMembers() {
  try {
    fs.writeFileSync(TEAM_MEMBERS_FILE, JSON.stringify(serverTeamMembers, null, 2), 'utf8');
  } catch (e) {
    logger.warn('Failed to save team members file', { error: e.message });
  }
}
loadTeamMembers();

// Confidential Student Support Tickets Storage & Persistence
const TICKETS_FILE = path.join(__dirname, 'tomanina_tickets.json');
let serverTickets = [];

function loadTickets() {
  try {
    if (fs.existsSync(TICKETS_FILE)) {
      const data = JSON.parse(fs.readFileSync(TICKETS_FILE, 'utf8'));
      if (Array.isArray(data)) {
        serverTickets = data;
        logger.info('Tickets loaded from file', { count: serverTickets.length });
        return;
      }
    }
    // Default initial mock if empty for demonstration
    serverTickets = [
      {
        id: 1700000000001,
        code: 'TKT-1042',
        category: 'استشارة دراسية واختبارات',
        message: 'أشعر بقلق وتوتر من اختبارات منتصف الفصل وأحتاج نصيحة لتنظيم وقتي بهدوء.',
        contact: 'طالب في الصف الثاني الثانوي',
        status: 'NEW',
        timestamp: Date.now() - 3600000
      }
    ];
    saveTickets();
  } catch (e) {
    logger.warn('Failed to load tickets file, using defaults', { error: e.message });
  }
}

function saveTickets() {
  try {
    fs.writeFileSync(TICKETS_FILE, JSON.stringify(serverTickets, null, 2), 'utf8');
  } catch (e) {
    logger.warn('Failed to save tickets file', { error: e.message });
  }
}
loadTickets();

// Periodic Memory Leak Prevention: purge expired sessions and stale lockout entries every 10 minutes
const cleanupTimer = setInterval(() => {
  const now = Date.now();
  let expiredSessionsCount = 0;
  for (const [token, session] of activeSessions.entries()) {
    if (!session || session.expiresAt <= now) {
      activeSessions.delete(token);
      expiredSessionsCount++;
    }
  }
  let prunedAttemptsCount = 0;
  for (const [key, attempt] of loginAttempts.entries()) {
    if (attempt && attempt.lockedUntil <= now && (now - (attempt.lastAttempt || 0) > 3600000)) {
      loginAttempts.delete(key);
      prunedAttemptsCount++;
    }
  }
  if (expiredSessionsCount > 0 || prunedAttemptsCount > 0) {
    logger.info('Memory cleanup completed', { prunedSessions: expiredSessionsCount, prunedAttempts: prunedAttemptsCount });
  }
}, 10 * 60 * 1000);
cleanupTimer.unref(); // Prevent timer from keeping the process alive on exit

// Secure Constant-Time String & Hash Comparison
function constantTimeCompare(input, expected) {
  try {
    const inputHash = crypto.createHash('sha256').update(String(input || '')).digest();
    const expectedHash = crypto.createHash('sha256').update(String(expected || '')).digest();
    return crypto.timingSafeEqual(inputHash, expectedHash);
  } catch (err) {
    logger.error('Error during constant-time comparison', { error: err.message });
    return false;
  }
}

// Strict Input Validator
function validateLoginInput(data) {
  if (!data || typeof data !== 'object') {
    return { valid: false, error: 'تنسيق البيانات غير صالح' };
  }
  const email = typeof data.email === 'string' ? data.email.trim().toLowerCase() : '';
  const password = typeof data.password === 'string' ? data.password.trim() : '';

  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!email || !emailRegex.test(email) || email.length > 254) {
    return { valid: false, error: 'البريد الإلكتروني المدخل غير صالح' };
  }
  if (!password || password.length < 6 || password.length > 128) {
    return { valid: false, error: 'كلمة المرور يجب أن تكون بين 6 و 128 خانة' };
  }
  return { valid: true, email, password };
}

// Security Headers Generator
function applySecurityHeaders(res, req) {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
  res.setHeader(
    'Content-Security-Policy',
    "default-src 'self' https://fonts.googleapis.com https://fonts.gstatic.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; frame-ancestors 'self' https://*.google.com https://localhost.corp.google.com:26001;"
  );

  // Strict Domain-Restricted CORS
  const origin = req.headers.origin;
  if (origin && (origin === ALLOWED_ORIGIN || origin.endsWith('.run.app') || origin === 'http://localhost:3000')) {
    res.setHeader('Access-Control-Allow-Origin', origin);
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    res.setHeader('Access-Control-Allow-Credentials', 'true');
  }
}

const server = http.createServer((req, res) => {
  // Prevent socket hang-ups and uncaught client abort errors
  req.on('error', (err) => {
    logger.warn('Client request stream error', { error: err.message, url: req.url });
  });
  res.on('error', (err) => {
    logger.warn('Response stream error', { error: err.message });
  });

  try {
    applySecurityHeaders(res, req);

    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    const pathname = path.normalize(parsedUrl.pathname);
    const clientIp = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    // 1. Hardened Login Endpoint with Rate Limiting and Account Lockout
    if (pathname === '/api/counselor/login' && req.method === 'POST') {
      let body = '';
      let isBodyOverflow = false;

      req.on('data', chunk => {
        if (isBodyOverflow) return;
        body += chunk;
        if (body.length > 16384) {
          isBodyOverflow = true;
          logger.warn('Request payload overflow detected and rejected', { ip: clientIp, length: body.length });
          res.writeHead(413, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ success: false, message: 'حجم البيانات المرسلة كبير جداً' }));
          req.destroy();
        }
      });

      req.on('end', () => {
        if (isBodyOverflow) return;

        try {
          const parsed = JSON.parse(body || '{}');
          const validation = validateLoginInput(parsed);

          if (!validation.valid) {
            res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(JSON.stringify({ success: false, message: validation.error }));
            return;
          }

          const { email, password } = validation;
          const lockoutKey = `${clientIp}_${email}`;
          const now = Date.now();
          const attemptData = loginAttempts.get(lockoutKey) || { count: 0, lockedUntil: 0, lastAttempt: now };
          attemptData.lastAttempt = now;

          // Check if account is temporarily locked
          if (attemptData.lockedUntil > now) {
            const minutesLeft = Math.ceil((attemptData.lockedUntil - now) / 60000);
            logger.warn('Login attempt on locked account', { email, ip: clientIp, minutesLeft });
            res.writeHead(429, {
              'Content-Type': 'application/json; charset=utf-8',
              'Retry-After': String(minutesLeft * 60)
            });
            res.end(JSON.stringify({
              success: false,
              message: `تم إيقاف المحاولات مؤقتاً لأسباب أمنية لكثرة المحاولات الخاطئة. يرجى المحاولة بعد ${minutesLeft} دقيقة.`
            }));
            return;
          }

          // Secure constant-time authentication check
          const isEmailMatch = constantTimeCompare(email, ADMIN_EMAIL);
          const isPassMatch = constantTimeCompare(password, ADMIN_PASSWORD);

          if (isEmailMatch && isPassMatch) {
            // Reset attempts upon success
            loginAttempts.delete(lockoutKey);

            // Generate cryptographically secure session token (256-bit CSPRNG)
            const sessionToken = 'tmn_sec_' + crypto.randomBytes(32).toString('hex');
            activeSessions.set(sessionToken, {
              email: ADMIN_EMAIL,
              role: 'SUPER_ADMIN',
              expiresAt: now + SESSION_TTL_MS
            });

            logger.info('Counselor logged in successfully', { email, ip: clientIp });

            res.writeHead(200, {
              'Content-Type': 'application/json; charset=utf-8',
              'Cache-Control': 'no-store, no-cache, must-revalidate'
            });
            res.end(JSON.stringify({
              success: true,
              name: 'أ. مازن السبايله',
              role: 'SUPER_ADMIN',
              token: sessionToken
            }));
          } else {
            // Increment failed attempt counter and apply lockout if limit exceeded
            attemptData.count = (attemptData.count || 0) + 1;
            if (attemptData.count >= MAX_ATTEMPTS) {
              attemptData.lockedUntil = now + LOCKOUT_DURATION_MS;
              logger.warn('Account lockout triggered', { email, ip: clientIp, attempts: attemptData.count });
            } else {
              logger.warn('Failed login attempt', { email, ip: clientIp, attemptNumber: attemptData.count });
            }
            loginAttempts.set(lockoutKey, attemptData);

            res.writeHead(401, {
              'Content-Type': 'application/json; charset=utf-8',
              'Cache-Control': 'no-store'
            });
            res.end(JSON.stringify({
              success: false,
              message: 'بيانات الاعتماد غير صحيحة. يرجى مراجعة المشرف العام.'
            }));
          }
        } catch (err) {
          logger.warn('JSON parsing error in login body', { error: err.message, ip: clientIp });
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ success: false, message: 'طلب غير صالح' }));
        }
      });
      return;
    }

    // 2. Server-side Session Verification Endpoint
    if (pathname === '/api/counselor/verify' && req.method === 'GET') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (session && session.expiresAt > Date.now()) {
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ valid: true, email: session.email, role: session.role }));
      } else {
        if (session) activeSessions.delete(token);
        res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ valid: false, message: 'الجلسة منتهية أو غير صالحة' }));
      }
      return;
    }

    // 3. System Health Check Endpoint
    if (pathname === '/api/system/health' && req.method === 'GET') {
      res.writeHead(200, {
        'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store'
      });
      res.end(JSON.stringify({
        status: 'HEALTHY',
        uptimeSeconds: Math.floor(process.uptime()),
        timestamp: new Date().toISOString(),
        activeSessionsCount: activeSessions.size,
        securityHeadersActive: true
      }));
      return;
    }

    // 4. System Error Monitoring Endpoint (Admin Protected)
    if (pathname === '/api/system/errors' && req.method === 'GET') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (session && session.expiresAt > Date.now() && session.role === 'SUPER_ADMIN') {
        res.writeHead(200, {
          'Content-Type': 'application/json; charset=utf-8',
          'Cache-Control': 'no-store'
        });
        res.end(JSON.stringify({
          success: true,
          status: 'MONITORING_ACTIVE',
          uptime: Math.floor(process.uptime()),
          loggedErrorsCount: errorLogRingBuffer.length,
          recentErrors: errorLogRingBuffer
        }));
      } else {
        res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'غير مصرح بالوصول إلى سجل الأخطاء' }));
      }
      return;
    }

    // 5. Team Members Management Endpoints (Super Admin Mazen Al-Sabaileh)
    if (pathname === '/api/counselor/team' && req.method === 'GET') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (!session || session.expiresAt <= Date.now()) {
        res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'غير مصرح بالوصول' }));
        return;
      }

      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({
        success: true,
        members: serverTeamMembers
      }));
      return;
    }

    if (pathname === '/api/counselor/team' && req.method === 'POST') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (!session || session.expiresAt <= Date.now() || session.role !== 'SUPER_ADMIN') {
        res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'إضافة الأعضاء مقتصرة حصرياً على المشرف العام أ. مازن السبايله' }));
        return;
      }

      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const { name, email, role, permissions } = JSON.parse(body || '{}');
          if (!name || !email || !name.trim() || !email.trim()) {
            res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(JSON.stringify({ success: false, message: 'يرجى إدخال اسم العضو والبريد الإلكتروني' }));
            return;
          }

          const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
          if (!emailRegex.test(email.trim())) {
            res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(JSON.stringify({ success: false, message: 'صيغة البريد الإلكتروني غير صالحة' }));
            return;
          }

          const cleanEmail = email.trim().toLowerCase();
          if (serverTeamMembers.some(m => m.email.toLowerCase() === cleanEmail)) {
            res.writeHead(409, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(JSON.stringify({ success: false, message: 'هذا البريد الإلكتروني مسجل مسبقاً لعضو آخر' }));
            return;
          }

          const newMember = {
            id: Date.now(),
            name: name.trim(),
            email: cleanEmail,
            role: role ? role.trim() : 'عضو فريق - متابع استجابات',
            permissions: permissions ? permissions.trim() : 'اطلاع على بيانات واستجابات النبضات فقط (محظور من الرسائل السرية)',
            isOwner: false,
            addedAt: new Date().toISOString()
          };

          serverTeamMembers.push(newMember);
          saveTeamMembers();
          logger.info('Team member added by Super Admin', { email: cleanEmail, name: newMember.name, ip: clientIp });

          res.writeHead(201, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({
            success: true,
            member: newMember,
            message: `تم إضافة ${newMember.name} (${cleanEmail}) إلى فريق العمل بنجاح`
          }));
        } catch (e) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ success: false, message: 'بيانات غير صالحة' }));
        }
      });
      return;
    }

    if (pathname.startsWith('/api/counselor/team/') && req.method === 'DELETE') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (!session || session.expiresAt <= Date.now() || session.role !== 'SUPER_ADMIN') {
        res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'حذف الأعضاء مقتصر حصرياً على المشرف العام أ. مازن السبايله' }));
        return;
      }

      const idParam = pathname.split('/').pop();
      const memberId = parseInt(idParam, 10);
      const targetMember = serverTeamMembers.find(m => m.id === memberId);

      if (!targetMember) {
        res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'العضو غير موجود' }));
        return;
      }

      if (targetMember.isOwner || targetMember.email.toLowerCase() === ADMIN_EMAIL.toLowerCase()) {
        res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'لا يمكن حذف حساب المشرف العام والمالك الأساسي' }));
        return;
      }

      serverTeamMembers = serverTeamMembers.filter(m => m.id !== memberId);
      saveTeamMembers();
      logger.info('Team member deleted by Super Admin', { memberId, email: targetMember.email, ip: clientIp });

      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({
        success: true,
        message: `تم حذف العضو ${targetMember.name} (${targetMember.email}) بنجاح وإلغاء صلاحياته.`
      }));
      return;
    }

    // 6. Confidential Student Support Tickets Endpoints
    // Public: Student submits a confidential message to the counselor
    if (pathname === '/api/tickets' && req.method === 'POST') {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const { message, contact, category } = JSON.parse(body || '{}');
          if (!message || !message.trim()) {
            res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(JSON.stringify({ success: false, message: 'يرجى كتابة رسالتك أولاً' }));
            return;
          }

          const ticketCode = 'TKT-' + Math.floor(1000 + Math.random() * 9000);
          const newTicket = {
            id: Date.now(),
            code: ticketCode,
            category: category ? category.trim() : 'استشارة عامة',
            message: message.trim(),
            contact: contact ? contact.trim() : 'طالب مجهول الهوية 100%',
            status: 'NEW',
            timestamp: Date.now(),
            counselorReply: null,
            replyTimestamp: null
          };

          serverTickets.unshift(newTicket);
          saveTickets();
          logger.info('Confidential student ticket received', { code: ticketCode, ip: clientIp });

          res.writeHead(201, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({
            success: true,
            ticket: newTicket,
            message: `تم إرسال رسالتك بسرية تامة للمرشد الطلابي. كود المتابعة: ${ticketCode}`
          }));
        } catch (e) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ success: false, message: 'بيانات الطلب غير صالحة' }));
        }
      });
      return;
    }

    // Public: Student tracks their ticket status and counselor reply by secret code
    if (pathname === '/api/tickets/track' && req.method === 'GET') {
      const codeQuery = (parsedUrl.searchParams.get('code') || '').trim().toUpperCase();
      if (!codeQuery) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'يرجى إدخال كود المتابعة' }));
        return;
      }

      const ticket = serverTickets.find(t => t.code.toUpperCase() === codeQuery);
      if (!ticket) {
        res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'لم يتم العثور على تذكرة بهذا الكود' }));
        return;
      }

      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({
        success: true,
        ticket: {
          code: ticket.code,
          category: ticket.category,
          status: ticket.status,
          timestamp: ticket.timestamp,
          hasReply: !!ticket.counselorReply,
          counselorReply: ticket.counselorReply || null,
          replyTimestamp: ticket.replyTimestamp || null
        }
      }));
      return;
    }

    // Protected: Counselor retrieves confidential tickets (Restricted to Super Admin Mazen Al-Sabaileh)
    if (pathname === '/api/counselor/tickets' && req.method === 'GET') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (!session || session.expiresAt <= Date.now() || session.role !== 'SUPER_ADMIN') {
        res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'الوصول للرسائل السرية مقتصر حصرياً على المشرف العام أ. مازن السبايله' }));
        return;
      }

      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({
        success: true,
        tickets: serverTickets
      }));
      return;
    }

    // Protected: Counselor replies to a ticket
    if (pathname.startsWith('/api/counselor/tickets/') && pathname.endsWith('/reply') && req.method === 'POST') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (!session || session.expiresAt <= Date.now() || session.role !== 'SUPER_ADMIN') {
        res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'الرد على الرسائل مقتصر حصرياً على المشرف العام أ. مازن السبايله' }));
        return;
      }

      const parts = pathname.split('/');
      const ticketId = parseInt(parts[parts.length - 2], 10);
      const ticket = serverTickets.find(t => t.id === ticketId);

      if (!ticket) {
        res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'التذكرة غير موجودة' }));
        return;
      }

      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const { reply, status } = JSON.parse(body || '{}');
          if (!reply || !reply.trim()) {
            res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(JSON.stringify({ success: false, message: 'يرجى كتابة نص الرد الإرشادي' }));
            return;
          }

          ticket.counselorReply = reply.trim();
          ticket.replyTimestamp = Date.now();
          ticket.status = status || 'RESOLVED';
          saveTickets();
          logger.info('Counselor replied to ticket', { ticketId, code: ticket.code, ip: clientIp });

          res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({
            success: true,
            ticket,
            message: 'تم إرسال رد وتوجيه المرشد للطالب بنجاح'
          }));
        } catch (e) {
          res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ success: false, message: 'خطأ في معالجة طلب الرد' }));
        }
      });
      return;
    }

    // Protected: Update ticket status
    if (pathname.startsWith('/api/counselor/tickets/') && req.method === 'PATCH') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (!session || session.expiresAt <= Date.now() || session.role !== 'SUPER_ADMIN') {
        res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'تحديث التذاكر مقتصر حصرياً على المشرف العام أ. مازن السبايله' }));
        return;
      }

      const idParam = pathname.split('/').pop();
      const ticketId = parseInt(idParam, 10);
      const ticket = serverTickets.find(t => t.id === ticketId);

      if (!ticket) {
        res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'التذكرة غير موجودة' }));
        return;
      }

      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const { status, counselorReply } = JSON.parse(body || '{}');
          if (status) {
            ticket.status = status;
          }
          if (counselorReply !== undefined) {
            ticket.counselorReply = counselorReply ? counselorReply.trim() : null;
            if (counselorReply) ticket.replyTimestamp = Date.now();
          }
          saveTickets();
          res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ success: true, ticket }));
        } catch (_) {
          ticket.status = ticket.status === 'NEW' ? 'IN_PROGRESS' : (ticket.status === 'IN_PROGRESS' ? 'RESOLVED' : 'NEW');
          saveTickets();
          res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end(JSON.stringify({ success: true, ticket }));
        }
      });
      return;
    }

    // Protected: Delete ticket
    if (pathname.startsWith('/api/counselor/tickets/') && req.method === 'DELETE') {
      const authHeader = req.headers.authorization || '';
      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const session = activeSessions.get(token);

      if (!session || session.expiresAt <= Date.now() || session.role !== 'SUPER_ADMIN') {
        res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, message: 'حذف التذاكر مقتصر حصرياً على المشرف العام أ. مازن السبايله' }));
        return;
      }

      const idParam = pathname.split('/').pop();
      const ticketId = parseInt(idParam, 10);
      serverTickets = serverTickets.filter(t => t.id !== ticketId);
      saveTickets();

      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: true, message: 'تم حذف التذكرة بنجاح' }));
      return;
    }

    // 7. Static File Server with Strict Path Traversal Protection
    const rawUrl = req.url || '';
    if (rawUrl.includes('..') || rawUrl.includes('%2e%2e') || rawUrl.includes('%2E%2E')) {
      logger.warn('Directory traversal pattern blocked', { rawUrl, ip: clientIp });
      res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('403 Forbidden: الوصول محظور');
      return;
    }

    let requestedPath = pathname === '/' ? '/index.html' : pathname;
    const safeResolvedPath = path.resolve(WEB_DIR, '.' + requestedPath);

    // Assert that the canonical path strictly stays inside WEB_DIR
    if (!safeResolvedPath.startsWith(WEB_DIR)) {
      logger.warn('Path traversal blocked outside WEB_DIR', { pathname, ip: clientIp });
      res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('403 Forbidden: الوصول محظور');
      return;
    }

    let filePath;
    if (fs.existsSync(safeResolvedPath) && fs.statSync(safeResolvedPath).isFile()) {
      filePath = safeResolvedPath;
    } else {
      // If a specific file asset with an extension was requested and does not exist, return 404
      const ext = path.extname(requestedPath);
      if (ext && ext !== '.html') {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('404 Not Found: الملف غير موجود');
        return;
      }
      filePath = HTML_FILE;
    }

    const mimeTypes = {
      '.html': 'text/html; charset=utf-8',
      '.css': 'text/css; charset=utf-8',
      '.js': 'application/javascript; charset=utf-8',
      '.json': 'application/json; charset=utf-8',
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.svg': 'image/svg+xml'
    };
    const ext = path.extname(filePath).toLowerCase();
    const contentType = mimeTypes[ext] || 'application/octet-stream';

    fs.readFile(filePath, (err, data) => {
      if (err) {
        logger.error('Error reading static file', { filePath, error: err.message });
        res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('خطأ في معالجة الطلب');
      } else {
        res.writeHead(200, {
          'Content-Type': contentType,
          'Cache-Control': filePath.endsWith('index.html') ? 'no-cache, must-revalidate' : 'public, max-age=86400'
        });
        res.end(data);
      }
    });

  } catch (err) {
    // Log stack trace server-side ONLY; never disclose to the user
    logger.error('Uncaught server handler error', { error: err.message, stack: err.stack });
    res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('حدث خطأ داخلي في الخادم');
  }
});

server.keepAliveTimeout = 65000;
server.headersTimeout = 66000;

server.listen(PORT, '0.0.0.0', () => {
  logger.info(`Tomanina Server running on port ${PORT}`, { port: PORT, env: process.env.NODE_ENV || 'production' });
});
