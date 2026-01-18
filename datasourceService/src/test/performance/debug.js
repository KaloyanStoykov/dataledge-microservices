import http from 'k6/http';

export const options = {
    vus: 1,
    iterations: 1,
};

const BASE_URL = 'http://192.168.49.2:30080/datasources';

export default function () {

    const MY_ACCESS_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI5Iiwic3ViIjoiZW1haWxAZW1haWwuY29tIiwiaWF0IjoxNzY4MzY5NTA1LCJleHAiOjE3NjgzNzMxMDV9.Vv6k_GSWEzAiUlP_RcfgUeGZ9-oAyirwpxRSYZNyA9E';

    const MY_USER_ID = '9';

    const params = {
        headers: {
            'Content-Type': 'application/json',


            'Cookie': `accessToken=${MY_ACCESS_TOKEN}`,

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