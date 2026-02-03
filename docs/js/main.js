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
            copyBtn.innerHTML = '📋 Copy';
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
                    copyBtn.innerHTML = '✓ Copied!';
                    copyBtn.style.background = 'var(--success)';
                    setTimeout(() => {
                        copyBtn.innerHTML = '📋 Copy';
                        copyBtn.style.background = 'var(--accent)';
                    }, 2000);
                } catch (err) {
                    copyBtn.innerHTML = '❌ Failed';
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

    // Starfield Animation
    function initStarfield() {
        const hero = document.querySelector('.hero');
        if (!hero) return;

        const canvas = document.createElement('canvas');
        canvas.className = 'starfield';
        canvas.style.cssText = `
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            pointer-events: none;
            z-index: 0;
        `;
        hero.style.position = 'relative';
        hero.insertBefore(canvas, hero.firstChild);

        const ctx = canvas.getContext('2d');
        let stars = [];
        const numStars = 150;

        function resize() {
            canvas.width = hero.offsetWidth;
            canvas.height = hero.offsetHeight;
            initStars();
        }

        function initStars() {
            stars = [];
            for (let i = 0; i < numStars; i++) {
                stars.push({
                    x: Math.random() * canvas.width,
                    y: Math.random() * canvas.height,
                    radius: Math.random() * 1.5 + 0.5,
                    alpha: Math.random() * 0.8 + 0.2,
                    speed: Math.random() * 0.3 + 0.1,
                    twinkle: Math.random() * 0.02
                });
            }
        }

        function animate() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            stars.forEach(star => {
                // Twinkle effect
                star.alpha += star.twinkle;
                if (star.alpha >= 1 || star.alpha <= 0.2) {
                    star.twinkle = -star.twinkle;
                }

                // Slow upward drift
                star.y -= star.speed;
                if (star.y < 0) {
                    star.y = canvas.height;
                    star.x = Math.random() * canvas.width;
                }

                // Draw star with glow
                ctx.beginPath();
                ctx.arc(star.x, star.y, star.radius, 0, Math.PI * 2);
                ctx.fillStyle = `rgba(255, 255, 255, ${star.alpha})`;
                ctx.shadowBlur = star.radius * 2;
                ctx.shadowColor = '#00d2ff';
                ctx.fill();
            });

            requestAnimationFrame(animate);
        }

        window.addEventListener('resize', resize);
        resize();
        animate();
    }

    // Initialize on DOM ready
    document.addEventListener('DOMContentLoaded', () => {
        highlightActiveNav();
        initSmoothScroll();
        initCodeCopy();
        initScrollSpy();
        initMobileMenu();
        initScrollAnimations();
        initStarfield();

        // Initialize Mermaid with dark theme
        if (typeof mermaid !== 'undefined') {
            mermaid.initialize({
                startOnLoad: true,
                theme: 'dark',
                themeVariables: {
                    primaryColor: '#1a4d1a',
                    primaryTextColor: '#e2e8f0',
                    primaryBorderColor: '#00d2ff',
                    lineColor: '#00d2ff',
                    secondaryColor: '#1c1e2e',
                    tertiaryColor: '#0b0c15',
                    background: '#0b0c15',
                    mainBkg: '#1c1e2e',
                    nodeBorder: '#00d2ff',
                    clusterBkg: '#1c1e2e',
                    clusterBorder: '#00d2ff',
                    titleColor: '#e2e8f0',
                    edgeLabelBackground: '#1c1e2e'
                },
                flowchart: {
                    useMaxWidth: true,
                    htmlLabels: true,
                    curve: 'basis'
                }
            });
        }
    });
})();

