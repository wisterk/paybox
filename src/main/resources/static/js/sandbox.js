/* ============================================
 *  sandbox.js
 *  Песочница API: создание тестового платежа
 * ============================================ */

// --- Вспомогательные функции ---

function syntaxHighlight(json) {
    return json.replace(
        /("(\\u[\da-fA-F]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
        function classifyToken(match) {
            let cls = 'json-number';

            if (/^"/.test(match)) {
                if (/:$/.test(match)) {
                    cls = 'json-key';
                } else {
                    cls = 'json-string';
                }
            } else if (/true|false/.test(match)) {
                cls = 'json-bool';
            } else if (/null/.test(match)) {
                cls = 'json-null';
            }

            return '<span class="' + cls + '">' + match + '</span>';
        }
    );
}

// --- Обработчики событий ---

async function createTestPayment() {
    const amount = parseFloat(document.getElementById('testAmount').value);
    const desc = document.getElementById('testDesc').value;
    const orderId = document.getElementById('testOrder').value;
    const responseBox = document.getElementById('responseBox');
    const payLink = document.getElementById('payLink');
    const redirectUrl = document.getElementById('testRedirect').value;

    const body = {
        amount: amount,
        currency: 'RUB',
        description: desc || null,
        orderId: orderId || null,
        redirectUrl: redirectUrl || null
    };

    try {
        const res = await fetch('/api/v1/payments', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-API-Key': apiKey
            },
            body: JSON.stringify(body)
        });

        const data = await res.json();
        responseBox.style.display = 'block';
        responseBox.innerHTML = syntaxHighlight(JSON.stringify(data, null, 2));

        if (data.paymentUrl) {
            payLink.href = data.paymentUrl;
            payLink.style.display = 'block';
            payLink.textContent = 'Открыть страницу оплаты';
        }
    } catch (err) {
        responseBox.style.display = 'block';
        responseBox.innerHTML = '<span class="json-error">Error: ' + err.message + '</span>';
    }
}
