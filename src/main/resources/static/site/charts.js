document.addEventListener('click', (event) => {
    const link = event.target.closest('.show-chart-in-new-tab');
    if (!link) {
        return;
    }

    const form = link.closest('form');
    if (!form) {
        return;
    }

    const chartUrl = new URL(link.href, window.location.href);
    const parameterNames = ['width', 'height', 'filterList', 'type', 'category', 'layout', 'selfIncluded'];
    parameterNames.forEach((name) => chartUrl.searchParams.delete(name));

    for (const [name, value] of new FormData(form).entries()) {
        if (name !== 'chart' && typeof value === 'string' && value !== '') {
            chartUrl.searchParams.append(name, value);
        }
    }

    link.href = chartUrl.toString();
});
