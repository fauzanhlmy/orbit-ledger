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

    // Mobile navbar toggle
    function initMobileNav() {
        const toggle = document.querySelector('.navbar-toggle');
        const menu = document.querySelector('.navbar-menu');
        const overlay = document.querySelector('.navbar-overlay');

        if (!toggle || !menu) return;

        function closeMenu() {
            toggle.classList.remove('active');
            menu.classList.remove('active');
            if (overlay) overlay.classList.remove('active');
            document.body.style.overflow = '';
        }

        function openMenu() {
            toggle.classList.add('active');
            menu.classList.add('active');
            if (overlay) overlay.classList.add('active');
            document.body.style.overflow = 'hidden';
        }

        toggle.addEventListener('click', () => {
            if (menu.classList.contains('active')) {
                closeMenu();
            } else {
                openMenu();
            }
        });

        if (overlay) {
            overlay.addEventListener('click', closeMenu);
        }

        // Close menu when clicking a nav link
        menu.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', closeMenu);
        });

        // Close menu on window resize if open
        window.addEventListener('resize', () => {
            if (window.innerWidth > 768) {
                closeMenu();
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

    // Orbit Arcs Animation
    function initStarfield() {
        const hero = document.querySelector('.hero');
        if (!hero) return;

        const canvas = document.createElement('canvas');
        canvas.className = 'orbit-arcs';
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
        let time = 0;

        function resize() {
            canvas.width = hero.offsetWidth;
            canvas.height = hero.offsetHeight;
        }

        function drawOrbitArc(x, y, radius, startAngle, endAngle, alpha, lineWidth) {
            ctx.beginPath();
            ctx.arc(x, y, radius, startAngle, endAngle);
            ctx.strokeStyle = `rgba(0, 210, 255, ${alpha})`;
            ctx.lineWidth = lineWidth;
            ctx.stroke();
        }

        function drawOrbitDot(centerX, centerY, radius, angle, dotRadius, alpha) {
            const x = centerX + Math.cos(angle) * radius;
            const y = centerY + Math.sin(angle) * radius;

            // Glow effect
            ctx.beginPath();
            ctx.arc(x, y, dotRadius * 2, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(0, 210, 255, ${alpha * 0.3})`;
            ctx.fill();

            // Dot
            ctx.beginPath();
            ctx.arc(x, y, dotRadius, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(0, 210, 255, ${alpha})`;
            ctx.fill();
        }

        function animate() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            time += 0.008;

            const w = canvas.width;
            const h = canvas.height;

            // Orbit radii
            const radius1 = Math.min(w, h) * 0.4;
            const radius2 = Math.min(w, h) * 0.55;
            const radius3 = Math.min(w, h) * 0.7;

            // Subtle pulsing alpha
            const pulse1 = 0.15 + Math.sin(time * 0.5) * 0.05;
            const pulse2 = 0.12 + Math.sin(time * 0.5 + 1) * 0.04;
            const pulse3 = 0.08 + Math.sin(time * 0.5 + 2) * 0.03;

            // Top-left arcs (0 to PI/2)
            drawOrbitArc(0, 0, radius1, 0, Math.PI / 2, pulse1, 1.5);
            drawOrbitArc(0, 0, radius2, 0, Math.PI / 2, pulse2, 1);
            drawOrbitArc(0, 0, radius3, 0, Math.PI / 2, pulse3, 0.5);

            // Bottom-right arcs (PI to 1.5*PI)
            drawOrbitArc(w, h, radius1, Math.PI, Math.PI * 1.5, pulse1, 1.5);
            drawOrbitArc(w, h, radius2, Math.PI, Math.PI * 1.5, pulse2, 1);
            drawOrbitArc(w, h, radius3, Math.PI, Math.PI * 1.5, pulse3, 0.5);

            // Animated dots on top-left orbits (traveling from right to down)
            const dotAngle1 = (time * 0.8) % (Math.PI / 2);
            const dotAngle2 = (time * 0.6 + 0.5) % (Math.PI / 2);
            const dotAngle3 = (time * 0.4 + 1) % (Math.PI / 2);

            drawOrbitDot(0, 0, radius1, dotAngle1, 4, 0.9);
            drawOrbitDot(0, 0, radius2, dotAngle2, 3, 0.7);
            drawOrbitDot(0, 0, radius3, dotAngle3, 2, 0.5);

            // Animated dots on bottom-right orbits
            const brDotAngle1 = Math.PI + (time * 0.8) % (Math.PI / 2);
            const brDotAngle2 = Math.PI + (time * 0.6 + 0.5) % (Math.PI / 2);
            const brDotAngle3 = Math.PI + (time * 0.4 + 1) % (Math.PI / 2);

            drawOrbitDot(w, h, radius1, brDotAngle1, 4, 0.9);
            drawOrbitDot(w, h, radius2, brDotAngle2, 3, 0.7);
            drawOrbitDot(w, h, radius3, brDotAngle3, 2, 0.5);

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
        initMobileNav();
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

