$(document).ready(function () {
    updateShopName();
    initPOSView();
});
$('.view-sales').hide();

let occupiedColour = '#f8d7da';
let availableColour = '#d4edda';

let tableSaleData = [];
let currentTableData = {}
let accountTableData = [];
let saleEdited = false;
let products;
var isPettyCashEnabled = false;
let kotProductList = [];
let tableBillNumber;

function getActiveMenu() {
    const navContainer = document.querySelector(".menu-container");
    if (!navContainer) return null;
    const activeMenu = navContainer.querySelector(".nav-link.active");
    return activeMenu ? activeMenu.innerText.trim() : null;
}

async function initTableView() {

    if ($.fn.DataTable.isDataTable('#table-view-table')) {
        $('#table-view-table').DataTable().clear().destroy();
        $('#table-view-table').empty();
    }
    let url = getHost() + `/table`;
    const response = await getRequest(url);

    if (response.status === 'success') {
        accountTableData = response?.data?.table || [];
        $('#view-sales-button').removeAttr('style');
    } else {
        customToastAlert("table-reservation-alert-container", "Error loading data");
        return;
    }
    renderTableCards(accountTableData);

    function renderTableCards(data) {
        let container = $('#card-view-container');
        container.empty();

        data.forEach(row => {
            const timeOccupiedAtFormatted = row?.timeOccupiedAt
                ? (() => {
                    const date = new Date(row.timeOccupiedAt * 1000);
                    const day = String(date.getDate()).padStart(2, '0');
                    const month = String(date.getMonth() + 1).padStart(2, '0');
                    const year = date.getFullYear();
                    const hours = String(date.getHours()).padStart(2, '0');
                    const minutes = String(date.getMinutes()).padStart(2, '0');
                    const seconds = String(date.getSeconds()).padStart(2, '0');

                    return `${day}-${month}-${year}, ${hours}:${minutes}:${seconds}`;
                })()
                : '';

            let cardClass = row.status === 'OCCUPIED' ? 'occupied-table' : 'available-table';

            let actionButtons = row.status === 'OCCUPIED'
                ? `<button class="btn btn-danger available-btn m-1" data-id="${row?.id}">Available</button>
                   <button class="btn btn-info view-btn" data-id="${row?.id}">Edit</button>`
                : `<button class="btn btn-secondary occupy-btn m-1" data-id="${row?.id}">Occupy</button>`;

            let cardHTML = `
                <div class="table-card ${cardClass}">
                    ${row?.tableSalesDocumentID ? `<div class="draft-label">Draft</div>` : ''}
                    <h5><strong>${row.name}</strong></h5>
                    <p><strong>Status:</strong> ${row.status}</p>
                    ${row.status === 'OCCUPIED' ? ` <p class="occupied-time">
                        <i class="fas fa-clock"></i>Occupied at : ${timeOccupiedAtFormatted}
                        </p>` : ''}
                    <div class="text-center">
                        ${actionButtons}
                    </div>
                </div>
            `;

            container.append(cardHTML);
        });
    }

    $('#tableSearchInput')?.on('input', function () {
        const inputText = $(this).val().toLowerCase();
        let data = accountTableData?.filter(items => items?.name?.toLowerCase()?.includes(inputText));
        renderTableCards(data);
    });

}

$('#close-day-sale-button').on('click', async function () {
    let draftSavedTables = accountTableData?.filter(item => item?.tableSalesDocumentID);

    if (draftSavedTables.length > 0) {
        customToastAlert("table-reservation-alert-container", "Some tables have saved drafts. Please check out saved drafts before closing the day", 'warning');
        return;
    }
    const modal = new bootstrap.Modal('#close-day-sale-modal');
    modal.show();

    let url = getHost() + `/table/closesale`;
    let response = await getRequest(url);
   // let response = JSON.parse(``)
    if (response?.status !== 'success') {
        customToastAlert("table-reservation-alert-container", "Error fetching data");
        return;
    }
    let productList = response?.data || [];
    loadProductTable(productList, response?.totalAcCharge || 0);
});

function updateStatus(tableID, status) {
    let url = getHost() + `/table/updatestatus`;

    let table = accountTableData?.find(item => item.id === Number(tableID));
    if (status === 'AVAILABLE' && table?.tableSalesDocumentID) {
        customToastAlert("table-reservation-alert-container", "Table has items in cart. Please view and save before making table available", 'warning');
        return;
    }
    let payload = {
        id: tableID,
        status: status.toUpperCase()
    }
    putRequest(url, payload);
    if (status === 'OCCUPIED') {
        viewDetails(tableID);
        $('#tableReservationModalClose').on('click', function () {
            if (tableSaleData?.length === 0) {
                updateStatus(tableID, 'AVAILABLE');
            }
            window.location.reload();
        });
    } else {
        window.location.reload();
    }
}

// ----------------------------------------------------------- Modal Functions -----------------------------------------------------------------------------

function viewDetails(id) {
    console.log('Viewing details for ID:', id);
    $('#ac-charges-checkbox').hide();
    $('#tableReservationModal').modal('show');
    loadTableData(id);
    loadProducts();
    try {
        let account = JSON.parse(localStorage.getItem("account"));
        if (account?.isBarCodeScanEnabled) {
            loadBarCodeScanner();
        }
        let acChargePercent = account?.acChargePercent || 0;
        if (acChargePercent > 0) {
            $('#add-ac-charge-checkbox').show();
        }
    } catch (error) {
        console.error('Error saving draft', error);
    }
}

function getAccountData() {
    try {
        let account = JSON.parse(localStorage.getItem("account"));
        return account;
    } catch (error) {
        console.error('Error saving draft', error);
    }
}

async function loadProducts() {
    let getProductMaster = true;
    let account = JSON.parse(localStorage.getItem('account'));
    
    let localStorageProducts = localStorage.getItem('products');
    if (localStorageProducts) {
        try {
            products = JSON.parse(localStorageProducts);
            getProductMaster = false;
        } catch (error) {
            console.error("Error parsing products:", error);
        }
    } else {
        console.log("No products found in localStorage.");
    }
    

    if (getProductMaster) {
        let url = getHost() + `/productmaster`;
        products = await getRequest(url);
        if (isLocalStorageEnabled) {
            localStorage.setItem('products', JSON.stringify(products))
        }
    }

    const dropdownMenu = $('#dropdownContent');
    dropdownMenu.empty();
  //  products.productList = products?.productList?.filter(item => item?.stock > 0 || item?.category?.toLowerCase() === 'kitchen');
    products?.productList?.forEach(function (item) {
        const $item = $('<li><a class="dropdown-item">' + item.SKU + ' - ' + item.brand + '</a></li>');
        dropdownMenu.append($item);
    });
}

async function loadTableData(id) {
    let saleResponse;
    $('#billNo').text('');
    currentTableData = accountTableData?.find(item => item?.id === Number(id));
    $('#tableReservationModalHeader').text(currentTableData?.name);
    if (currentTableData?.tableSalesDocumentID) {
        let url = getHost() + `/table/getsingletablesale/${currentTableData?.tableSalesDocumentID}`;
        saleResponse = await getRequest(url);
        tableSaleData = saleResponse?.data?.saleDetails || [];
    } else {
        tableSaleData = [];
    }
    tableBillNumber = currentTableData?.billNumber || saleResponse?.data?.billNumber || '';
    $('#needToAddAcCharges').prop('checked', false);
    $('#needToAddAcCharges').prop('disabled', false);
    let acCharge = saleResponse?.data?.acCharge;
    if (acCharge > 0) {
        $('#needToAddAcCharges').prop('checked', true);
        $('#needToAddAcCharges').prop('disabled', true);
    }
    if (tableBillNumber) {
        $('#billNo').text(`Bill No : ${tableBillNumber || ''}`);
    }

    $('#searchInput-waiters').val(saleResponse?.data?.waiter);
    renderTableSaleData();

}

$('#searchInput').on('input', function () {
    const query = $(this).val().toLowerCase();
    const $dropdownContent = $('#dropdownContent');
    $dropdownContent.empty();

    const filteredProducts = products?.productList?.filter(function (item) {
        return item?.SKU?.toLowerCase().includes(query) || item?.brand?.toLowerCase().includes(query) || item?.code?.toLowerCase()?.includes(query);
    });

    filteredProducts.forEach(function (item) {
        const $item = $('<li><a class="dropdown-item" href="#">' + item.SKU + ' - ' + item.brand + '</a></li>');
        $dropdownContent.append($item);
    });

    if (filteredProducts.length > 0) {
        $dropdownContent.show();
    } else {
        $dropdownContent.hide();
    }
});


$('#dropdownContent').on('click', 'a', function () {
    const selectedItemText = $(this).text();
    const selectedSKU = selectedItemText.split(' - ')[0];

    const selectedBrand = selectedItemText.split(' - ')[1];

    const selectedProduct = products.productList.find(item => item.SKU === selectedSKU && item.brand === selectedBrand);

    $('#searchInput').val(selectedItemText);
});

$('#add-to-cart').on('click', function () {
    const selectedItemText = $('#searchInput').val();
    const selectedSKU = selectedItemText.split(' - ')[0];
    const selectedBrand = selectedItemText.split(' - ')[1];

    const quantity = Number($('#input-qty').val()) || 1;


    addSaleProduct({ selectedSKU, selectedBrand, quantity });
    $('#searchInput').val('');
    $('#input-qty').val(1);
});

function addSaleProduct({ selectedSKU, selectedBrand, quantity, UPC }) {
    let selectedProduct = {};

    if (UPC && UPC !== undefined) {
        selectedProduct = products?.productList?.find(item =>
            item?.UPC?.split(',').map(upc => upc.trim()).includes(UPC)
        );
        selectedSKU = selectedProduct?.SKU;
        selectedBrand = selectedProduct?.brand;
    } else {
        selectedProduct = products?.productList?.find(item => item?.SKU === selectedSKU && item?.brand === selectedBrand);
    }

    if (!selectedProduct) {
        customToastAlert("table-reservation-modal-alert-container", "Product not found");
        return;
    }

    if (isNaN(quantity) || quantity === "" || quantity === 0) {
        customToastAlert("table-reservation-modal-alert-container", "Invalid quantity");
        return;
    }

    if (!Array.isArray(tableSaleData)) {
        console.error('tableData is not an array');
    }

    let existingItem = tableSaleData?.find(item => (item?.SKU === selectedSKU && item?.brand === selectedBrand));

  //  if ((existingItem && existingItem?.quantity + quantity > selectedProduct?.stock) && selectedProduct?.category?.toLowerCase() !== 'kitchen') {
  //      customToastAlert("table-reservation-modal-alert-container", "Quantity exceeds available stock");
  //      return;
  //  }

  //  if (!existingItem && ((selectedProduct?.quantity || 0) + quantity > selectedProduct?.stock) && selectedProduct?.category?.toLowerCase() !== 'kitchen') {
  //      customToastAlert("table-reservation-modal-alert-container", "Quantity exceeds available stock");
  //      return;
  //  }

    if (existingItem && existingItem?.quantity + quantity >= 0) {
        existingItem.quantity += quantity;
        existingItem.updateQuantity = (existingItem.updateQuantity || 0) + quantity;
    }
    // else if (existingItem && existingItem?.quantity + quantity == 0) {
    //     tableSaleData = tableSaleData?.filter(item => !(item?.SKU === selectedSKU && item?.brand === selectedBrand));
    // }
    else if (quantity > 0) {
        selectedProduct.quantity = quantity;
        selectedProduct.updateQuantity = quantity;
        tableSaleData.push(selectedProduct);
    } else {
        customToastAlert("table-reservation-modal-alert-container", "Invalid quantity");
        return;
    }
    saleEdited = true;

    //  ----------------------- add kot list --------------------

    let existingKOTProduct = kotProductList?.find(item => (item?.SKU === selectedSKU && item?.brand === selectedBrand));
    if (quantity > 0) {
        if (!existingKOTProduct) {
            kotProductList.push({
                SKU: selectedSKU,
                brand: selectedBrand,
                quantity: quantity
            })
        } else {
            existingKOTProduct.quantity += quantity;
        }
    }

    renderTableSaleData();
}

function renderTableSaleData() {

    let filteredData = tableSaleData.filter(item => item?.quantity > 0);
    const table = $('#tableSaleData').DataTable({
        autoWidth: false,
        data: filteredData,
        searching: false,
        paging: false,
        info: false,
        footer: true,
        destroy: true,
        columns: [
            {
                title: 'Item',
                width: "30%",
                render: function (data, type, row) {
                    return `${row?.SKU} `;
                }
            },
            {
                title: 'Category',
                width: "30%",
                render: function (data, type, row) {
                    return `${row?.category || ""} `;
                }
            },
            {
                title: 'Quantity',
                width: "20%",
                render: function (data, type, row) {
                    return `${row?.quantity}`;
                }
            },
            {
                title: 'Sale Amount',
                width: "20%",
                render: function (data, type, row) {
                    return parseFloat(row?.quantity) * parseFloat(row?.salePrice);
                }
            },
            {
                title: 'Action',
                width: "20%",
                render: function (data, type, row, rowData) {
                    return `
                          <button class="btn btn-danger btn-sm delete-row" data-id="${rowData?.row}">
                                <iconify-icon icon="solar:trash-bin-trash-bold" class="fs-20 align-middle"></iconify-icon>
                        </button>
                    `;
                }
            }
        ],
        dom: "<'row'<'col-sm-6'l><'col-sm-6 d-flex justify-content-end'B>>" +
            "<'row'<'col-sm-12'tr>>" +
            "<'row'<'col-sm-5'i><'col-sm-7'p>>",
        buttons: [
            {
                extend: 'print',
                text: 'Print All (F6)',
                title: 'Bill',
                exportOptions: {
                    columns: ':visible'
                },
                attr: { id: 'printBtn' },
                customize: function (win) {
                    $(win.document.body).css({
                        'font-size': '12px', 
                        'font-family': 'Arial, sans-serif'
                    });

                    $(win.document.body).find('table').css({
                        'font-size': '12px', 
                        'font-family': 'Arial, sans-serif'
                    });

                    $(win.document.body).find('table').append(`
                        <tfoot>
                            <tr style="text-align:right">
                                <th colspan="3" style="text-align:right;vertical-align: bottom;">Total Amount:</th>
                                <th style="text-align:left" id="printTotalAmount"></th>
                            </tr>
                        </tfoot>
                    `);

                    let totalAmount = $('#tableSaleData').DataTable().column(3).footer().innerHTML;
                    $(win.document.body).find('#printTotalAmount').html(totalAmount);
                }
            },
            {
                text: 'Print KOT (F5)',
                action: function (e, dt, button, config) {
                    let account = JSON.parse(localStorage.getItem("account"));
                    let waiter = $('#searchInput-waiters')?.val() || '';
                    let currentDate = new Date();
                    let formattedDate = currentDate.toLocaleDateString();
                    let formattedTime = currentDate.toLocaleTimeString();
                    let printContent = `
                    <html>
                    <head>
                        <title>Print</title>
                        <style>
                            @page {
                            size: 10cm 10cm;
                            margin: 0;
                            }
                            @media print { @page { margin: 0;} body { margin: 1cm; } }
                            body { font-size: 12px; font-family: Arial, sans-serif; }
                            table { width: 100%; border-collapse: collapse; margin-top: 10px; margin-left: 40px; }
                            th, td { padding: 6px; text-align: left; }
                            .center-text { text-align: center; font-size: 14px; }
                            .header-container { 
                                display: flex; 
                                justify-content: space-between; 
                                align-items: flex-start; 
                                margin-bottom: 10px; 
                                padding: 0 30px; /* Add spacing on sides */
                            }
                            .header-left, .header-right { 
                                font-size: 12px; 
                                width: 50%; /* Equal width for both columns */
                            }
                            .header-left { text-align: left; }
                            .header-right { text-align: right; }
                            hr { border: 1px solid black; margin: 5px 0; }
                        </style>
                    </head>
                    <body>
                        <div class="center-text">
                            <h3>${account?.shopName || 'Shop'}</h3>
                            <hr>
                            <h4>KOT</h4>
                            <hr>
                        </div>
                
                        <div class="header-container">
                            <div class="header-left">
                                <p>Bill No: ${tableBillNumber || ''}</p>
                                <p>Table: ${currentTableData?.name || ''}</p>
                                <p>Waiter: ${waiter || 'N/A'}</p>
                            </div>
                            <div class="header-right">
                                <p>Date: ${formattedDate} </p>
                                <p>Time: ${formattedTime} </p>
                            </div>
                        </div>
                        <hr>
                        <table>
                            <thead>
                                <tr>
                                    <th>Item</th>
                                    <th>Quantity</th>
                                </tr>
                            </thead>
                            <tbody>
                `;

                    if (kotProductList?.length === 0) {
                        customToastAlert("table-reservation-modal-alert-container", "No items to print KOT or KOT already printed");
                        return;
                    }

                    kotProductList?.forEach(row => {
                        printContent += `
                            <tr>
                                <td>${row.SKU || '-'}</td>
                                <td>${row.quantity || '0'}</td>
                            </tr>
                        `;
                    });

                    printContent += `
                                </tbody>
                            </table>
                            <hr>
                            <p class="center-text"> This is not a bill </p>
                        </body>
                        </html>
                    `;

                    let newWin = window.open('', '_blank');
                    newWin.document.write(printContent);
                    newWin.document.close();
                    newWin.print();
                    newWin.close();
                    kotProductList = [];
                },
                attr: { id: 'printKOTBtn' }
            }
        ],
        initComplete: function () {
            const menu = getActiveMenu();

            if (menu === 'POS') {
                $('#printKOTBtn').remove();
                $('#printBtn').remove();
            }
        },
        footerCallback: function (row, data, start, end, display) {
            let api = this.api();
            let total = data?.reduce(function (a, b) {
                let quantity = parseFloat(b?.quantity);
                let salePrice = parseFloat(b?.salePrice);

                if (!isNaN(quantity) && !isNaN(salePrice)) {
                    return a + (quantity * salePrice);
                }
                return a;
            }, 0);

            let html = '';

            let needToAddAcCharges = $('#needToAddAcCharges')?.is(':checked');
            if (needToAddAcCharges) {
                let account = getAccountData()
                let acChargePercent = account?.acChargePercent?.toFixed(2) || 0
                if (account && acChargePercent > 0) {
                    let acCharge = (total / 100) * acChargePercent;
                    html += `
                        <div style="text-align: right;width: 30%">${total.toFixed(2)}</div>
                        <div style="display: flex; justify-content: flex-end; width: 100%;">
                        <div style="text-align: right; width: 30%;">${acCharge.toFixed(2)}</div>
                        <div style="text-align: left;font-size:12px; width: 70%;overflow: visible;">(AC charge of ${acChargePercent} %)</div>
                    </div>
                        `;
                    total += acCharge;
                }
            }
            html += `<div style="text-align: right; vertical-align: bottom;width: 30%">${total.toFixed(2)}</div>`;
            $(api.column(3).footer()).html(html);
        },
    });

    $('#tableSaleData tbody').on('click', '.delete-row', function () {
        let row = $(this).closest('tr');
        let rowData = table.row(row).data();

        let originalIndex = tableSaleData?.findIndex(item => item?.SKU === rowData?.SKU && item?.category === rowData?.category);
        if (originalIndex !== -1) {
            if (tableSaleData[originalIndex].quantity > 0) {
                tableSaleData[originalIndex].quantity -= 1;
            }
        }
        renderTableSaleData();
    });

    $('#searchInput-waiters').on('focus input', function () {
        let text = $(this)?.val()?.toLowerCase();
        const dropdownContent = $('#waiters-dropdown');
        dropdownContent.empty();

        let waitersString = localStorage.getItem('waiters');
        if (waitersString) {
            let waiters = JSON.parse(waitersString);
            waiters = waiters?.filter(waiter => waiter?.status?.toUpperCase() === 'ACTIVE' && waiter?.name?.toLowerCase()?.includes(text));
            waiters?.forEach(function (item) {
                const $item = $('<li><a class="dropdown-item">' + item?.name + '</a></li>');
                dropdownContent.append($item);
            });
        }
    });

    $('#waiters-dropdown').on('click', 'a', function () {
        const text = $(this).text();
        $('#searchInput-waiters').val(text);
    });
}

$('#needToAddAcCharges')?.on('click', function () {
    renderTableSaleData();
});

document.addEventListener('keydown', function (event) {
    if (event.key === 'F5') {
        event.preventDefault();
        setTimeout(() => {
            let table = $('#tableSaleData').DataTable();
            table.buttons(1).trigger();
        }, 500);
    } else if (event.key == 'F6') {
        event.preventDefault();
        setTimeout(() => {
            let table = $('#tableSaleData').DataTable();
            table.buttons(0).trigger();
        }, 500);
        setTimeout(() => {
            document.querySelector('#tableReservationModalCheckOut').click();
        }, 2000);
    } else if (event.key == 'F8') {
        event.preventDefault();
        document.querySelector('#tableReservationModalCheckOut').click();
    } else if (event.key == 'F7') {
        event.preventDefault();
        document.querySelector('#tableReservationModalSaveDraft').click();
    }
});

document.addEventListener('DOMContentLoaded', function () {
    const modalElement = document.getElementById('tableReservationModal');

    modalElement?.addEventListener('hide.bs.modal', function () {
        if (saleEdited) {
            // get confirmation from user 
            let confirmation = confirm('You have unsaved changes. Do you want to leave?');
            if (confirmation) {
                resetValues();
            } else {
                event.preventDefault();
                return;
            }
        }
        resetValues();
    });
});

function resetValues() {
    $('#searchInput').val('');
    $('#searchInput-waiters').val('');
    $('#input-qty').val('');
    currentTableData = undefined;
    saleEdited = false;
 //   if ($.fn.DataTable.isDataTable('#tableSaleData')) {
 //       $('#tableSaleData').DataTable().destroy();
 //       $('#tableSaleData').empty();
 //   }
}

document.addEventListener('DOMContentLoaded', function () {
    document.querySelector('#card-view-container')?.addEventListener('click', function (event) {
        if (event.target.classList.contains('available-btn')) {
            let id = event.target.getAttribute('data-id');
            updateStatus(id, 'AVAILABLE');
        } else if (event.target.classList.contains('occupy-btn')) {
            let id = event.target.getAttribute('data-id');
            updateStatus(id, 'OCCUPIED');
        } else if (event.target.classList.contains('view-btn')) {
            let id = event.target.getAttribute('data-id');
            viewDetails(id);
        }
    });

    document?.querySelector('#tableReservationModalSaveDraft')?.addEventListener('click', async function (event) {

        let url = getHost() + `/table/savedraft`;
        if (tableSaleData?.length <= 0) {
            customToastAlert("table-reservation-modal-alert-container", "Empty data");
            return;
        }


        let payload = {
            id: currentTableData.id,
            data: tableSaleData,
            tableSalesDocumentID: currentTableData.tableSalesDocumentID
        }

        let needToAddAcCharges = $('#needToAddAcCharges').is(':checked');
        if (needToAddAcCharges) {
            let acCharge = 0;
            let account = getAccountData();
            let acChargePercent = parseFloat(account?.acChargePercent?.toFixed(2) || 0) || 0;
            if (account && acChargePercent > 0) {
                let total = tableSaleData?.reduce((total, item) => {
                    return total += item?.quantity * item?.salePrice;
                }, 0);
                acCharge = (total / 100) * acChargePercent;
                payload.acCharge = parseFloat(acCharge.toFixed(2));
            }
        }


        if (tableBillNumber) {
            payload.billNumber = tableBillNumber;
        }
        let waiter = $('#searchInput-waiters')?.val();
        if (waiter) {
            payload.waiter = waiter;
        }

        try {
            const response = await putRequest(url, payload);
            if (response?.status === 'success') {
                customToastAlert("table-reservation-modal-alert-container", "Draft saved successfully", "success");
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            }

        } catch (error) {
            console.error('Error saving draft', error);
        }
        resetValues();
    });

    document.querySelector('#tableReservationModalCheckOut').addEventListener('click', async function (event) {

        let url = getHost() + `/table/checkout`;
        const menu = getActiveMenu();
        let needToAddAcCharges = $('#needToAddAcCharges').is(':checked');
        let acCharge = 0;
        let account = getAccountData();
        let acChargePercent = parseFloat(account?.acChargePercent?.toFixed(2) || 0) || 0;

        if (menu === 'POS') {
            url = `${getHost()}/pos/checkout`;
        } else if (account && acChargePercent > 0 && needToAddAcCharges) {
            let total = tableSaleData?.reduce((total, item) => {
                return total += item?.quantity * item?.salePrice;
            }, 0);
            acCharge = parseFloat(((total / 100) * acChargePercent).toFixed(2));
        }
        if (tableSaleData?.length <= 0) {
            customToastAlert("table-reservation-modal-alert-container", "Empty data");
            return;
        }
        let payload = {
            id: currentTableData?.id,
            data: tableSaleData,
            tableSalesDocumentID: currentTableData?.tableSalesDocumentID
        }


        if (account && acChargePercent > 0) {
            payload.acCharge = acCharge;
        }

        if (tableBillNumber) {
            payload.billNumber = tableBillNumber;
        }
        let waiter = $('#searchInput-waiters')?.val();
        if (waiter) {
            payload.waiter = waiter;
        }
        try {
            const response = await putRequest(url, payload);
            if (response?.status === 'success') {
                customToastAlert("table-reservation-modal-alert-container", "Check out success", "success");
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                customToastAlert("table-reservation-modal-alert-container", "Unable to checkout.Error occured");
            }

        } catch (error) {
            console.error('Error saving Check', error);
        }
        resetValues();
    });
});


// ------------ close sales entry ----------------



async function loadProductTable(salesProductList, totalAcCharge) {
    let fullProductList = salesProductList;

    salesProductList = salesProductList?.filter(item => item?.category?.toLowerCase() !== 'kitchen') || [];
    let productMasterurl = getHost() + `/productmaster`;
    const response = await getRequest(productMasterurl);
   // const response = JSON.parse(``);
    let data = await constructList(response, salesProductList);
    var afterPurchaseHasSale = true;
    if (response.afterPurchaseHasSale != null) {
        afterPurchaseHasSale = response.afterPurchaseHasSale;
    }
    const container = document.getElementById("close-sales-entry-table");
    container.innerHTML = "";
    if (document.getElementById("close-sales-entry-table"))
        new gridjs.Grid({
            sort: true,
            pagination: {
                limit: 350
            },
            fixedHeader: true,
            height: '550px',
            data: data,
            columns: [
                {
                    name: "SKU",
                    formatter: (cell, row) => gridjs.html(`<span id="sellerSKU-${row.cells[8].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "Opening Stock",
                    formatter: (cell, row) => {
                        const stock = parseFloat(row.cells[3].data);
                        const purchaseStock = parseFloat(row.cells[2].data);
                        const openingStock = parseFloat(cell);
                        let cellValue;
                        if (!afterPurchaseHasSale) {
                            if (openingStock + purchaseStock == stock) {
                                cellValue = openingStock;
                            } else {
                                cellValue = stock;
                            }
                        } else {
                            cellValue = stock;
                        }
                        return gridjs.html(`<span id="opening-stock-${row.cells[8].data}" class="text-dark fw-medium mt-3">${cellValue}</span>`)
                    }

                },
                {
                    name: "Purchase",
                    formatter: (cell, row) => {
                        const stock = parseFloat(row.cells[3].data);
                        const purchaseStock = parseFloat(cell);
                        const openingStock = parseFloat(row.cells[1].data);
                        let cellValue = 0;
                        if (!afterPurchaseHasSale) {
                            if (openingStock + purchaseStock === stock) {
                                cellValue = purchaseStock;
                            }
                        }
                        return gridjs.html(`<span id="purchase-stock-${row.cells[8].data}" class="text-dark fw-medium mt-3">${cellValue}</span>`);
                    }
                },
                {
                    name: "Stock",
                    formatter: (cell, row) => {
                        let stock = parseFloat(row.cells[3].data);
                        return gridjs.html(`<span id="stock-${row.cells[8].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
                    }
                },
                {
                    name: "Closing Stock",
                    formatter: (cell, row) => gridjs.html(`<div class="col-md-8"><input type="number" id="closing-stock-${row.cells[8].data}" value="${row.cells[4].data}"+ class="form-control ram-sales" ></div>`)
                },
                {
                    name: "Sales",
                    formatter: (cell, row) => {
                        return gridjs.html(`<span id="sales-${row.cells[8].data}" class="text-dark fw-medium mt-3">${row.cells[5].data}</span>`);
                    }
                },
                {
                    name: "Sale Price",
                    formatter: (cell, row) => gridjs.html(`<span id="sale-price-${row.cells[8].data}" class="text-dark fw-medium mt-3">${cell}</span>`) // Render a button
                },
                {
                    name: "Total Sale Amount",
                    formatter: (cell, row) => gridjs.html(`<span id="saleAmount-${row.cells[8].data}" class="text-dark fw-medium mt-3 ram-saleAmount">${cell}</span>`) // Render a button
                },
                {
                    name: "index",
                    hidden: true,
                    formatter: (cell, row) => gridjs.html(`<span id="saleAmount-${row.cells[8].data}" class="text-dark fw-medium mt-3 ram-saleAmount">0</span>`) // Render a button
                }],
        }).render(document.getElementById("close-sales-entry-table")).updateConfig({ data: data }).forceRender();

    let account = JSON.parse(localStorage.getItem("account"));
    if (account?.isKitchenSalesEnabled) {
        let kitchenItems = fullProductList?.filter(item => item?.category?.toLowerCase() === 'kitchen') || [];
        let kitchenSalesData = constructKitchenItems(response?.productList || [], kitchenItems);
        const table = $('#kitchenSalesTable').DataTable({
            autoWidth: false,
            data: kitchenSalesData,
            searching: false,
            paging: false,
            info: false,
            destroy: true,
            columns: [
                {
                    title: 'SKU',
                    width: "20%",
                    data: 'SKU'
                },
                {
                    title: 'Quantity',
                    width: "10%",
                    render: function (data, type, row, meta) {
                        return `<input type="number" class="form-control quantity-input" data-row="${meta.row}" value="${row?.quantity || 0}" min="0">`;
                    }
                },
                {
                    title: 'Sale amount',
                    width: "10%",
                    render: function (data, type, row) {
                        return `<span>${row?.totalSaleAmount || 0}</span>`;
                    }
                }
            ],
            footerCallback: function (row, data, start, end, display) {
                let total = data.reduce((sum, row) => sum + (parseFloat(row.totalSaleAmount) || 0), 0);
                $('#kitchen-sales').text(total);
                $('#final-kitchen-sales').text(total);
                $('#payment-kitchenSales').val(total);
                $(this.api().column(2).footer()).html(`<b>${total.toFixed(2)}</b>`);
            }
        });
        $('#kitchenSalesTable tbody').on('input', '.quantity-input', function () {
            let rowIndex = $(this).data('row');
            let newQuantity = parseFloat($(this).val()) || 0;

            let rowData = table.row(rowIndex).data();
            rowData.quantity = newQuantity;
            rowData.totalSaleAmount = newQuantity * (parseFloat(rowData.salePrice) || 0);

            table.row(rowIndex).data(rowData).draw(false);

        });
        $('#kitchenSalesTab').show();
    } else {
        $('#kitchenSalesTab').hide().empty();
    }



    loadExpenses();
    // calculate total sales amount 

    let totalSales = data.reduce((acc, row) => {
        return acc += parseFloat(row[5] * row[6]) || 0;
    }, 0);

    $("#salePriceSum").text(totalSales);
    $("#final-sales").text(totalSales + totalAcCharge);
     // disabling petty case for tables as of now
    if (response.hasOwnProperty("pettyCash")) {
        $("#pettyCase-summary").show();
        if (response.pettyCash.$numberLong) {
            $("#opening-petty-cash").html(response.pettyCash.$numberLong);
        }
        isPettyCashEnabled = true;
    } else {
        $("#non-pettyCase-summary").show();
        $("#sales-denomination").hide();
    }
    if (totalAcCharge > 0) {
        $("#ac-charges").show();
        $("#total-ac-charges").text(totalAcCharge);
    } else {
        $("#total-ac-charges").text(0);
        $("#ac-charges").hide();
    }
    const storedAccount = localStorage.getItem('account');
    const accountInfo = storedAccount ? JSON.parse(storedAccount) : null;
    if (accountInfo && accountInfo.hasOwnProperty('isSalesSettlementEnabled')) {
        $("#finalSettlement-sec").show();
    }
    if (accountInfo && accountInfo.hasOwnProperty('isOtherIncomeEnabled')) {
        $("#other_income").show();
        $("#summary-other-income").show();
    }

}

async function constructList(response, salesProductList) {
    var tableList = [];
    var sortedProducts;
    var isCustomSortingEnabled = false;
    const storedAccount = localStorage.getItem('account');
    const accountInfo = storedAccount ? JSON.parse(storedAccount) : null;
    if (accountInfo && accountInfo.hasOwnProperty('isCustomSortingEnabled')) {
        isCustomSortingEnabled = accountInfo.isCustomSortingEnabled;
    }
    if (accountInfo && accountInfo.hasOwnProperty('isStoreRoomAvailable')) {
        response = await loadBARCounterProducts(response);
        isStoreRoomAvailable = true;
    }
    if (isCustomSortingEnabled) {
        sortedProducts = sortByValume(response.productList);
    } else {
        if(accountInfo && accountInfo.hasOwnProperty('isCustomOrderEnabled')) {
            sortedProducts = sortByCustomOrder(response.productList);
        }else {
           sortedProducts = sortByBrand(response.productList);
        }
    }
    for (var i in sortedProducts) {
        var item = sortedProducts[i];
        if(item.stock !=0){
            if (item?.category?.toLowerCase() === 'kitchen') {
                continue;
            }
            let filteredProduct = salesProductList.find(item => item?.SKU === sortedProducts[i]?.SKU);
            var tableRow = [];
            tableRow.push(item.SKU);
            tableRow.push(item.openingStock);
            tableRow.push(item.purchaseStock);
            tableRow.push(item.stock);
            tableRow.push(item.stock - (filteredProduct?.quantity || 0));
            tableRow.push(filteredProduct?.quantity || 0);
            tableRow.push(item.salePrice);
            tableRow.push(item.salePrice * (filteredProduct?.quantity || 0));
            tableRow.push(i);
            tableList.push(tableRow);
        }
    }
    return tableList;
}


function constructKitchenItems(productList, kitchenItemsList) {
    let kitchenItems = []
    for (var i in kitchenItemsList) {
        let singleItem = kitchenItemsList[i];
        let filteredProduct = productList.find(item => item?.SKU === singleItem?.SKU);
        let obj = {
            'SKU': singleItem?.SKU,
            'quantity': singleItem?.quantity,
            'salePrice': filteredProduct.salePrice,
            'totalSaleAmount': filteredProduct.salePrice * (singleItem?.quantity || 0)
        }
        kitchenItems.push(obj);
    }
    return kitchenItems;
}

async function loadExpenses() {
    let expensesurl = getHost() + `/expenses`;
    const response = await getRequest(expensesurl);
   //   const response = JSON.parse(``);
    const $dropdown = $('.ram-exp-name');
    $dropdown.empty();
    for (var i in response.data) {
        $dropdown.append($('<option></option>').val(response.data[i].name).text(response.data[i].name));
    }
    const storedAccount = localStorage.getItem('account');
    const accountInfo = storedAccount ? JSON.parse(storedAccount) : null;
    if (accountInfo && accountInfo.hasOwnProperty('isOtherIncomeEnabled')) {
        $(".ram-exp-nar").show();
    }

    //Load Date drop Down
    const $Salesdropdown = $('#salesDate');
    const today = new Date();

    for (let i = 0; i < 3; i++) {
        const date = new Date();
        date.setDate(today.getDate() - i);
        const formattedDate = date.toISOString().split('T')[0]; // Format as YYYY-MM-DD
        $Salesdropdown.append(`<option value="${formattedDate}">${formattedDate}</option>`);
    }

}

function saveSales() {
    let salesurl = getHost() + `/sales`;
    $('#saleSaveButton').html(`<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>`);
    var productList = constructProductList();
    var expenseList = constructExpenses();
    var payments = constructPayments();
    var sales = {};
    sales.productList = productList;
    sales.expenseList = expenseList;
    sales.payments = payments;
    sales.totalSalesAmount = parseFloat($("#final-sales").text());
    sales.totalExpensesAmount = parseFloat($("#final-expenses").text());
    sales.totalDigitalAmount = parseFloat($("#final-digital-payment").text());
    sales.finalCashSettlement = parseFloat($("#final-settlement").text());
    const selectedDate = $("#salesDate").val(); // Get selected date
    const unixTime = Math.floor(new Date(selectedDate).getTime() / 1000);
    sales.saleDate = unixTime;
    sales.shouldRemoveTableSales = true;
    if (isPettyCashEnabled) {
        sales.openingPettyCash = parseFloat($("#opening-petty-cash").text());
        sales.totalExpensesAmount = parseFloat($("#final-expenses-new").text());
        sales.totalDigitalAmount = parseFloat($("#digital-payment-new").text());
        sales.kitchenSales = parseFloat($("#kitchen-sales").text());
        sales.cashInHand = parseFloat($("#cash-in-hand").text());
        sales.closingPettyCash = parseFloat($("#closing-petty-cash").val());
    } else {
        var kitchenSalesValue = parseInt($("#final-kitchen-sales").text(), 10);
        var cashInHandValue = parseInt($("#final-cash-in-hand").val(), 10);
        var diffSettlementValue = parseInt($("#diff-settlement").text(), 10);
        if (kitchenSalesValue !== 0) {
            sales.kitchenSales = kitchenSalesValue;
        }
        if (cashInHandValue !== 0 && !isNaN(cashInHandValue)) { // Ensure it's a valid number
            sales.cashInHand = cashInHandValue;
        }
        if (diffSettlementValue !== 0) {
            sales.diffSettlement = diffSettlementValue;
        }
        var otherIncome = parseInt($("#final-other-income").text(), 10); 
        if (otherIncome !== 0) {
            sales.otherIncome = otherIncome;
            sales.otherIncomeNaretion = $("#payment-otherIncome-naration").val();
        }

    }

    let account = JSON.parse(localStorage.getItem("account"));
    if (account?.isKitchenSalesEnabled) {
        let kitchenSalesData = $('#kitchenSalesTable').DataTable().rows().data().toArray() || [];
        sales.kitchenSales = kitchenSalesData;
    }
    if (account?.isTableReservationEnabled) {
        sales.resetBillNumber = true;
    }
    if (account && account.hasOwnProperty('isStoreRoomAvailable')) {       
        sales.isStoreRoomAvailable = true;
    }
    postRequestWithCallback(salesurl, sales, saveSalesCallback, null);

}

function saveSalesCallback() {
    $('#saleSaveButton').html(`<span class="badge bg-primary me-1">Updated</span>`);
    $('#saleSaveButton').prop('disabled', true);
}



function calcualteFinalSettlementAmount() {
    var totSaleAmount = parseFloat($("#final-sales").text());
    var totExpenses = parseFloat($("#final-expenses").text());
    var totDigitalPayment = parseFloat($("#final-digital-payment").text());
    var kitchenSale = parseFloat($("#final-kitchen-sales").text()) || 0;
    var otherIncome = parseFloat($("#final-other-income").text()) || 0;
    $("#final-settlement").text((totSaleAmount + kitchenSale+otherIncome) - (totExpenses + totDigitalPayment));
}

function enableButton() {
    const validationResponse = validateItemsClosingStock();
    if (validationResponse !== "true") {
        alert(validationResponse); // Show the alert message
    } else {
        $('#saleSaveButton').prop('disabled', false);
        $("#iAMGdbtn").addClass("disabled")
            .css({
                "pointer-events": "none",
                "opacity": "0.6"
            })
            .off("click");
    }
}

function validateItemsClosingStock() {
    var returnValue = "true";
    $('.ram-saleAmount').each(function () {
        const id = $(this).attr('id');
        const index = id.replace(/\D/g, '');

        var SKU = $("#sellerSKU-" + index).text();
        var closingStockValue = $("#closing-stock-" + index).val().trim(); // Get the value and remove extra spaces

        if (closingStockValue === "") {
            returnValue = "SKU: " + SKU + " has no Closing Stock. Please add it before proceeding.";
            return false;
        } else {
            var closingStock = Number(closingStockValue); // Convert the value to a number
            if (isNaN(closingStock)) {
                returnValue = "SKU: " + SKU + " has an invalid number. Please check.";
                return false;
            }
        }

    });
    return returnValue;
}


function constructProductList() {
    var productList = [];
    $('.ram-saleAmount').each(function () {
        const id = $(this).attr('id');
        const index = id.replace(/\D/g, '');
        var product = {
            SKU: $("#sellerSKU-" + index).text(),
            openingStock: parseFloat($("#opening-stock-" + index).text()),
            purchaseStock: parseFloat($("#purchase-stock-" + index).text()),
            stock: parseFloat($("#stock-" + index).text()),
            closingStock: parseFloat($("#closing-stock-" + index).val()),
            sales: parseFloat($("#sales-" + index).text()),
            salePrice: parseFloat($("#sale-price-" + index).text()),
            totalSaleAmount: parseFloat($("#saleAmount-" + index).text()),
            category: getCategory($("#sellerSKU-" + index).text())
        };
        productList.push(product);
    });
    return productList;
}

function getCategory(SKU) {
    const productMaster = localStorage.getItem('products');
    const products = productMaster ? JSON.parse(productMaster) : null;
    for (var i in products.productList) {
        var item = products.productList[i];
        if (item.SKU == SKU) {
            return item.brand;
        }
    }
}


function constructExpenses() {
    var expenseList = [];
    $('.ram-exp-name').each(function () {
        const id = $(this).attr('id');
        
        const amount = $("#exp-amount-" + id).val();
        const narration = $("#exp-naration-" + id).val(); 

        if (!isNaN(parseFloat(amount))) {

            var expense = {
                details: $(this).val(),
                amount: parseFloat(amount)
            };

            // only add narration if not empty
            if (narration && narration.trim() !== "") {
                expense.narration = narration.trim();
            }

            expenseList.push(expense);
        }
    });
    $('.ram-exp-free-text').each(function () {
        const id = $(this).attr('id');
        if (!isNaN(parseFloat($("#exp-amount-" + id).val()))) {
            var expense = {
                details: $(this).val(),
                amount: parseFloat($("#exp-amount-" + id).val())
            };
            expenseList.push(expense);
        }
    });
    return expenseList;
}

function constructPayments() {
    var card = parseFloat($("#payment-card").val());
    var gpay = parseFloat($("#payment-gpay").val());
    var netbanking = parseFloat($("#payment-netbanking").val());
    var payment = {};
    if (!isNaN(card)) {
        payment.card = card;
    }
    if (!isNaN(gpay)) {
        payment.gpay = gpay;
    }
    if (!isNaN(netbanking)) {
        payment.netbanking = netbanking;
    }
    return payment;
}


$(document).ready(function () {
    // Function to handle blur event on any text box
    function onSalesProcess() {
        const id = $(this).attr('id');
        const number = id.replace(/\D/g, '');
        var salePrice = parseFloat($('#sale-price-' + number).text());
        var sales = updateSales(number);
        $("#saleAmount-" + number).text(sales * salePrice);

        udpateTotalSaleAmount();
    }

    $(document).on('input', '.ram-sales', onSalesProcess);

    $('.ram-exp').on('blur', udpateTotalExpensesAmount);

    function updateSales(number) {
        var openingStock = parseFloat($("#stock-" + number).text());
        var closingStock = parseFloat($("#closing-stock-" + number).val());
        var sales = openingStock - closingStock;
        $("#sales-" + number).text(sales.toFixed(2)); // Keep the result to two decimal places if needed
        return sales;
    }

    function udpateTotalSaleAmount() {
        let salePriceSum = 0;
        $('.ram-saleAmount').each(function () {
            const price = parseFloat($(this).text()) || 0;
            salePriceSum += price;
        });
        $("#salePriceSum").text(salePriceSum);
        $("#final-sales").text(salePriceSum);
        $("#actual-liquor-sale").text(salePriceSum);
    }

    function udpateTotalExpensesAmount() {
        let expSum = 0;
        $('.ram-exp').each(function () {
            const price = parseFloat($(this).val()) || 0;
            expSum += price;
        });
        $("#total-expenses").text(expSum);
        $("#final-expenses").text(expSum);
        $("#final-expenses-new").text(expSum);
    }

    function onPaymentProcess(quantity, denomination) {
        quantity = parseInt(quantity) || 0;
        const result = denomination * quantity;
        $(`#${denomination}s`).text(result);
        let total = 0;
        $('span[id$="s"]').each(function () {
            total += parseInt($(this).text()) || 0;
        });
        $('#total-fullCurrency-value').text(total);
        $('#cash-in-hand').text(total);
    }

    $(document).on('input', 'input.ram-payment-500', function () {
        onPaymentProcess($(this).val(), 500);
    });
    $(document).on('input', 'input.ram-payment-200', function () {
        onPaymentProcess($(this).val(), 200);
    });
    $(document).on('input', 'input.ram-payment-100', function () {
        onPaymentProcess($(this).val(), 100);
    });
    $(document).on('input', '#payment-kitchenSales', function () {
        $('#kitchen-sales').text($(this).val());
        $('#final-kitchen-sales').text($(this).val());
    });
    $(document).on('input', '#payment-otherIncome', function () {
        $('#final-other-income').text($(this).val());
    });

    $(document).on('input', '#payment-card, #payment-gpay', function () {
        const cardPayment = parseFloat($('#payment-card').val()) || 0;
        const digitalPayment = parseFloat($('#payment-gpay').val()) || 0;
        const totalPayment = cardPayment + digitalPayment;
        const totalPaymentNew = cardPayment + digitalPayment;
        $('#digital-payment-new').text(totalPaymentNew.toFixed(2));
        $('#final-digital-payment').text(totalPayment.toFixed(2));
    });
    $(document).on('input', '#closing-petty-cash', function () {
        const openingPettyCash = parseFloat($('#opening-petty-cash').text()) || 0;
        const expenses = parseFloat($('#final-expenses').text()) || 0;
        const digitalPayment = parseFloat($('#digital-payment-new').text()) || 0;
        const kitchenSales = parseFloat($('#kitchen-sales').text()) || 0;
        const cashInHand = parseFloat($('#cash-in-hand').text()) || 0;
        const closingPettyCash = parseFloat($(this).val()) || 0;

        const totalSale = (expenses + digitalPayment + cashInHand + closingPettyCash) - openingPettyCash;
        const liquorSale = totalSale - kitchenSales;

        // Update table
        $('#total-sale').text(totalSale.toFixed(2));
        $('#liquor-sale').text(liquorSale.toFixed(2));

        var diffLiquorSale = liquorSale.toFixed(2) - parseFloat($("#actual-liquor-sale").text());
        $("#diff-liquor-sale").text(diffLiquorSale);
        if (diffLiquorSale < 0) {
            $('#diff-liquor-sale-row').addClass('table-primary');
        } else {
            $('#diff-liquor-sale-row').addClass('table-success');
        }

    });
    $("#final-cash-in-hand").on("input", function () {
        let finalSettlement = parseFloat($("#final-settlement").text()) || 0;
        let cashInHand = parseFloat($(this).val()) || 0;
        let difference = cashInHand - finalSettlement;
        let formattedDifference = (difference >= 0 ? "+" : "-") + Math.abs(difference);
        $("#diff-settlement").text(formattedDifference);
    });

});


//  ---------------------------------------------   bar code scanner -----------------------------------------------------


function loadBarCodeScanner() {
    let salesData = [];

    $(document).keypress(function (event) {
        if (event.which === 13 && salesData.length > 0) {
            event.preventDefault();
            let scannedCode = salesData.join('').trim();
            salesData = [];
            addSaleProduct({ 'UPC': scannedCode, quantity: 1 });
        } else {
            salesData.push(String.fromCharCode(event.which));
        }
    });
}



//  -------------------------------------  POS  ----------------------------------------------------------------------------------

async function initPOSView() {
    tableSaleData = [];
    loadProducts();
    renderTableSaleData();
    loadBarCodeScanner();
    $('#view-sales-button').removeAttr('style');
    /* If user presses Space bar then check out event will trigger here */
    $(document).on('keydown', function (event) {
        if (event.key === ' ' || event.keyCode === 32) {
            event.preventDefault();
            $('#tableReservationModalCheckOut').trigger('click');
        }
    });
}



$('#add-waiter-button').on('click', function () {
    let waiters = localStorage.getItem('waiters');

    if (waiters) {
        waiters = JSON.parse(waiters);

        let rows = waiters.map(waiter => {
            return `<tr id="waiter_delete_${waiter.id}">
                <td>${waiter.id}</td>
                <td class="name">${waiter.name}</td>
                <td class="status">${waiter.status}</td>
                <td>
                    <iconify-icon icon="solar:pen-2-broken" class="btn btn-soft-primary btn-sm" data-id="${waiter.id}" onclick="editWaiter(${waiter.id})"></iconify-icon>
                </td>
            </tr>`;
        });

        // Append rows to the correct table body
        $('#waiterTableBody').html(rows.join(''));
    } else {
        console.log("No waiters found in localStorage.");
    }

    // Show the modal
    $('#waiterModal').modal('show');
});

function editWaiter(waiterId) {
    let row = document.getElementById(`waiter_delete_${waiterId}`);
    let nameBeforeEdit = row.querySelector(".name").textContent.trim();
    let statusBeforeEdit = row.querySelector(".status").textContent.trim();
    let nameCell = row.querySelector(".name");
    let status = row.querySelector(".status");

    nameCell.innerHTML = `<input type="text" value="${nameBeforeEdit}" id=name-${waiterId} class="form-control">`;
    status.innerHTML = `
    <div class="form-group d-flex align-items-left">
    <select id="status-${waiterId}" class="form-control">
    <option value="ACTIVE" ${statusBeforeEdit === "ACTIVE" ? "selected" : ""}>ACTIVE</option>
    <option value="INACTIVE" ${statusBeforeEdit === "INACTIVE" ? "selected" : ""}>INACTIVE</option>
    </select>
    </div>`;
    let actionCell = row.querySelector("td:last-child");
    actionCell.innerHTML =
        `<iconify-icon icon="solar:check-circle-bold" class="btn btn-soft-success btn-sm" onclick="saveWaiter(${waiterId}, '${nameBeforeEdit}', '${statusBeforeEdit}')"></iconify-icon>
        <iconify-icon icon="solar:close-circle-bold" class="btn btn-soft-danger btn-sm" onclick="cancelEdit(${waiterId}, '${nameBeforeEdit}', '${statusBeforeEdit}')"></iconify-icon>`;

}

function cancelEdit(waiterId, originalName, originalStatus) {
    let row = document.getElementById(`waiter_delete_${waiterId}`);
    let nameCell = row.querySelector(".name");
    let status = row.querySelector(".status");
    nameCell.innerHTML = originalName;
    status.innerHTML = originalStatus;
    let actionCell = row.querySelector("td:last-child");
    actionCell.innerHTML = `
    <iconify-icon icon="solar:pen-2-broken" class="btn btn-soft-primary btn-sm" onclick="editWaiter(${waiterId})"></iconify-icon>
`;
}


async function saveWaiter(waiterID, nameBeforeEdit, statusBeforeEdit) {
    let url = `${webmanagerurl}/waiters/updatedetails`;
    let updatedName = document.getElementById(`name-${waiterID}`).value.trim();
    let updatedStatus = document.getElementById(`status-${waiterID}`).value.trim();
    const request = {
        id: waiterID,
        name: updatedName,
        status: updatedStatus,
        nameBeforeEdit: nameBeforeEdit,
        statusBeforeEdit: statusBeforeEdit
    }
    const response = await fetch(url, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getCookie('accessToken')}`
        },
        body: JSON.stringify(request)
    });
    let responseData = await response.json();
    if (responseData.hasOwnProperty('status') && responseData.status === 'error') {
        customToastAlert("table-reservation-alert-container", "Unable to update waiter. Error occurred", "error");
    } else if (responseData.hasOwnProperty('status') && responseData.status === 'success') {
        customToastAlert("table-reservation-alert-container", "Waiter updated successfully", "success");
        let row = document.getElementById(`waiter_delete_${waiterID}`);
        row.querySelector(".name").innerHTML = updatedName;
        row.querySelector(".status").innerHTML = updatedStatus;
        row.querySelector("td:last-child").innerHTML = `
            <iconify-icon icon="solar:pen-2-broken" class="btn btn-soft-primary btn-sm" onclick="editWaiter(${waiterID})"></iconify-icon>
        `;
    }
    getWaiters();
}

async function getWaiters() {
    let url = `${webmanagerurl}/waiters`;
    const response = await getRequest(url);
    if (response?.status === 'success' && response?.data) {
        setlocalStorage('waiters', JSON.stringify(response?.data));
    }
}


// ----------------------------------------- sales view ----------------------------------------------------

async function getSales(params) {
    const menu = getActiveMenu().replace(" ", "").toLowerCase();
    let url = `${webmanagerurl}/table/getsales/${menu}`;
    const response = await getRequest(url);
    if (response?.status === 'success' && response?.data) {
        renderSalesTable(response?.data);
    } else {
        customToastAlert("table-reservation-alert-container1", "Error loading data");
    }

    $(document).on('click', '.view-details', function () {
        let rowDataStr = $(this).data('id');
        let rowData = JSON.parse(decodeURIComponent(rowDataStr));
        let tableObj = accountTableData?.find(item => item?.id === rowData?.tableID);
        const table = $('#view-sales-details').DataTable({
            autoWidth: false,
            data: rowData?.saleDetails,
            searching: false,
            paging: false,
            info: false,
            footer: true,
            destroy: true,
            columns: [
                {
                    title: 'SKU',
                    width: "30%",
                    render: function (data, type, row) {
                        return `${row?.SKU}`;
                    }
                },
                {
                    title: 'Category',
                    width: "30%",
                    render: function (data, type, row) {
                        return `${row?.category}`;
                    }
                },
                {
                    title: 'Quantity',
                    width: "30%",
                    render: function (data, type, row) {
                        return `${row?.quantity}`;
                    }
                },
                {
                    title: 'Amount',
                    width: "30%",
                    render: function (data, type, row) {
                        return `${row?.quantity * row?.salePrice}`;
                    }
                }

            ],
            footerCallback: function (row, data, start, end, display) {
                let api = this.api();
                let total = data.reduce(function (a, b) {
                    let quantity = parseFloat(b?.quantity);
                    let salePrice = parseFloat(b?.salePrice);

                    if (!isNaN(quantity) && !isNaN(salePrice)) {
                        return a + (quantity * salePrice);
                    }
                    return a;
                }, 0);
                $(api.column(3).footer()).html(total);
            },
        });

        $('#viewSalesModal').modal('show');
        $('#viewSalesModalHeader').text(tableObj?.name);

    });

}

$('#view-sales-button').on('click', async function () {
    $('.view-sales').show();
    const menu = getActiveMenu();

    if (menu === 'Table Sales') {
        $('.view-table').hide();
    } else if (menu === 'POS') {
        $('.view-pos').hide();
    }
    getSales();
})

$('#view-tables').on('click', async function () {
    $('.view-sales').hide();
    const menu = getActiveMenu();
    if (menu === 'Table Sales') {
        $('.view-table').show();
    } else if (menu === 'POS') {
        $('.view-pos').show();
    }
})

async function renderSalesTable(data) {
    const table = $('#salesTable').DataTable({
        autoWidth: false,
        data: data,
        searching: false,
        paging: true,
        info: false,
        footer: true,
        destroy: true,
        columns: [
            {
                title: 'Table',
                width: "30%",
                render: function (data, type, row) {
                    let tableObj = accountTableData?.find(item => row?.tableID === item?.id);
                    return `${tableObj?.name || 'NA'} `;
                }
            },
            {
                title: 'Waiter',
                width: "30%",
                render: function (data, type, row) {
                    return `${row?.waiter || 'NA'}`;
                }
            },
            {
                title: 'Bill No',
                width: "20%",
                render: function (data, type, row) {
                    return `${row?.billNumber || 'NA'} `;
                }
            },
            {
                title: 'Status',
                width: "20%",
                render: function (data, type, row) {
                    return `${row?.status}`;
                }
            },
            {
                title: 'Total',
                width: "30%",
                render: function (data, type, row) {
                    let total = row?.saleDetails?.reduce(function (sum, salesDetail) {
                        return sum + (salesDetail?.quantity * salesDetail?.salePrice)
                    }, 0)
                    return `${total}`;
                }
            },
            {
                title: 'Action',
                width: "20%",
                render: function (data, type, row, rowData) {
                    return `
                    <button class="btn btn-info btn-sm view-details" data-id="${encodeURIComponent(JSON.stringify(row))}" data-toggle="modal" data-target="#viewSalesModal">
                        <iconify-icon icon="solar:eye-broken" class="align-middle fs-18"></iconify-icon>
                    </button>
                `;
                }
            }
        ]
    })
    const menu = getActiveMenu();
    if (menu === 'POS') {
        $('#salesTable').DataTable().column(0).visible(false);
    }

}