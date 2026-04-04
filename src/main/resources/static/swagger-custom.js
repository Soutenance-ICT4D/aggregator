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
