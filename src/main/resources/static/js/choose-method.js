/* ============================================
 *  choose-method.js
 *  Выбор способа оплаты: шаги, валидация, подтверждение
 * ============================================ */

// --- Константы и ссылки на DOM ---
let selectedMethod = null;
let selectedNetwork = null;

// --- Вспомогательные функции ---

function getNetCurrency(net) {
    const map = { BTC: 'BTC', ETH: 'ETH', USDT_TRC20: 'USDT', TON: 'TON', USDT_TON: 'USDT' };
    return map[net] || net;
}

function showError(errorDiv, message) {
    errorDiv.textContent = message;
    errorDiv.classList.remove('hidden');
}

// --- Обработчики событий ---

function handleMethodCardClick(step1, step2, step3, errorDiv) {
    selectedMethod = this.dataset.method;

    if (selectedMethod === 'CRYPTO') {
        step1.classList.add('hidden');
        step2.classList.remove('hidden');
    } else if (selectedMethod === 'INVOICE') {
        step1.classList.add('hidden');
        step3.classList.remove('hidden');
        document.getElementById('step3Title').textContent = 'Данные для счёта';
        document.getElementById('invoiceForm').classList.remove('hidden');
        document.getElementById('confirmBtn').textContent = 'Выставить счёт';
    } else {
        // СБП — подтверждаем сразу
        confirmPayment();
    }
}

function handleNetworkCardClick(errorDiv) {
    if (this.dataset.disabled === 'true') {
        showError(errorDiv, 'Курс для этой сети недоступен. Попробуйте другую.');
        return;
    }

    selectedNetwork = this.dataset.network;
    document.querySelectorAll('#step2 .method-card').forEach(function clearSelection(c) {
        c.classList.remove('selected');
    });
    this.classList.add('selected');
    errorDiv.classList.add('hidden');
    confirmPayment();
}

function backToStep1() {
    document.getElementById('step2').classList.add('hidden');
    document.getElementById('step3').classList.add('hidden');
    document.getElementById('step1').classList.remove('hidden');
    document.getElementById('error').classList.add('hidden');
    selectedMethod = null;
    selectedNetwork = null;
}

async function confirmPayment() {
    const errorDiv = document.getElementById('error');
    errorDiv.classList.add('hidden');

    const body = { method: selectedMethod };
    if (selectedNetwork) body.cryptoNetwork = selectedNetwork;

    // Счёт: валидация и прикрепление данных плательщика
    if (selectedMethod === 'INVOICE') {
        const payerName = document.getElementById('payerName').value.trim();
        const payerInn = document.getElementById('payerInn').value.trim();
        const contactEmail = document.getElementById('contactEmail').value.trim();

        if (!payerName) {
            showError(errorDiv, 'Укажите наименование плательщика');
            return;
        }
        if (!payerInn || !/^\d{10,12}$/.test(payerInn)) {
            showError(errorDiv, 'Укажите корректный ИНН (10 или 12 цифр)');
            return;
        }
        if (!contactEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contactEmail)) {
            showError(errorDiv, 'Укажите корректный email для отправки счёта');
            return;
        }

        body.payer = { name: payerName, inn: payerInn };
        body.contactEmails = [contactEmail];
    }

    try {
        const res = await fetch('/pay/' + paymentId + '/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Error');
        }

        await res.json();
        window.location.reload();
    } catch (err) {
        showError(errorDiv, err.message || 'Ошибка при подтверждении платежа');
    }
}

// --- Инициализация ---

function init() {
    const step1 = document.getElementById('step1');
    const step2 = document.getElementById('step2');
    const step3 = document.getElementById('step3');
    const errorDiv = document.getElementById('error');

    // Отображение курсов для крипто-сетей
    if (rates && paymentCurrency === 'RUB') {
        document.querySelectorAll('.crypto-rate-info').forEach(function displayRate(el) {
            const net = el.dataset.network;
            const rateValue = rates[net];

            if (rateValue && rateValue > 0) {
                const converted = (paymentAmount / rateValue).toFixed(6);
                el.innerHTML = '~' + converted + ' ' + getNetCurrency(net) +
                    ' <span class="rate-detail">(1 = ' +
                    Number(rateValue).toLocaleString('ru') + ' RUB)</span>';
            } else {
                el.textContent = 'Курс недоступен';
            }
        });
    }

    // Клик по карточке метода оплаты
    document.querySelectorAll('#step1 .method-card').forEach(function bindMethodClick(card) {
        card.addEventListener('click', function onMethodClick() {
            handleMethodCardClick.call(this, step1, step2, step3, errorDiv);
        });
    });

    // Пометка недоступных сетей и обработка кликов
    document.querySelectorAll('#step2 .method-card').forEach(function bindNetworkClick(card) {
        const net = card.dataset.network;
        const hasRate = rates && rates[net] && rates[net] > 0;

        if (paymentCurrency === 'RUB' && !hasRate) {
            card.style.opacity = '0.4';
            card.style.cursor = 'not-allowed';
            card.dataset.disabled = 'true';
        }

        card.addEventListener('click', function onNetworkClick() {
            handleNetworkCardClick.call(this, errorDiv);
        });
    });

    // Экспорт функций в глобальную область для вызова из HTML
    window.backToStep1 = backToStep1;
    window.confirmPayment = confirmPayment;
}

document.addEventListener('DOMContentLoaded', init);
