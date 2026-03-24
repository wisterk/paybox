/* ============================================
 *  party-suggest.js
 *  Автоподсказки по наименованию и ИНН контрагента
 * ============================================ */

// --- Вспомогательные функции ---

function escHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function debounce(fn, ms) {
    let timer = null;

    return function debouncedFn() {
        clearTimeout(timer);
        const args = arguments;
        timer = setTimeout(function fireDebounced() {
            fn.apply(null, args);
        }, ms);
    };
}

async function fetchSuggestions(query, container, nameInput, innInput, nameSugg, innSugg) {
    try {
        const res = await fetch('/api/v1/suggest/party', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query: query, count: 5 })
        });

        const data = await res.json();

        if (!data.suggestions || data.suggestions.length === 0) {
            container.classList.add('hidden');
            return;
        }

        container.innerHTML = '';

        data.suggestions.forEach(function renderSuggestion(s) {
            const item = document.createElement('div');
            item.className = 'suggestion-item';

            const name = s.data.name ? (s.data.name.short_with_opf || s.value) : s.value;
            const inn = s.data.inn || '';

            item.innerHTML = '<span class="sugg-name">' + escHtml(name) + '</span>' +
                (inn ? '<span class="sugg-inn">ИНН ' + inn + '</span>' : '');

            item.addEventListener('click', function onSuggestionClick() {
                nameInput.value = name;
                innInput.value = s.data.inn || '';
                container.classList.add('hidden');
                nameSugg.classList.add('hidden');
                innSugg.classList.add('hidden');
            });

            container.appendChild(item);
        });

        container.classList.remove('hidden');
    } catch (err) {
        container.classList.add('hidden');
    }
}

// --- Обработчики событий ---

function handleNameInput(nameInput, nameSugg, innInput, innSugg) {
    const q = nameInput.value.trim();
    if (q.length < 2) {
        nameSugg.classList.add('hidden');
        return;
    }
    fetchSuggestions(q, nameSugg, nameInput, innInput, nameSugg, innSugg);
}

function handleInnInput(innInput, innSugg, nameInput, nameSugg) {
    const q = innInput.value.trim();
    if (q.length < 3) {
        innSugg.classList.add('hidden');
        return;
    }
    fetchSuggestions(q, innSugg, nameInput, innInput, nameSugg, innSugg);
}

function handleOutsideClick(e, nameInput, nameSugg, innInput, innSugg) {
    if (!nameInput.contains(e.target) && !nameSugg.contains(e.target)) {
        nameSugg.classList.add('hidden');
    }
    if (!innInput.contains(e.target) && !innSugg.contains(e.target)) {
        innSugg.classList.add('hidden');
    }
}

// --- Инициализация ---

function init() {
    const nameInput = document.getElementById('payerName');
    const innInput = document.getElementById('payerInn');
    const nameSugg = document.getElementById('payerSuggestions');
    const innSugg = document.getElementById('innSuggestions');

    if (!nameInput || !nameSugg) return;

    nameInput.addEventListener('input', debounce(function onNameInputDebounced() {
        handleNameInput(nameInput, nameSugg, innInput, innSugg);
    }, 300));

    innInput.addEventListener('input', debounce(function onInnInputDebounced() {
        handleInnInput(innInput, innSugg, nameInput, nameSugg);
    }, 300));

    // Скрытие подсказок при клике вне поля
    document.addEventListener('click', function onDocumentClick(e) {
        handleOutsideClick(e, nameInput, nameSugg, innInput, innSugg);
    });
}

document.addEventListener('DOMContentLoaded', init);
