document.addEventListener('DOMContentLoaded', function () {
    var methodInput = document.getElementById('method');
    var step1 = document.getElementById('step1');
    var step2 = document.getElementById('step2');
    var dot1 = document.getElementById('dot1');
    var dot2 = document.getElementById('dot2');
    var errorDiv = document.getElementById('error');
    var form = document.getElementById('paymentForm');

    // Method card selection
    document.querySelectorAll('.method-card').forEach(function (card) {
        card.addEventListener('click', function () {
            var method = this.dataset.method;
            methodInput.value = method;
            goStep2(method);
        });
    });

    // Navigate to step 2
    window.goStep2 = function (method) {
        step1.classList.add('hidden');
        step2.classList.remove('hidden');
        dot1.classList.remove('active');
        dot1.classList.add('done');
        dot2.classList.add('active');

        var title = document.getElementById('step2Title');
        var desc = document.getElementById('step2Desc');
        var submitBtn = document.getElementById('submitBtn');
        var invoiceFields = document.getElementById('invoiceFields');
        var cryptoFields = document.getElementById('cryptoFields');
        var amountLabel = document.getElementById('amountLabel');

        invoiceFields.classList.add('hidden');
        cryptoFields.classList.add('hidden');
        amountLabel.textContent = 'Сумма (RUB)';

        if (method === 'INVOICE') {
            title.textContent = 'Счёт на оплату';
            desc.textContent = 'Счёт будет отправлен на email через Т-Банк';
            invoiceFields.classList.remove('hidden');
            submitBtn.textContent = 'Выставить счёт';
            submitBtn.className = 'btn btn-yellow';
        } else if (method === 'SBP_QR') {
            title.textContent = 'Оплата через СБП';
            desc.textContent = 'QR-код для мгновенной оплаты через банковское приложение';
            submitBtn.textContent = 'Создать QR-код';
            submitBtn.className = 'btn btn-yellow';
        } else if (method === 'CRYPTO') {
            title.textContent = 'Оплата криптовалютой';
            desc.textContent = 'Прямой перевод на кошелёк с автоматическим отслеживанием';
            cryptoFields.classList.remove('hidden');
            submitBtn.textContent = 'Получить адрес';
            submitBtn.className = 'btn';
            updateCryptoLabel();
        }
    };

    // Back to step 1
    window.goStep1 = function () {
        step2.classList.add('hidden');
        step1.classList.remove('hidden');
        dot2.classList.remove('active');
        dot1.classList.remove('done');
        dot1.classList.add('active');
        errorDiv.classList.add('hidden');
    };

    // Crypto label update
    var cryptoNetworkSelect = document.getElementById('cryptoNetwork');
    if (cryptoNetworkSelect) {
        cryptoNetworkSelect.addEventListener('change', updateCryptoLabel);
    }

    function updateCryptoLabel() {
        var sel = document.getElementById('cryptoNetwork');
        var label = document.getElementById('amountLabel');
        if (sel && sel.selectedOptions.length > 0) {
            label.textContent = 'Сумма (' + sel.selectedOptions[0].textContent + ')';
        }
    }

    // Submit
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        errorDiv.classList.add('hidden');

        var method = methodInput.value;
        var body = {
            amount: parseFloat(document.getElementById('amount').value),
            method: method,
            description: document.getElementById('description').value || null,
            orderId: document.getElementById('orderId').value || null
        };

        if (method === 'INVOICE') {
            var payerName = document.getElementById('payerName').value;
            var payerInn = document.getElementById('payerInn').value;
            var contactEmail = document.getElementById('contactEmail').value;
            if (payerName && payerInn) {
                body.payer = {name: payerName, inn: payerInn};
            }
            if (contactEmail) {
                body.contactEmails = [contactEmail];
            }
        }

        if (method === 'CRYPTO') {
            var networkSel = document.getElementById('cryptoNetwork');
            if (networkSel) body.cryptoNetwork = networkSel.value;
        }

        fetch('/pay', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body)
        })
        .then(function (res) {
            if (!res.ok) return res.json().then(function (err) { throw new Error(err.message || 'Error'); });
            return res.json();
        })
        .then(function (data) {
            window.location.href = '/pay/' + data.id;
        })
        .catch(function (err) {
            errorDiv.textContent = err.message;
            errorDiv.classList.remove('hidden');
        });
    });
});
