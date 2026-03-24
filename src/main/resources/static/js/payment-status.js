/* ============================================
 *  payment-status.js
 *  Страница статуса платежа: QR, WebSocket, копирование
 * ============================================ */

// --- Константы и ссылки на DOM ---
const QR_OPTIONS = {
    width: 160,
    margin: 2,
    color: { dark: '#1a1f2e', light: '#ffffff' },
    errorCorrectionLevel: 'M'
};

// --- Вспомогательные функции ---

function hideQrLoader() {
    const loader = document.getElementById('qrLoader');
    if (loader) loader.style.display = 'none';
}

function renderQr(container, text, opts, fallbackFn) {
    if (!container || typeof QRCode === 'undefined' || !text) {
        hideQrLoader();
        return;
    }

    QRCode.toCanvas(text, opts, function handleQrResult(err, canvas) {
        if (!err && canvas) {
            hideQrLoader();
            container.style.display = 'inline-block';
            container.appendChild(canvas);
        } else if (fallbackFn) {
            fallbackFn();
        }
    });
}

function buildCryptoUri(network, address, amount) {
    if (!amount || amount === '') return address;

    switch (network) {
        case 'BTC':
            return 'bitcoin:' + address + '?amount=' + amount;
        case 'ETH':
            return 'ethereum:' + address + '?value=' + amount;
        case 'TON':
        case 'USDT_TON':
            try {
                return 'ton://transfer/' + address + '?amount=' + toNano(amount, network === 'TON' ? 9 : 6);
            } catch (e) {
                return address;
            }
        case 'USDT_TRC20':
            return address;
        default:
            return address;
    }
}

function toNano(amount, decimals) {
    const parts = String(amount).split('.');
    const whole = parts[0];
    let frac = parts.length > 1 ? parts[1] : '';

    while (frac.length < decimals) frac += '0';
    frac = frac.substring(0, decimals);

    return (BigInt(whole) * BigInt(Math.pow(10, decimals)) + BigInt(frac)).toString();
}

function formatTimestamps() {
    document.querySelectorAll('[data-timestamp]').forEach(function formatSingleTimestamp(el) {
        const iso = el.getAttribute('data-iso');
        if (!iso) return;

        try {
            const date = new Date(iso);
            el.textContent = date.toLocaleString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            // Игнорируем ошибки парсинга даты
        }
    });
}

function showCopyFeedback(selector) {
    const btn = document.querySelector(selector);
    if (!btn) return;

    const original = btn.innerHTML;
    btn.innerHTML = '&#10003;';
    btn.style.color = 'var(--success)';

    setTimeout(function restoreCopyButton() {
        btn.innerHTML = original;
        btn.style.color = '';
    }, 1500);
}

function showShareFeedback(btn) {
    if (!btn) return;

    const original = btn.innerHTML;
    btn.innerHTML = '<span class="share-icon">&#10003;</span> Ссылка скопирована';
    btn.style.borderColor = 'var(--success)';
    btn.style.color = 'var(--success)';

    setTimeout(function restoreShareButton() {
        btn.innerHTML = original;
        btn.style.borderColor = '';
        btn.style.color = '';
    }, 2000);
}

function fallbackCopyToClipboard(text) {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.opacity = '0';
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
}

// --- Обработчики событий ---

async function copyRefCode() {
    const el = document.getElementById('refCode');
    if (!el) return;

    const text = el.textContent;

    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
    } else {
        fallbackCopyToClipboard(text);
    }
    showCopyFeedback('.crypto-pay-copy');
}

async function copyAddress() {
    const addr = document.getElementById('cryptoAddress');
    if (!addr) return;

    const text = addr.textContent;

    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
    } else {
        fallbackCopyToClipboard(text);
    }
    showCopyFeedback('.crypto-pay-copy');
}

async function changeMethod() {
    try {
        await fetch('/pay/' + paymentId + '/reset', { method: 'POST' });
        window.location.reload();
    } catch (err) {
        window.location.reload();
    }
}

async function sharePayment() {
    const url = window.location.href;
    const title = document.title;

    // Мобильные устройства: нативный диалог шаринга
    if (navigator.share) {
        try {
            await navigator.share({ title: title, url: url });
        } catch (e) {
            // Пользователь отменил шаринг
        }
        return;
    }

    // Десктоп: копирование ссылки
    const btn = document.getElementById('shareBtn');

    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(url);
        showShareFeedback(btn);
    } else {
        fallbackCopyToClipboard(url);
        showShareFeedback(btn);
    }
}

// --- Инициализация ---

function init() {
    // Отрисовка QR для СБП
    if (typeof qrData === 'string' && qrData.length > 0 && !isCrypto) {
        const qrContainer = document.getElementById('qrCode');
        if (qrContainer && typeof QRCode !== 'undefined') {
            renderQr(qrContainer, qrData, QR_OPTIONS, null);
        }
    }

    // Отрисовка QR для крипто — показываем контейнер при успехе
    if (isCrypto && typeof cryptoAddress === 'string' && cryptoAddress.length > 0) {
        const cryptoQrContainer = document.getElementById('cryptoQr');
        if (cryptoQrContainer && typeof QRCode !== 'undefined') {
            const qrContent = buildCryptoUri(cryptoNetwork, cryptoAddress, cryptoAmount);
            renderQr(cryptoQrContainer, qrContent, QR_OPTIONS, function fallbackToPlainAddress() {
                renderQr(cryptoQrContainer, cryptoAddress, QR_OPTIONS, null);
            });
        }
    }

    // WebSocket для обновления статуса в реальном времени
    if (!isTerminal) {
        const socket = new SockJS('/ws');
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function onStompConnected() {
            stompClient.subscribe('/topic/payments/' + paymentId, function onPaymentMessage(message) {
                const payment = JSON.parse(message.body);
                updateStatus(payment, stompClient);
            });
        });
    }

    // Форматирование временных меток в локальный часовой пояс
    formatTimestamps();
}

function updateStatus(payment, stompClient) {
    const statusEl = document.getElementById('status');
    statusEl.textContent = payment.status;
    statusEl.className = 'status-badge ' + payment.status;

    const terminalStatuses = ['PAID', 'FAILED', 'EXPIRED', 'CANCELLED'];
    if (terminalStatuses.includes(payment.status)) {
        if (stompClient) stompClient.disconnect();
        // Перезагрузка для обновления всей страницы (скрытие кнопок, время оплаты и т.д.)
        setTimeout(function reloadPage() {
            window.location.reload();
        }, 1000);
    }
}

document.addEventListener('DOMContentLoaded', init);
