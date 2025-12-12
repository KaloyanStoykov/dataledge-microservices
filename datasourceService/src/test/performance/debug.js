import http from 'k6/http';

export const options = {
    vus: 1,
    iterations: 1,
};

const BASE_URL = 'http://localhost:8080/datasources';

export default function () {
    // 1. FILL THIS IN (From your Screenshot)
    // Copy the entire "Value" string from your browser console
    // It starts with "eyJhb..."
    const MY_ACCESS_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIyMSIsInN1YiI6ImVtYWlsQGVtYWlsLmNvbSIsImlhdCI6MTc2NTEzNzcxMiwiZXhwIjoxNzY1MTQxMzEyfQ.USROzFNfSc9ml9BBUltNT5T_QnTKu1PEv3ixoKQ2u8I';

    // 2. FILL THIS IN (The User ID)
    // Since your frontend sends 'X-DataLedge-User-ID', k6 must send it too.
    // This is usually the UUID or Integer ID of the logged-in user.
    const MY_USER_ID = '21';

    const params = {
        headers: {
            'Content-Type': 'application/json',

            // THE COOKIE: We format it exactly as the browser does
            // Name=Value
            'Cookie': `accessToken=${MY_ACCESS_TOKEN}`,

            // THE HEADER: We mimic your frontend's "automatic" behavior
            'X-DataLedge-User-ID': MY_USER_ID,
        },
    };

    const payload = JSON.stringify({
        name: `Test DB - ${Math.floor(Math.random() * 10000)}`,
        typeId: 1,
        description: '',
        type: 'POSTGRES',
        url: 'jdbc:postgresql://localhost:5432/mydb',
        created: Date.now(),
        updated: Date.now()
    });

    console.log(`Sending POST to ${BASE_URL}...`);

    const res = http.post(BASE_URL, payload, params);

    // --- DEBUGGING OUTPUT ---
    if (res.status !== 201) {
        console.log('❌ FAILED STATUS:', res.status);
        console.log('❌ RESPONSE BODY:', res.body);
    } else {
        console.log('✅ SUCCESS! Created ID:', res.json('id'));
        console.log('✅ (You can now move to the full load test)');
    }
}