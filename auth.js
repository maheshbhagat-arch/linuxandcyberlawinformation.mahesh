document.addEventListener('DOMContentLoaded', function() {
    // Tab switching functionality
    const tabBtns = document.querySelectorAll('.tab-btn');
    const authForms = document.querySelectorAll('.auth-form');
    
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabId = btn.getAttribute('data-tab');
            
            // Update active tab button
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            // Show corresponding form
            authForms.forEach(form => {
                if (form.id === `${tabId}-form`) {
                    form.classList.add('active');
                } else {
                    form.classList.remove('active');
                }
            });
        });
    });
    
    // Toggle password visibility
    const togglePasswordBtns = document.querySelectorAll('.toggle-password');
    
    togglePasswordBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const input = btn.previousElementSibling;
            
            if (input.type === 'password') {
                input.type = 'text';
                btn.classList.remove('fa-eye');
                btn.classList.add('fa-eye-slash');
            } else {
                input.type = 'password';
                btn.classList.remove('fa-eye-slash');
                btn.classList.add('fa-eye');
            }
        });
    });
    
    // Form validation and submission
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();
            
            const email = document.getElementById('login-email').value;
            const password = document.getElementById('login-password').value;
            
            // Simple validation
            if (!email || !password) {
                showNotification('Please fill in all fields', 'error');
                return;
            }
            
            // Simulate login process
            showNotification('Login successful! Redirecting...', 'success');
            
            // In a real application, you would send this data to a server
            console.log('Login attempt:', { email, password });
            
            // Simulate redirect after successful login
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 2000);
        });
    }
    
    if (registerForm) {
        registerForm.addEventListener('submit', (e) => {
            e.preventDefault();
            
            const name = document.getElementById('register-name').value;
            const email = document.getElementById('register-email').value;
            const password = document.getElementById('register-password').value;
            const confirmPassword = document.getElementById('register-confirm-password').value;
            const termsAccepted = document.getElementById('terms').checked;
            
            // Validation
            if (!name || !email || !password || !confirmPassword) {
                showNotification('Please fill in all fields', 'error');
                return;
            }
            
            if (password !== confirmPassword) {
                showNotification('Passwords do not match', 'error');
                return;
            }
            
            if (!termsAccepted) {
                showNotification('You must accept the terms and conditions', 'error');
                return;
            }
            
            // Simulate registration process
            showNotification('Registration successful! Please check your email to verify your account.', 'success');
            
            // In a real application, you would send this data to a server
            console.log('Registration attempt:', { name, email, password });
            
            // Simulate redirect after successful registration
            setTimeout(() => {
                // Switch to login tab
                document.querySelector('[data-tab="login"]').click();
                
                // Reset form
                registerForm.reset();
            }, 3000);
        });
    }
    
    // Google sign-in button
    const googleBtns = document.querySelectorAll('.google-btn');
    
    googleBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            showNotification('Google sign-in would be implemented here with OAuth integration', 'info');
            
            // In a real application, you would initiate the Google OAuth flow
            console.log('Google sign-in clicked');
        });
    });
    
    // Forgot password link
    const forgotPasswordLink = document.querySelector('.forgot-password');
    
    if (forgotPasswordLink) {
        forgotPasswordLink.addEventListener('click', (e) => {
            e.preventDefault();
            showNotification('Password reset functionality would be implemented here', 'info');
        });
    }
    
    // Function to show notifications
    function showNotification(message, type) {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;
        
        // Add styles based on type
        let bgColor;
        switch(type) {
            case 'success':
                bgColor = 'rgba(0, 255, 136, 0.9)';
                break;
            case 'error':
                bgColor = 'rgba(255, 59, 48, 0.9)';
                break;
            case 'info':
                bgColor = 'rgba(0, 170, 255, 0.9)';
                break;
            default:
                bgColor = 'rgba(255, 255, 255, 0.9)';
        }
        
        notification.style.backgroundColor = bgColor;
        notification.style.color = '#0a0a0a';
        notification.style.padding = '1rem';
        notification.style.borderRadius = '4px';
        notification.style.position = 'fixed';
        notification.style.top = '100px';
        notification.style.left = '50%';
        notification.style.transform = 'translateX(-50%)';
        notification.style.zIndex = '2000';
        notification.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15)';
        notification.style.maxWidth = '80%';
        notification.style.textAlign = 'center';
        notification.style.fontWeight = '600';
        
        // Add to DOM
        document.body.appendChild(notification);
        
        // Remove after 3 seconds
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transition = 'opacity 0.5s ease';
            
            setTimeout(() => {
                document.body.removeChild(notification);
            }, 500);
        }, 3000);
    }
});
