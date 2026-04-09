function toastHTML(message, type) {
    let toastClass = 'text-bg-danger';  // Default to 'danger' for error
    let toastTitle = 'Error:';  // Default title

    if (type === 'success') {
        toastClass = 'text-bg-success';
        toastTitle = 'Success:';
    } else if (type === 'warning') {
        toastClass = 'text-bg-warning';
        toastTitle = 'Warning:';
    }

    let toastHTML = `
    <div class="position-fixed p-3" style="z-index: 1100; top: 20px; right: 20px;">
        <div class="toast align-items-center ${toastClass} border-0 shadow-lg fade show" 
             role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <strong>${toastTitle}</strong> ${message}
                </div>
                <button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    </div>
    `;

    return toastHTML;
}


function customToastAlert(elementID, message, type) {
    let targetElement = document.getElementById(elementID);
    if (!targetElement) {
        console.error(`Element with ID '${elementID}' not found.`);
        return;
    }
    let html = toastHTML(message, type)
    targetElement.innerHTML = html;

    // Show toast using Bootstrap JS API
    let toastEl = targetElement.querySelector('.toast');
    let toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();
}