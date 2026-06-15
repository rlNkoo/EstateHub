import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'Test123!';

const photo = open('./1.jpg', 'b');

export const options = {
    vus: 5,
    duration: '1m',

    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<2000'],
    },
};

function jsonHeaders(token = null) {
    const headers = {
        'Content-Type': 'application/json',
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    return { headers };
}

export default function () {
    const email = `user_${__VU}_${__ITER}_${Date.now()}@estatehub.test`;

    // 1. Rejestracja
    const registerRes = http.post(
        `${BASE_URL}/auth/register`,
        JSON.stringify({
            email: email,
            password: PASSWORD,
        }),
        jsonHeaders()
    );

    check(registerRes, {
        'register status 201': (r) => r.status === 201,
    });

    // 2. Testowa aktywacja konta
    const activateRes = http.post(
        `${BASE_URL}/auth/test/activate`,
        JSON.stringify({
            email: email,
        }),
        jsonHeaders()
    );

    check(activateRes, {
        'activate status 200/204': (r) => r.status === 200 || r.status === 204,
    });

    // 3. Logowanie
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({
            email: email,
            password: PASSWORD,
        }),
        jsonHeaders()
    );

    check(loginRes, {
        'login status 200': (r) => r.status === 200,
        'login has accessToken': (r) => !!r.json('accessToken'),
    });

    const accessToken = loginRes.json('accessToken');

    if (!accessToken) {
        return;
    }

    // 4. Utworzenie draftu ogłoszenia
    const createListingRes = http.post(
        `${BASE_URL}/listings`,
        null,
        jsonHeaders(accessToken)
    );

    check(createListingRes, {
        'create listing status 201': (r) => r.status === 201,
        'create listing has id': (r) => !!r.json('id'),
    });

    const listingId = createListingRes.json('id');

    if (!listingId) {
        return;
    }

    // 5. Update ogłoszenia
    const updatePayload = {
        title: 'Mieszkanie 2 pokoje w centrum',
        description: 'Jasne mieszkanie po remoncie, blisko metra.',
        priceAmount: 850000,
        currencyCode: 'PLN',
        address: {
            country: 'PL',
            city: 'Warszawa',
            street: 'Marszałkowska 101',
            postalCode: '00-001',
        },
        area: 48.5,
        rooms: 3,
        floor: 3,
        propertyType: 'APARTMENT',
        photoIds: [],
    };

    const updateRes = http.put(
        `${BASE_URL}/listings/${listingId}`,
        JSON.stringify(updatePayload),
        jsonHeaders(accessToken)
    );

    check(updateRes, {
        'update listing status 200': (r) => r.status === 200,
        'update listing status DRAFT': (r) => r.json('status') === 'DRAFT',
    });

    // 6. Publikacja ogłoszenia
    const publishRes = http.post(
        `${BASE_URL}/listings/${listingId}/publish`,
        null,
        jsonHeaders(accessToken)
    );

    check(publishRes, {
        'publish listing status 200': (r) => r.status === 200,
        'publish listing status PUBLISHED': (r) => r.json('status') === 'PUBLISHED',
    });

    // krótka pauza na event Kafka -> SearchService
    sleep(1);

    // 7. Upload zdjęcia
    const uploadData = {
        file: http.file(photo, '1.jpg', 'image/jpeg'),
    };

    const uploadRes = http.post(
        `${BASE_URL}/media/listings/${listingId}/photos`,
        uploadData,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
        }
    );

    check(uploadRes, {
        'upload photo status 201': (r) => r.status === 201,
        'upload photo has mediaId': (r) => !!r.json('mediaId'),
    });

    // 8. Wyszukiwanie
    const searchRes = http.get(
        `${BASE_URL}/search/listings?city=Warszawa&propertyType=APARTMENT&page=0&size=10`
    );

    check(searchRes, {
        'search status 200': (r) => r.status === 200,
    });

    // 9. Archiwizacja ogłoszenia
    const archiveRes = http.post(
        `${BASE_URL}/listings/${listingId}/archive`,
        null,
        jsonHeaders(accessToken)
    );

    check(archiveRes, {
        'archive listing status 200': (r) => r.status === 200,
        'archive listing status ARCHIVED': (r) => r.json('status') === 'ARCHIVED',
    });
}