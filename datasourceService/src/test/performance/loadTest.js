import http from 'k6/http';
import { check, group, sleep } from 'k6';

// --- 1. CONFIGURATION (The Load Profile) ---
export const options = {
    // We use "stages" to simulate real traffic patterns
    stages: [
        { duration: '2m', target: 1000 },  // Step 1: Moderate Load
        { duration: '1m', target: 1000 },
        { duration: '1m', target: 0 },   // Cooldown
    ],

    // The test is considered "FAILED" if we break these rules
    thresholds: {
        // 95% of requests must finish within 600ms
        http_req_duration: ['p(95)<600'],
        // We want less than 1% error rate
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8080/datasources';

// Paste your working constants here
const MY_ACCESS_TOKEN = 'TOKEN';
const MY_USER_ID = '21';

export default function () {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Cookie': `accessToken=${MY_ACCESS_TOKEN}`,
            'X-DataLedge-User-ID': MY_USER_ID,
        },
    };

    let newDataSourceId;

    // --- STEP 1: CREATE ---
    group('Create DataSource', function () {
        const payload = JSON.stringify({
            name: `LoadTest-DB-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
            typeId: 1,
            description: 'K6 Load Test',
            type: 'POSTGRES',
            url: 'jdbc:postgresql://localhost:5432/mydb',
            created: Date.now(),
            updated: Date.now()
        });

        const res = http.post(BASE_URL, payload, params);

        // 1. Check status FIRST. If this fails, do NOT try to parse JSON.
        const success = check(res, {
            'is created (201)': (r) => r.status === 201,
        });

        // 2. Only read the body if the request succeeded
        if (success) {
            try {
                check(res, {
                    'has id': (r) => r.json('id') !== undefined,
                });
                newDataSourceId = res.json('id');
            } catch (error) {
                console.error('Failed to parse JSON on successful 201 response');
            }
        } else {
            // Optional: Print error for the first few failures to debug
            // console.log(`Request Failed: Status ${res.status}`);
        }
    });

    sleep(1);

    // --- STEP 2: DELETE ---
    // Only run this if we actually got an ID from Step 1
    if (newDataSourceId) {
        group('Delete DataSource', function () {
            const res = http.del(`${BASE_URL}/${newDataSourceId}`, null, params);
            check(res, {
                'is deleted (200)': (r) => r.status === 200,
            });
        });
    }

    sleep(1);
}