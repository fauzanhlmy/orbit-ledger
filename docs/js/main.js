// Orbit Ledger Documentation - JavaScript
(function () {
    'use strict';

    // Highlight active navigation based on current page
    function highlightActiveNav() {
        const currentPage = window.location.pathname.split('/').pop() || 'index.html';
        const navLinks = document.querySelectorAll('.navbar-nav a');

        navLinks.forEach(link => {
            const href = link.getAttribute('href');
            if (href === currentPage || (currentPage === '' && href === 'index.html')) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    // Smooth scroll for anchor links
    function initSmoothScroll() {
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', function (e) {
                e.preventDefault();
                const target = document.querySelector(this.getAttribute('href'));
                if (target) {
                    target.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            });
        });
    }

    // Add copy button to code blocks
    function initCodeCopy() {
        document.querySelectorAll('.code-block').forEach(block => {
            const header = block.querySelector('.code-header');
            if (!header) return;

            const copyBtn = document.createElement('button');
            copyBtn.className = 'copy-btn';
            copyBtn.innerHTML = 'ðŸ“‹ Copy';
            copyBtn.style.cssText = `
                background: var(--accent);
                border: none;
                color: var(--text);
                padding: 4px 12px;
                border-radius: 4px;
                cursor: pointer;
                font-size: 0.8rem;
                transition: all 0.2s ease;
            `;

            copyBtn.addEventListener('click', async () => {
                const code = block.querySelector('pre').textContent;
                try {
                    await navigator.clipboard.writeText(code);
                    copyBtn.innerHTML = 'âœ“ Copied!';
                    copyBtn.style.background = 'var(--success)';
                    setTimeout(() => {
                        copyBtn.innerHTML = 'ðŸ“‹ Copy';
                        copyBtn.style.background = 'var(--accent)';
                    }, 2000);
                } catch (err) {
                    copyBtn.innerHTML = 'âŒ Failed';
                }
            });

            header.appendChild(copyBtn);
        });
    }

    // Sidebar scroll spy
    function initScrollSpy() {
        const sections = document.querySelectorAll('section[id]');
        const sidebarLinks = document.querySelectorAll('.sidebar-nav a');

        if (sections.length === 0 || sidebarLinks.length === 0) return;

        function updateActiveLink() {
            const scrollPos = window.scrollY + 150;

            sections.forEach(section => {
                const top = section.offsetTop;
                const height = section.offsetHeight;
                const id = section.getAttribute('id');

                if (scrollPos >= top && scrollPos < top + height) {
                    sidebarLinks.forEach(link => {
                        link.classList.remove('active');
                        if (link.getAttribute('href') === '#' + id) {
                            link.classList.add('active');
                        }
                    });
                }
            });
        }

        window.addEventListener('scroll', updateActiveLink);
        updateActiveLink();
    }

    // Mobile menu toggle
    function initMobileMenu() {
        const menuBtn = document.querySelector('.mobile-menu-btn');
        const nav = document.querySelector('.navbar-nav');

        if (!menuBtn || !nav) return;

        menuBtn.addEventListener('click', () => {
            nav.classList.toggle('show');
        });
    }

    // Fade in animation on scroll
    function initScrollAnimations() {
        const observerOptions = {
            root: null,
            rootMargin: '0px',
            threshold: 0.1
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('fade-in');
                    observer.unobserve(entry.target);
                }
            });
        }, observerOptions);

        document.querySelectorAll('.feature-card, .stat-item').forEach(el => {
            el.style.opacity = '0';
            observer.observe(el);
        });
    }

    // Initialize on DOM ready
    document.addEventListener('DOMContentLoaded', () => {
        highlightActiveNav();
        initSmoothScroll();
        initCodeCopy();
        initScrollSpy();
        initMobileMenu();
        initScrollAnimations();
    });
})();

