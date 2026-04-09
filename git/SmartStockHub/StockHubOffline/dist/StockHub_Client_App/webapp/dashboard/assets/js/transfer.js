// Initialize on page load
let allProducts = [];
let transferList = [];

document.addEventListener("DOMContentLoaded", async function () {
    const response = await loadProducts();
    if (response && response.productList) {
        allProducts = response.productList;
        // Don't populate selects, instead wait for user input
    }

    setupSearchableDropdown('fromItem', 'fromItemDropdown', updateFromItemAvailability, (p) => !p.SKU.startsWith("AC "));
    setupSearchableDropdown('toItem', 'toItemDropdown', updateToItemAvailability, (p) => p.SKU.startsWith("AC "), [{ SKU: "External" }]);

    document.getElementById('btnAdd').addEventListener('click', addToTransferTable);

    // Delegate delete button click
    document.getElementById('transferTable').addEventListener('click', function (e) {
        if (e.target.classList.contains('delete-btn')) {
            const index = e.target.getAttribute('data-index');
            deleteTransfer(index);
        }
    });

    document.getElementById('btnSaveTransfer').addEventListener('click', saveTransfers);

    // Hide dropdowns when clicking outside
    document.addEventListener('click', function (e) {
        if (!e.target.closest('.position-relative')) {
            document.querySelectorAll('.dropdown-menu').forEach(menu => menu.style.display = 'none');
        }
    });
});

function setupSearchableDropdown(inputId, dropdownId, onSelectCallback, customFilter, extraItems = []) {
    const input = document.getElementById(inputId);
    const dropdown = document.getElementById(dropdownId);
    let currentFocus = -1;

    input.addEventListener('input', function () {
        const query = this.value.toLowerCase();
        dropdown.innerHTML = '';
        currentFocus = -1;

        if (query.length === 0) {
            dropdown.style.display = 'none';
            return;
        }

        let filtered = allProducts.filter(p => p.SKU.toLowerCase().includes(query));

        if (customFilter) {
            filtered = filtered.filter(customFilter);
        }

        if (extraItems) {
            const extraFiltered = extraItems.filter(p => p.SKU.toLowerCase().includes(query));
            filtered = filtered.concat(extraFiltered);
        }

        if (filtered.length > 0) {
            filtered.forEach(p => {
                const li = document.createElement('li');
                const a = document.createElement('a');
                a.className = 'dropdown-item';
                a.href = '#';
                a.textContent = p.SKU;
                a.onclick = (e) => {
                    e.preventDefault();
                    selectItem(p.SKU);
                };
                li.appendChild(a);
                dropdown.appendChild(li);
            });
            dropdown.style.display = 'block';
        } else {
            dropdown.style.display = 'none';
        }
    });

    input.addEventListener('keydown', function (e) {
        let items = dropdown.getElementsByTagName('a');
        if (e.key === 'ArrowDown') {
            currentFocus++;
            addActive(items);
            if (currentFocus >= 0 && items[currentFocus]) {
                items[currentFocus].scrollIntoView({ block: 'nearest' });
            }
        } else if (e.key === 'ArrowUp') {
            currentFocus--;
            addActive(items);
            if (currentFocus >= 0 && items[currentFocus]) {
                items[currentFocus].scrollIntoView({ block: 'nearest' });
            }
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (currentFocus > -1 && items) {
                items[currentFocus].click();
            }
        }
    });

    function addActive(items) {
        if (!items) return false;
        removeActive(items);
        if (currentFocus >= items.length) currentFocus = 0;
        if (currentFocus < 0) currentFocus = (items.length - 1);
        items[currentFocus].classList.add('active');
    }

    function removeActive(items) {
        for (let i = 0; i < items.length; i++) {
            items[i].classList.remove('active');
        }
    }

    function selectItem(sku) {
        input.value = sku;
        dropdown.style.display = 'none';
        if (onSelectCallback) onSelectCallback(sku);
    }

    input.addEventListener('focus', function () {
        if (this.value.length === 0) {
            // Show top 10 or something? Let's just follow strictly "type any char" to be safe with large lists.
        } else {
            this.dispatchEvent(new Event('input'));
        }
    });
}

function updateFromItemAvailability(sku) {
    const product = allProducts.find(p => p.SKU === sku);
    if (product) {
        document.getElementById('availableQty').textContent = product.stock || 0;
    } else {
        document.getElementById('availableQty').textContent = 0;
    }
}

function updateToItemAvailability(sku) {
    // User requested not to show available stock here.
    // This field will now be used for "Quantity to Add".
    document.getElementById('toItemAvailableQty').value = '';
}


// Load products from API
async function loadProducts() {
    try {
        let url = `${webmanagerurl}/productmaster`;
        const response = await getRequest(url);
        return response;
    } catch (error) {
        console.error("Failed to fetch products:", error);
        return { productList: [] };
    }
}

function addToTransferTable() {
    const fromSku = document.getElementById('fromItem').value;
    const toSku = document.getElementById('toItem').value;
    const toItemNewQty = parseInt(document.getElementById('toItemAvailableQty').value) || 0;

    if (!fromSku || !toSku) {
        alert("Please select both items.");
        return;
    }

    if (fromSku === toSku) {
        alert("From Item and To Item cannot be the same.");
        return;
    }

    const fromProduct = allProducts.find(p => p.SKU === fromSku);
    const toProduct = allProducts.find(p => p.SKU === toSku);
    const isExternal = toSku === "External";

    if (!fromProduct || (!toProduct && !isExternal)) {
        alert("Invalid product selection. Please select items from the list.");
        return;
    }

    const toItemOriginalStock = isExternal ? 0 : (parseInt(toProduct.stock) || 0);
    const transferQty = toItemNewQty; // Input is now "Quantity to Add"

    if (transferQty <= 0) {
        alert("Quantity to transfer must be positive.");
        return;
    }

    const fromItemCurrentStock = parseInt(fromProduct.stock) || 0;

    if (transferQty > fromItemCurrentStock) {
        alert(`Insufficient stock! You only have ${fromItemCurrentStock} of ${fromSku}.`);
        return;
    }

    // Add to list
    const transferRecord = {
        fromSku: fromSku,
        fromOriginal: fromItemCurrentStock,
        transferQty: transferQty,
        fromCurrent: fromItemCurrentStock - transferQty,

        toSku: toSku,
        toOriginal: toItemOriginalStock,
        toCurrent: toItemOriginalStock + transferQty // Calculate new stock
    };

    transferList.push(transferRecord);

    // Update in-memory stock for consistent further transfers
    fromProduct.stock = transferRecord.fromCurrent;
    if (toProduct) toProduct.stock = transferRecord.toCurrent;

    // Refresh UI
    updateFromItemAvailability(fromSku);
    if (toProduct) {
        document.getElementById('toItemAvailableQty').value = toProduct.stock;
    }

    // Clear inputs after add? optional. Let's keep them ensuring user sees what happened, or maybe clear them for next entry. 
    // Clearing is usually better for "Add" workflows.
    document.getElementById('fromItem').value = '';
    document.getElementById('toItem').value = '';
    document.getElementById('availableQty').textContent = '0';
    document.getElementById('toItemAvailableQty').value = '0';

    renderTable();
}

function deleteTransfer(index) {
    const record = transferList[index];

    // Revert stock changes in memory
    const fromProduct = allProducts.find(p => p.SKU === record.fromSku);
    const toProduct = allProducts.find(p => p.SKU === record.toSku);

    if (fromProduct) fromProduct.stock += record.transferQty;
    if (toProduct) toProduct.stock -= record.transferQty;

    // Remove from list
    transferList.splice(index, 1);

    // Update UI if the deleted item is currently selected in the filtered box (unlikely but possible)
    // We won't auto-fill the search boxes, just let the user search again.

    renderTable();
}

function renderTable() {
    const tbody = document.querySelector('#transferTable tbody');
    tbody.innerHTML = '';

    transferList.forEach((item, index) => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.fromSku}</td>
            <td>${item.fromOriginal}</td>
            <td>${item.transferQty}</td>
            <td>${item.fromCurrent}</td>
            <td>${item.toSku}</td>
            <td>${item.toOriginal}</td>
            <td>${item.transferQty}</td>
            <td>${item.toCurrent}</td>
            <td><button class="btn btn-sm btn-danger delete-btn" data-index="${index}">Delete</button></td>
        `;
        tbody.appendChild(row);
    });
}

async function saveTransfers() {
    if (transferList.length === 0) {
        customToastAlert("transfer-alert-container", "No transfers to save.", "warning");
        return;
    }

    const button = document.getElementById('btnSaveTransfer');
    const originalText = button.textContent;
    button.disabled = true;
    button.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Saving...';

    const url = `${webmanagerurl}/transfers`;
    // Construct payload per expected backend structure. 
    // Assuming a list of transfer requests.
    const body = {
        requests: transferList.map(t => ({
            fromSKU: t.fromSku,
            toSKU: t.toSku,
            quantity: t.transferQty,
            fromStock: t.fromCurrent,
            toStock: t.toCurrent
        }))
    };

    try {
        await postRequest(url, body);

        button.textContent = 'Saved!';
        button.classList.remove('btn-success');
        button.classList.add('btn-primary');

        customToastAlert("transfer-alert-container", "Transfers saved successfully!", "success");

        setTimeout(() => {
            location.reload();
        }, 1500);

    } catch (error) {
        console.error("Save failed", error);
        button.disabled = false;
        button.innerHTML = originalText;
        customToastAlert("transfer-alert-container", "Failed to save transfers. Please check usage or server status.", "error");
    }
}
