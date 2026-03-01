// Estado de la aplicación
let currentToken = null;
let currentUser = null;

// API Base URL
const API_URL = '';

// ============================================
// AUTENTICACIÓN
// ============================================

async function handleLogin(event) {
    event.preventDefault();

    const correo = document.getElementById('correo').value;
    const password = document.getElementById('password').value;

    // Limpiar mensaje de error previo
    mostrarErrorLogin('');

    // Validación básica antes de llamar al backend
    if (!correo || !password) {
        mostrarErrorLogin('Por favor completa todos los campos.');
        return;
    }

    // Deshabilitar botón mientras carga (evita doble submit)
    const btn = document.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.textContent = 'Iniciando sesión...';

    try {
        const response = await fetch(`${API_URL}/api/v1/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ correo, password })
        });

        const data = await response.json();
        console.log('Login response:', data);

        if (response.ok) {
            // Guardar en localStorage
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify(data.usuario));
            localStorage.setItem('tokenId', data.tokenId);

            console.log('Usuario guardado:', data.usuario);

            // Redirigir según el rol
            const rol = data.usuario.rol;
            console.log('Redirigiendo a:', rol);

            if (rol === 'ADMIN') {
                window.location.href = '/admin.html';
            } else if (rol === 'ESTUDIANTE') {
                window.location.href = '/estudiante.html';
            } else if (rol === 'DOCENTE') {
                window.location.href = '/docente.html';
            } else {
                window.location.href = '/estudiante.html';
            }
        } else {
            // ✅ FIX: Obtener el mensaje real del backend
            const mensaje = data.message || data.error || 'Credenciales inválidas';

            // ✅ FIX: Detectar si es bloqueo para mostrar mensaje especial
            const esBloqueado = mensaje.toLowerCase().includes('bloqueada') ||
                                mensaje.toLowerCase().includes('bloqueado');

            if (esBloqueado) {
                // Extraer minutos del mensaje si vienen (ej: "Espere 15 minutos")
                const match = mensaje.match(/(\d+)\s*minuto/i);
                const minutos = match ? match[1] : '15';
                mostrarErrorLogin(
                    `🔒 Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intente nuevamente en ${minutos} minutos o contacte al administrador.`,
                    'bloqueado'
                );
            } else {
                mostrarErrorLogin('❌ ' + mensaje);
            }
        }
    } catch (error) {
        console.error('Error en login:', error);
        mostrarErrorLogin('Sin conexión con el servidor. Verifique su red e intente nuevamente.');
    } finally {
        // Re-habilitar botón siempre
        btn.disabled = false;
        btn.textContent = 'Iniciar Sesión';
    }
}

/**
 * Muestra un mensaje de error inline debajo del formulario.
 * tipo: '' (normal) | 'bloqueado' (estilo especial naranja/rojo)
 */
function mostrarErrorLogin(mensaje, tipo = '') {
    let errorDiv = document.getElementById('login-error');

    // Crear el div si no existe aún
    if (!errorDiv) {
        errorDiv = document.createElement('div');
        errorDiv.id = 'login-error';
        const form = document.querySelector('form');
        form.appendChild(errorDiv);
    }

    if (!mensaje) {
        errorDiv.style.display = 'none';
        errorDiv.textContent = '';
        return;
    }

    // Estilos base
    errorDiv.style.display = 'block';
    errorDiv.style.marginTop = '12px';
    errorDiv.style.padding = '12px 16px';
    errorDiv.style.borderRadius = '8px';
    errorDiv.style.fontSize = '14px';
    errorDiv.style.fontWeight = '500';
    errorDiv.style.lineHeight = '1.5';

    if (tipo === 'bloqueado') {
        // Estilo especial para bloqueo — naranja oscuro
        errorDiv.style.backgroundColor = '#fff3cd';
        errorDiv.style.color = '#856404';
        errorDiv.style.border = '1px solid #ffc107';
    } else {
        // Estilo para error genérico — rojo suave
        errorDiv.style.backgroundColor = '#f8d7da';
        errorDiv.style.color = '#721c24';
        errorDiv.style.border = '1px solid #f5c6cb';
    }

    errorDiv.textContent = mensaje;
}

async function handleLogout() {
    try {
        const token = localStorage.getItem('token');
        if (token) {
            await fetch(`${API_URL}/api/v1/auth/logout`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });
        }
    } catch (error) {
        console.error('Error en logout:', error);
    } finally {
        localStorage.clear();
        window.location.href = '/index.html';
    }
}

// ============================================
// FUNCIONES COMUNES (existen en todas las páginas)
// ============================================

async function cargarEstadisticas() {
    const statsTotal = document.getElementById('stats-total');
    const statsActivas = document.getElementById('stats-activas');
    const statsRevocadas = document.getElementById('stats-revocadas');
    const statsExpiradas = document.getElementById('stats-expiradas');
    
    // Si no existe el elemento, salir (estamos en otra página)
    if (!statsTotal) return;
    
    try {
        const response = await fetch(`${API_URL}/api/v1/sesiones/estadisticas`);
        const stats = await response.json();
        
        statsTotal.textContent = stats.total || 0;
        statsActivas.textContent = stats.activas || 0;
        statsRevocadas.textContent = stats.revocadas || 0;
        statsExpiradas.textContent = stats.expiradas || 0;
    } catch (error) {
        console.error('Error cargando estadísticas:', error);
    }
}

async function cargarRoles() {
    const container = document.getElementById('roles-container');
    if (!container) return;
    
    try {
        const response = await fetch(`${API_URL}/api/v1/usuarios/roles`);
        const roles = await response.json();
        
        if (roles && roles.length > 0) {
            container.innerHTML = roles.map(rol => `
                <div class="role-card">
                    <h4>${rol.nombre}</h4>
                    <div class="permissions">
                        ${rol.permisos.map(p => `<span class="permission-tag">${p}</span>`).join('')}
                    </div>
                </div>
            `).join('');
        } else {
            container.innerHTML = '<p class="empty-state">No hay roles disponibles</p>';
        }
    } catch (error) {
        console.error('Error cargando roles:', error);
    }
}

// ============================================
// FUNCIONES PARA ADMIN (solo en admin.html)
// ============================================

async function cargarDashboardAdmin() {
    // Actualizar navbar
    const navName = document.getElementById('user-name');
    const navEmail = document.getElementById('user-email');
    const navAvatar = document.getElementById('user-avatar');
    if (navName) navName.textContent = currentUser.nombreCompleto || currentUser.nombre;
    if (navEmail) navEmail.textContent = currentUser.correo;
    if (navAvatar) navAvatar.textContent = currentUser.nombre.charAt(0).toUpperCase();
    // Verificar que estamos en admin.html
    const userName = document.getElementById('user-name');
    const userEmail = document.getElementById('user-email');
    
    if (userName && userEmail) {
        userName.textContent = currentUser.nombreCompleto || currentUser.nombre;
        userEmail.textContent = currentUser.correo;
    }

    // Mensaje de bienvenida en el dashboard
    const bienvenida = document.getElementById('bienvenida');
    if (bienvenida) {
        const nombreMostrar = currentUser.nombreCompleto || currentUser.nombre;
        bienvenida.innerHTML = `<p>¡Hola, <strong>${nombreMostrar}</strong>! Bienvenido al panel de administración de Campus360.</p>`;
    }
    
    // Cargar cada sección solo si existe el elemento
    await cargarEstadisticas(); // Ya verifica internamente
    await cargarRoles(); // Ya verifica internamente
    await cargarTodasLasSesiones();
}

async function cargarTodasLasSesiones() {
    const tbody = document.getElementById('sesiones-tbody');
    if (!tbody) return;
    
    try {
        // NUEVO ENDPOINT: trae TODAS las sesiones de TODOS los usuarios
        const response = await fetch(`${API_URL}/api/v1/sesiones/todas`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        const sesiones = await response.json();
        
        if (sesiones && sesiones.length > 0) {
            tbody.innerHTML = sesiones.map(s => `
                <tr>
                    <td>${s.id}</td>
                    <td>${s.usuarioNombre} (${s.usuarioCorreo})</td>
                    <td>${s.tokenPreview}</td>
                    <td>${s.dispositivo || 'Desconocido'}</td>
                    <td>${s.ip || 'N/A'}</td>
                    <td>${s.minutosRestantes} min</td>
                    <td>
                        <button class="btn btn-danger btn-sm" onclick="revocarSesion(${s.id})">
                            Revocar
                        </button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No hay sesiones activas</td></tr>';
        }
    } catch (error) {
        console.error('Error cargando sesiones:', error);
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Error al cargar sesiones</td></tr>';
    }
}

async function buscarSesionesUsuario() {
    const input = document.getElementById('buscarUsuario');
    const resultado = document.getElementById('resultadoBusqueda');
    const tbody = document.getElementById('sesiones-tbody');
    
    if (!input || !resultado || !tbody) return;
    
    const termino = input.value.trim();
    if (!termino) {
        resultado.innerHTML = '<div class="alert alert-error">Ingresa un ID, correo o nombre</div>';
        return;
    }
    
    try {
        // Mostrar loading
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Buscando...</td></tr>';
        
        const response = await fetch(`${API_URL}/api/v1/sesiones/buscar?termino=${encodeURIComponent(termino)}`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        const sesiones = await response.json();
        
        if (sesiones && sesiones.length > 0) {
            tbody.innerHTML = sesiones.map(s => `
                <tr>
                    <td>${s.id}</td>
                    <td>${s.usuarioNombre} (${s.usuarioCorreo})</td>
                    <td>${s.tokenPreview}</td>
                    <td>${s.dispositivo || 'Desconocido'}</td>
                    <td>${s.ip || 'N/A'}</td>
                    <td>${s.minutosRestantes} min</td>
                    <td>
                        <button class="btn btn-danger btn-sm" onclick="revocarSesion(${s.id})">
                            Revocar
                        </button>
                    </td>
                </tr>
            `).join('');
            resultado.innerHTML = `<div class="alert alert-success">✅ ${sesiones.length} sesiones encontradas</div>`;
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No hay sesiones activas para este usuario</td></tr>';
            resultado.innerHTML = '<div class="alert alert-info">ℹ️ No se encontraron sesiones</div>';
        }
    } catch (error) {
        console.error('Error:', error);
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Error al buscar</td></tr>';
        resultado.innerHTML = '<div class="alert alert-error">Error al buscar sesiones</div>';
    }
}

// ============================================
// FUNCIONES PARA ESTUDIANTE (solo en estudiante.html)
// ============================================

async function cargarDashboardEstudiante() {
    // Actualizar navbar
    const navAvatar = document.getElementById('user-avatar');
    if (navAvatar) navAvatar.textContent = currentUser.nombre.charAt(0).toUpperCase();
    const userName = document.getElementById('user-name');
    const userEmail = document.getElementById('user-email');
    
    if (userName) userName.textContent = currentUser.nombreCompleto || currentUser.nombre;
    if (userEmail) userEmail.textContent = currentUser.correo;

    // Mensaje de bienvenida
    const bienvenida = document.getElementById('bienvenida');
    if (bienvenida) {
        const nombreMostrar = currentUser.nombreCompleto || currentUser.nombre;
        bienvenida.innerHTML = `<p>¡Hola, <strong>${nombreMostrar}</strong>! Bienvenido a tu portal estudiantil de Campus360.</p>`;
    }
    
    await Promise.all([
        cargarInfoCuenta(),
        cargarMisSesionesEstudiante(),
        cargarSesionActualEstudiante()
    ]);
}

async function cargarInfoCuenta() {
    const info = document.getElementById('info-cuenta');
    if (!info) return;
    
    info.innerHTML = `
        <p><strong>ID:</strong> ${currentUser.id}</p>
        <p><strong>Correo:</strong> ${currentUser.correo}</p>
        <p><strong>Nombre:</strong> ${currentUser.nombre}</p>
        <p><strong>Rol:</strong> ${currentUser.rol}</p>
    `;
}

async function cargarSesionActualEstudiante() {
    const sesionActual = document.getElementById('sesion-actual');
    if (!sesionActual) return;
    
    try {
        const response = await fetch(`${API_URL}/api/v1/sesiones/usuario/${currentUser.id}`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        const sesiones = await response.json();
        const tokenActual = localStorage.getItem('token');
        
        if (sesiones && sesiones.length > 0) {
            const actual = sesiones.find(s => tokenActual && s.tokenPreview && 
                tokenActual.includes(s.tokenPreview.replace('...', '').replace('jwt_', '')));
            
            if (actual) {
                sesionActual.innerHTML = `
                    <p><strong>Dispositivo:</strong> ${actual.dispositivo || 'Desconocido'}</p>
                    <p><strong>IP:</strong> ${actual.ip || 'No registrada'}</p>
                    <p><strong>Iniciada:</strong> ${new Date(actual.fechaCreacion).toLocaleString()}</p>
                    <p><strong>Expira en:</strong> ${actual.minutosRestantes} minutos</p>
                `;
                return;
            }
        }
        
        sesionActual.innerHTML = '<p>No se pudo identificar la sesión actual</p>';
    } catch (error) {
        console.error('Error cargando sesión actual:', error);
        sesionActual.innerHTML = '<p>Error al cargar la sesión actual</p>';
    }
}

async function cargarMisSesionesEstudiante() {
    const tbody = document.getElementById('mis-sesiones-tbody');
    if (!tbody) return;
    
    try {
        const response = await fetch(`${API_URL}/api/v1/sesiones/usuario/${currentUser.id}`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        const sesiones = await response.json();
        
        if (sesiones && sesiones.length > 0) {
            tbody.innerHTML = sesiones.map(s => `
                <tr>
                    <td>${s.dispositivo || 'Desconocido'}</td>
                    <td>${s.ip || 'N/A'}</td>
                    <td>${new Date(s.fechaCreacion).toLocaleString()}</td>
                    <td>${s.minutosRestantes} minutos</td>
                    <td><span class="badge badge-active">Activa</span></td>
                    <td>
                        <button class="btn btn-danger btn-sm" onclick="revocarSesion(${s.id})">
                            Revocar
                        </button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No hay sesiones activas</td></tr>';
        }
    } catch (error) {
        console.error('Error cargando sesiones:', error);
        tbody.innerHTML = '<tr><td colspan="6" class="empty-state">Error al cargar sesiones</td></tr>';
    }
}

// ============================================
// FUNCIONES PARA DOCENTE (solo en docente.html)
// ============================================

async function cargarDashboardDocente() {
    // Actualizar navbar
    const navAvatar = document.getElementById('user-avatar');
    if (navAvatar) navAvatar.textContent = currentUser.nombre.charAt(0).toUpperCase();
    const userName = document.getElementById('user-name');
    const userEmail = document.getElementById('user-email');
    
    if (userName) userName.textContent = currentUser.nombreCompleto || currentUser.nombre;
    if (userEmail) userEmail.textContent = currentUser.correo;

    // Mensaje de bienvenida
    const bienvenida = document.getElementById('bienvenida');
    if (bienvenida) {
        const nombreMostrar = currentUser.nombreCompleto || currentUser.nombre;
        bienvenida.innerHTML = `<p>¡Hola, <strong>${nombreMostrar}</strong>! Bienvenido a tu portal docente de Campus360.</p>`;
    }
    
    await Promise.all([
        cargarInfoCuentaDocente(),
        cargarMisSesionesDocente()
    ]);
}

async function cargarInfoCuentaDocente() {
    const info = document.getElementById('info-cuenta');
    if (!info) return;
    
    info.innerHTML = `
        <p><strong>ID:</strong> ${currentUser.id}</p>
        <p><strong>Correo:</strong> ${currentUser.correo}</p>
        <p><strong>Nombre:</strong> ${currentUser.nombre}</p>
        <p><strong>Rol:</strong> ${currentUser.rol}</p>
    `;
}

async function cargarMisSesionesDocente() {
    const tbody = document.getElementById('mis-sesiones-tbody');
    if (!tbody) return;
    
    try {
        const response = await fetch(`${API_URL}/api/v1/sesiones/usuario/${currentUser.id}`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        const sesiones = await response.json();
        
        if (sesiones && sesiones.length > 0) {
            tbody.innerHTML = sesiones.map(s => `
                <tr>
                    <td>${s.dispositivo || 'Desconocido'}</td>
                    <td>${s.ip || 'N/A'}</td>
                    <td>${new Date(s.fechaCreacion).toLocaleString()}</td>
                    <td>${s.minutosRestantes} minutos</td>
                    <td><span class="badge badge-active">Activa</span></td>
                    <td>
                        <button class="btn btn-danger btn-sm" onclick="revocarSesion(${s.id})">
                            Revocar
                        </button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No hay sesiones activas</td></tr>';
        }
    } catch (error) {
        console.error('Error cargando sesiones:', error);
        tbody.innerHTML = '<tr><td colspan="6" class="empty-state">Error al cargar sesiones</td></tr>';
    }
}

// ============================================
// FUNCIÓN COMPARTIDA PARA REVOCAR SESIONES
// ============================================

async function revocarSesion(tokenId) {
    if (!confirm('¿Estás seguro de que quieres revocar esta sesión?')) return;
    
    try {
        const response = await fetch(`${API_URL}/api/v1/sesiones/${tokenId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        if (response.ok) {
            alert('✅ Sesión revocada exitosamente');
            
            // ACTUALIZAR TODO AUTOMÁTICAMENTE
            const path = window.location.pathname;
            if (path.includes('admin.html')) {
                await Promise.all([
                    cargarEstadisticas(),      // Actualiza los 4 cuadros
                    cargarTodasLasSesiones()   // Actualiza la tabla
                ]);
            } else if (path.includes('estudiante.html')) {
                await Promise.all([
                    cargarMisSesionesEstudiante(),
                    cargarSesionActualEstudiante()
                ]);
            } else if (path.includes('docente.html')) {
                await cargarMisSesionesDocente();
            }
        } else {
            alert('❌ Error al revocar la sesión');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error revocando sesión');
    }
}

// ============================================
// INICIALIZACIÓN
// ============================================

document.addEventListener('DOMContentLoaded', async () => {
    const path = window.location.pathname;
    console.log('Página actual:', path);
    
    // Si estamos en index.html, no hacemos nada
    if (path.includes('index.html') || path === '/') {
        console.log('Página de login, no se requiere autenticación');
        return;
    }
    
    // Recuperar sesión
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    
    if (!savedToken || !savedUser) {
        console.log('No hay sesión, redirigiendo a login');
        window.location.href = '/index.html';
        return;
    }
    
    try {
        currentToken = savedToken;
        currentUser = JSON.parse(savedUser);
        
        console.log('Sesión recuperada:', currentUser);
        
        // Validar según la página
        if (path.includes('admin.html')) {
            if (currentUser.rol !== 'ADMIN') {
                alert('No tienes permisos para acceder al panel de administración');
                window.location.href = '/index.html';
                return;
            }
            await cargarDashboardAdmin();
            
        } else if (path.includes('estudiante.html')) {
            if (currentUser.rol !== 'ESTUDIANTE') {
                alert('Esta página es solo para estudiantes');
                window.location.href = '/index.html';
                return;
            }
            await cargarDashboardEstudiante();
            
        } else if (path.includes('docente.html')) {
            if (currentUser.rol !== 'DOCENTE') {
                alert('Esta página es solo para docentes');
                window.location.href = '/index.html';
                return;
            }
            await cargarDashboardDocente();
        }
        
    } catch (error) {
        console.error('Error en inicialización:', error);
        alert('Error al cargar el panel. Por favor, inicia sesión nuevamente.');
        localStorage.clear();
        window.location.href = '/index.html';
    }
});