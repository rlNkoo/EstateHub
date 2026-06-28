import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'Test123!';

const photo = open('./1.jpg', 'b');

const registerDuration = new Trend('step_01_register_duration');
const activateDuration = new Trend('step_02_activate_duration');
const loginDuration = new Trend('step_03_login_duration');
const createListingDuration = new Trend('step_04_create_listing_duration');
const updateListingDuration = new Trend('step_05_update_listing_duration');
const publishListingDuration = new Trend('step_06_publish_listing_duration');
const uploadPhotoDuration = new Trend('step_07_upload_photo_duration');
const deletePhotoDuration = new Trend('step_08_delete_photo_duration');
const searchDuration = new Trend('step_09_search_duration');
const archiveListingDuration = new Trend('step_10_archive_listing_duration');

const flowFailed = new Rate('flow_failed');

export const options = {
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],

    scenarios: {
        estatehub_flow: {
            executor: 'constant-arrival-rate',

            rate: 2,
            timeUnit: '1s',

            duration: '1m',

            preAllocatedVUs: 40,
            maxVUs: 150,
        },
    },

    thresholds: {
        http_req_failed: ['rate<0.05'],
        flow_failed: ['rate<0.05'],
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

function authHeaders(token) {
    return {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    };
}

function addDuration(metric, response) {
    metric.add(response.timings.duration);
}

export default function () {
    const email = `user_${__VU}_${__ITER}_${Date.now()}@estatehub.test`;

    // REQUEST 1: Rejestracja użytkownika
    // POST /auth/register
    const registerRes = http.post(
        `${BASE_URL}/auth/register`,
        JSON.stringify({
            email: email,
            password: PASSWORD,
        }),
        {
            ...jsonHeaders(),
            tags: { endpoint: '01_register' },
        }
    );

    addDuration(registerDuration, registerRes);

    const registerOk = check(registerRes, {
        '01 register status 201': (r) => r.status === 201,
    });

    if (!registerOk) {
        flowFailed.add(1);
        return;
    }

    // REQUEST 2: Testowa aktywacja konta
    // POST /auth/test/activate
    const activateRes = http.post(
        `${BASE_URL}/auth/test/activate`,
        JSON.stringify({
            email: email,
        }),
        {
            ...jsonHeaders(),
            tags: { endpoint: '02_activate' },
        }
    );

    addDuration(activateDuration, activateRes);

    const activateOk = check(activateRes, {
        '02 activate status 200/204': (r) => r.status === 200 || r.status === 204,
    });

    if (!activateOk) {
        flowFailed.add(1);
        return;
    }

    // REQUEST 3: Logowanie użytkownika i pobranie JWT
    // POST /auth/login
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({
            email: email,
            password: PASSWORD,
        }),
        {
            ...jsonHeaders(),
            tags: { endpoint: '03_login' },
        }
    );

    addDuration(loginDuration, loginRes);

    const loginOk = check(loginRes, {
        '03 login status 200': (r) => r.status === 200,
        '03 login has accessToken': (r) => !!r.json('accessToken'),
    });

    if (!loginOk) {
        flowFailed.add(1);
        return;
    }

    const accessToken = loginRes.json('accessToken');

    // REQUEST 4: Utworzenie wersji roboczej ogłoszenia
    // POST /listings
    const createListingRes = http.post(
        `${BASE_URL}/listings`,
        null,
        {
            ...jsonHeaders(accessToken),
            tags: { endpoint: '04_create_listing' },
        }
    );

    addDuration(createListingDuration, createListingRes);

    const createOk = check(createListingRes, {
        '04 create listing status 201': (r) => r.status === 201,
        '04 create listing has id': (r) => !!r.json('id'),
    });

    if (!createOk) {
        flowFailed.add(1);
        return;
    }

    const listingId = createListingRes.json('id');

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

    // REQUEST 5: Aktualizacja danych ogłoszenia
    // PUT /listings/{listingId}
    const updateRes = http.put(
        `${BASE_URL}/listings/${listingId}`,
        JSON.stringify(updatePayload),
        {
            ...jsonHeaders(accessToken),
            tags: { endpoint: '05_update_listing' },
        }
    );

    addDuration(updateListingDuration, updateRes);

    const updateOk = check(updateRes, {
        '05 update listing status 200': (r) => r.status === 200,
        '05 update listing status DRAFT': (r) => r.json('status') === 'DRAFT',
    });

    if (!updateOk) {
        flowFailed.add(1);
        return;
    }

    // REQUEST 6: Publikacja ogłoszenia
    // POST /listings/{listingId}/publish
    const publishRes = http.post(
        `${BASE_URL}/listings/${listingId}/publish`,
        null,
        {
            ...jsonHeaders(accessToken),
            tags: { endpoint: '06_publish_listing' },
        }
    );

    addDuration(publishListingDuration, publishRes);

    const publishOk = check(publishRes, {
        '06 publish listing status 200': (r) => r.status === 200,
        '06 publish listing status PUBLISHED': (r) => r.json('status') === 'PUBLISHED',
    });

    if (!publishOk) {
        flowFailed.add(1);
        return;
    }

    // Krótka pauza techniczna na propagację eventu:
    // ListingService -> Kafka -> SearchService -> Elasticsearch
    sleep(1);

    const shouldUploadPhoto = Math.random() < 0.2;

    if (shouldUploadPhoto) {
        const uploadData = {
            file: http.file(photo, '1.jpg', 'image/jpeg'),
        };

        // REQUEST 7: Upload zdjęcia do ogłoszenia
        // POST /media/listings/{listingId}/photos
        const uploadRes = http.post(
            `${BASE_URL}/media/listings/${listingId}/photos`,
            uploadData,
            {
                ...authHeaders(accessToken),
                tags: { endpoint: '07_upload_photo' },
            }
        );

        addDuration(uploadPhotoDuration, uploadRes);

        const uploadOk = check(uploadRes, {
            '07 upload photo status 201': (r) => r.status === 201,
            '07 upload photo has mediaId': (r) => !!r.json('mediaId'),
        });

        if (!uploadOk) {
            flowFailed.add(1);
            return;
        }

        const mediaId = uploadRes.json('mediaId');

        // REQUEST 8: Usunięcie zdjęcia
        // DELETE /media/photos/{mediaId}
        const deletePhotoRes = http.del(
            `${BASE_URL}/media/photos/${mediaId}`,
            null,
            {
                ...authHeaders(accessToken),
                tags: { endpoint: '08_delete_photo' },
            }
        );

        addDuration(deletePhotoDuration, deletePhotoRes);

        const deletePhotoOk = check(deletePhotoRes, {
            '08 delete photo status 204': (r) => r.status === 204,
        });

        if (!deletePhotoOk) {
            flowFailed.add(1);
            return;
        }
    }

    // REQUEST 9: Wyszukiwanie opublikowanych ogłoszeń
    // GET /search/listings
    const searchRes = http.get(
        `${BASE_URL}/search/listings?city=Warszawa&propertyType=APARTMENT&page=0&size=10`,
        {
            tags: { endpoint: '09_search_listings' },
        }
    );

    addDuration(searchDuration, searchRes);

    const searchOk = check(searchRes, {
        '09 search status 200': (r) => r.status === 200,
    });

    if (!searchOk) {
        flowFailed.add(1);
        return;
    }

    // REQUEST 10: Archiwizacja ogłoszenia
    // POST /listings/{listingId}/archive
    const archiveRes = http.post(
        `${BASE_URL}/listings/${listingId}/archive`,
        null,
        {
            ...jsonHeaders(accessToken),
            tags: { endpoint: '10_archive_listing' },
        }
    );

    addDuration(archiveListingDuration, archiveRes);

    const archiveOk = check(archiveRes, {
        '10 archive listing status 200': (r) => r.status === 200,
        '10 archive listing status ARCHIVED': (r) => r.json('status') === 'ARCHIVED',
    });

    if (!archiveOk) {
        flowFailed.add(1);
        return;
    }

    flowFailed.add(0);
}

function value(data, metric, stat) {
    return data.metrics[metric]?.values?.[stat] ?? 0;
}

function rate(data, metric) {
    return ((data.metrics[metric]?.values?.rate ?? 0) * 100).toFixed(2);
}

function ms(data, metric, stat) {
    return `${value(data, metric, stat).toFixed(2)} ms`;
}

export function handleSummary(data) {
    return {
        stdout: `
================ ESTATEHUB PERFORMANCE SUMMARY ================

STABILITY
http_req_failed: ${rate(data, 'http_req_failed')} %
flow_failed:    ${rate(data, 'flow_failed')} %

HTTP RESPONSE TIMES
avg: ${ms(data, 'http_req_duration', 'avg')}
med: ${ms(data, 'http_req_duration', 'med')}
p90: ${ms(data, 'http_req_duration', 'p(90)')}
p95: ${ms(data, 'http_req_duration', 'p(95)')}
max: ${ms(data, 'http_req_duration', 'max')}

TEST SCALE
iterations: ${value(data, 'iterations', 'count')}
http_reqs:  ${value(data, 'http_reqs', 'count')}
vus_max:    ${value(data, 'vus_max', 'max')}

OPERATION RESPONSE TIMES
01 Register User:      ${ms(data, 'step_01_register_duration', 'avg')}
02 Activate Account:   ${ms(data, 'step_02_activate_duration', 'avg')}
03 Login:              ${ms(data, 'step_03_login_duration', 'avg')}
04 Create Listing:     ${ms(data, 'step_04_create_listing_duration', 'avg')}
05 Update Listing:     ${ms(data, 'step_05_update_listing_duration', 'avg')}
06 Publish Listing:    ${ms(data, 'step_06_publish_listing_duration', 'avg')}
07 Upload Photo:       ${ms(data, 'step_07_upload_photo_duration', 'avg')}
08 Delete Photo:       ${ms(data, 'step_08_delete_photo_duration', 'avg')}
09 Search Listings:    ${ms(data, 'step_09_search_duration', 'avg')}
10 Archive Listing:    ${ms(data, 'step_10_archive_listing_duration', 'avg')}

================================================================
`,
    };
}