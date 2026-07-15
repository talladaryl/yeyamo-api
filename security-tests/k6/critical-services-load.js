import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
assertLocalTarget(baseUrl);
const paths = (__ENV.READ_PATHS || '/actuator/health,/api/v1/catalog/destinations')
  .split(',').map((value) => value.trim()).filter(Boolean);

export const options = {
  discardResponseBodies: true,
  scenarios: {
    progressive_and_spike: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 25 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 300 },
        { duration: '20s', target: 600 },
        { duration: '10s', target: 1000 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750', 'p(99)<1500'],
  },
};

export default function () {
  const headers = { 'X-Correlation-Id': `security-load-${__VU}-${__ITER}` };
  if (__ENV.ACCESS_TOKEN) headers.Authorization = `Bearer ${__ENV.ACCESS_TOKEN}`;
  const responses = http.batch(paths.map((path) => ({
    method: 'GET',
    url: `${baseUrl}${path}`,
    params: { headers, redirects: 0, tags: { path } },
  })));

  responses.forEach((response) => check(response, {
    'no server error': (r) => r.status < 500,
    'expected API status': (r) => [200, 204, 401, 403, 404, 429].includes(r.status),
  }));
}

function assertLocalTarget(value) {
  const target = new URL(value);
  const allowed = ['localhost', '127.0.0.1', 'host.docker.internal'];
  if (!allowed.includes(target.hostname)) {
    throw new Error(`Refusing non-local load target: ${target.hostname}`);
  }
}
