import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    admin_reads: {
      executor: "constant-arrival-rate",
      rate: Number(__ENV.RATE || 10),
      timeUnit: "1s",
      duration: __ENV.DURATION || "2m",
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<750"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const token = __ENV.ADMIN_TOKEN;
const endpoints = [
  "/api/v1/admin/platform-users?page=0&size=25&sort=createdAt,desc",
  "/api/v1/booking-management/bookings?page=0&size=25&sort=createdAt,desc",
  "/api/v1/payments/admin?page=0&size=25&sort=createdAt,desc",
  "/api/v1/analytics/admin/dashboard",
  "/api/v1/moderation/reports?page=0&size=25&sort=createdAt,desc",
];

export function setup() {
  if (!token) {
    throw new Error("ADMIN_TOKEN is required");
  }
}

export default function () {
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const response = http.get(`${baseUrl}${endpoint}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      "X-Correlation-Id": `k6-${__VU}-${__ITER}`,
    },
  });

  check(response, {
    "admin endpoint succeeds": (result) => result.status >= 200 && result.status < 300,
    "correlation id returned": (result) => Boolean(result.headers["X-Correlation-Id"]),
  });
  sleep(0.1);
}
