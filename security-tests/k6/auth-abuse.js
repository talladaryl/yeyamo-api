import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
assertLocalTarget(baseUrl);

export const options = {
  scenarios: {
    invalid_login_burst: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.AUTH_RATE || 30),
      timeUnit: '1s',
      duration: __ENV.DURATION || '30s',
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.95'],
  },
};

export default function () {
  const suffix = `${__VU}-${__ITER % 50}`;
  const response = http.post(`${baseUrl}/api/v1/auth/login`, JSON.stringify({
    email: `security-test-${suffix}@invalid.test`,
    password: 'Invalid-only-for-authorized-local-test-1!',
  }), {
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-Id': `security-auth-${__VU}-${__ITER}`,
    },
    redirects: 0,
    tags: { scenario: 'invalid_login' },
  });

  check(response, {
    'invalid credentials or throttling only': (r) => [400, 401, 403, 429].includes(r.status),
    'no server error': (r) => r.status < 500,
    'rate limit exposes retry guidance': (r) => r.status !== 429 || Boolean(r.headers['Retry-After']),
  });
  sleep(0.05);
}

function assertLocalTarget(value) {
  const target = new URL(value);
  const allowed = ['localhost', '127.0.0.1', 'host.docker.internal'];
  if (!allowed.includes(target.hostname)) {
    throw new Error(`Refusing non-local security target: ${target.hostname}`);
  }
}
