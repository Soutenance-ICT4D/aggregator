// swagger-custom.js
(function () {
    function applyTitle() {
        document.title = "Sharepay API Documentation";
    }

    applyTitle();

    const observer = new MutationObserver(function () {
        if (document.title !== "Sharepay API Documentation") {
            applyTitle();
        }
    });

    observer.observe(document.querySelector("title") || document.head, {
        childList: true,
        subtree: true,
        characterData: true
    });
})();

/**
 * Efface les credentials Swagger UI du localStorage puis redirige vers la page de déconnexion.
 * Appelé par le bouton "Déconnexion" de la page de documentation externe.
 */
function swaggerLogout(logoutUrl) {
    if (!confirm('Voulez-vous vraiment vous déconnecter?')) return false;
    localStorage.removeItem('authorized');
    window.location.href = logoutUrl;
    return false;
}
