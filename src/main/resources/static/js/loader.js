// loader.js — reliable loading screen that always shows and always hides
(function () {
  var ls = null;

  function hide() {
    if (!ls) ls = document.getElementById('loading-screen');
    if (!ls) return;
    ls.classList.add('hidden');
  }

  function tryHide() {
    // Double rAF ensures the browser has painted the actual page
    requestAnimationFrame(function () {
      requestAnimationFrame(function () {
        hide();
      });
    });
  }

  // Run as soon as DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      setTimeout(tryHide, 400);
    });
  } else {
    // DOM already ready (script loaded late / cached page)
    setTimeout(tryHide, 200);
  }

  // Hard fallback — never leave loading screen stuck
  setTimeout(hide, 2500);
})();
