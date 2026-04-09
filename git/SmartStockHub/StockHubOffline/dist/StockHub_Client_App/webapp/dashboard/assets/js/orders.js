$(document).ready(function () {
    console.log("Product Page is fully loaded!");
    init();
});

// URLs are already defined in http-util.js
// const productMasterurl = ... 
// const salesurl = ...
let expensesurl = `${webmanagerurl}/expenses`;
let memberssurl = `${webmanagerurl}/members`;
var isPettyCashEnabled = false;
var isStoreRoomAvailable = false;
var memberSales;
var dates = [];
var isEditMode = false;
var currentEditOrderId = null;
async function init() {
    dates = [];
    updateShopName();
    const response = await getRequest(salesurl);
    // const response =JSON.parse(``);
    var data = constructSalesList(response);
    const container = document.getElementById("sales-table");

    let userRole = "";
    try {
        const storedUser = localStorage.getItem('user');
        if (storedUser) userRole = JSON.parse(storedUser).role;
    } catch (e) { console.error(e); }

    container.innerHTML = "";
    if (document.getElementById("sales-table"))
        new gridjs.Grid({
            sort: true,
            pagination: {
                limit: 30
            },
            fixedHeader: true,
            height: '550px',
            search: true,
            data: data.sort((a, b) => b[0] - a[0]),
            columns: [
                {
                    name: "Date",
                    sort: true,
                    formatter: (cell, row) => {
                        const date = new Date(cell * 1000);
                        const formattedDate = date.toLocaleDateString();
                        dates.push(formattedDate);
                        if (row.cells[0].data === data[0][0] && userRole === "OWNER") {
                            return gridjs.html(`${formattedDate}<a href="#!" onclick="editOrder('${row.cells[5].data}')" class="btn btn-soft-primary btn-sm" data-bs-toggle="modal" data-bs-target="#exampleModalXl-new"><iconify-icon icon="solar:pen-2-broken" class="align-middle fs-18"></iconify-icon></a>`);
                        }
                        return gridjs.html(`${formattedDate}`);
                    }
                },
                {
                    name: "Sales Amount",
                    formatter: (cell, row) => gridjs.html(`<span id="saleAmount-tab-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "Expenses",
                    formatter: (cell, row) => gridjs.html(`<span id="expenses-tab-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "Digital Payment",
                    formatter: (cell, row) => gridjs.html(`<span id="digitab-tab-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "Final Settlement",
                    formatter: (cell, row) => gridjs.html(`<span id="final-tab-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "view",
                    formatter: (cell, row) => gridjs.html(`<div class="d-flex gap-2"><a href="#!" onclick=loadSalesDetails("${cell}") class="btn btn-light btn-sm" data-bs-toggle="modal" data-bs-target="#exampleModalXl"><iconify-icon icon="solar:eye-broken" class="align-middle fs-18"></iconify-icon></a><a href="#!" onclick=printDailySalesSummary("${row.cells[5].data}") class="btn btn-light btn-sm" data-bs-toggle="modal" data-bs-target="#dailySaleSummary"><iconify-icon icon="solar:inbox-line-broken" class="align-middle fs-18"></iconify-icon></a></div>`)
                },
                {
                    name: "Members",
                    formatter: (cell, row) => gridjs.html(
                        `<a href="#!" onclick=loadMembersSales("${row.cells[6].data}") 
                        class="btn btn-light btn-sm" 
                        data-bs-toggle="modal" 
                        data-bs-target="#memberSales">
                        <iconify-icon icon="solar:users-group-two-rounded-bold-duotone" 
                            class="align-middle fs-18"></iconify-icon>
                    </a>`
                    )
                },
                {
                    name: "Ledger",
                    formatter: (cell, row) => gridjs.html(`<a href="#!" onclick=printLedger("${row.cells[5].data}") class="btn btn-light btn-sm"><iconify-icon icon="solar:clipboard-text-bold-duotone" class="align-middle fs-18"></iconify-icon></a><a href="#!" onclick=printDailyReport("${row.cells[5].data}") class="btn btn-light btn-sm"><iconify-icon icon="solar:bill-list-bold-duotone" class="align-middle fs-18"></iconify-icon></a></div>`)
                }],
        }).render(document.getElementById("sales-table"));
}

function constructSalesList(response) {
    var tableList = [];
    for (var i in response.data) {
        var item = response.data[i];
        var tableRow = [];
        tableRow.push(item.timeCreatedAt);
        tableRow.push(item.totalSalesAmount);
        tableRow.push(item.totalExpensesAmount);
        tableRow.push(item.totalDigitalAmount);
        tableRow.push(item.finalCashSettlement);
        // Create a robust ID check for both Online (MongoDB style) and Offline (Dexie style)
        var id = item._id;
        if (id && typeof id === 'object' && id.$oid) {
            id = id.$oid;
        }
        tableRow.push(id);
        tableRow.push(item.invoiceNumber);
        //    tableRow.push(item.startingBillNumber ? item.startingBillNumber : 0); 
        tableList.push(tableRow);
    }
    return tableList;
}

async function loadSalesDetails(id) {
    const response = await getRequest(salesurl + "/id?id=" + id);
    //    const response = JSON.parse(``);
    var salesData = response.data.productList.map(item => [
        item.SKU,
        item.openingStock,
        item.purchaseStock,
        item.stock,
        item.sales,
        item.closingStock,
        item.salePrice,
        item.totalSaleAmount
    ]);

    // Clear and rebuild sales table
    $("#sales-details-table").empty();
    $("#sales-details-table").DataTable({
        data: salesData,
        columns: [
            { title: "SKU" },
            { title: "Opening Stock" },
            { title: "Purchase Stock" },
            { title: "Stock" },
            { title: "Sales" },
            { title: "Closing Stock" },
            { title: "Sale Price" },
            { title: "Total Sale Amount" }
        ],
        order: [],
        pageLength: 100,
        destroy: true,
        dom: 'Bfrtip',
        buttons: ['print', 'pdfHtml5', 'excelHtml5'],
    });

    // Prepare expense data
    if (response.data.expenseList) {
        var expenseData = response.data.expenseList.map(item => [
            item.details,
            item.amount
        ]);

        // Clear and rebuild expenses table
        $("#sales-expenses-table").empty();
        $("#sales-expenses-table").DataTable({
            data: expenseData,
            columns: [
                { title: "Details" },
                { title: "Amount" }
            ],
            destroy: true,
            dom: 'Bfrtip',
            buttons: ['print']
        });
    }

    if (response.data && response.data.openingPettyCash !== undefined || response.data.diffSettlement !== undefined) {
        if (response.data.openingPettyCash !== undefined && response.data.openingPettyCash !== null) {
            $('#summary-opening-petty-cash').text(response.data.openingPettyCash);
        } else {
            $('#summary-opening-petty-cash').hide(); // Hide the element if data is missing
        }
        $('#summary-final-expenses-new').text(response.data.totalExpensesAmount);
        $('#summary-digital-payment-new').text(response.data.totalDigitalAmount);
        $('#summary-kitchen-sales').text(response.data.kitchenSales);
        $('#summary-cash-in-hand').text(response.data.cashInHand);
        if (response.data.closingPettyCash !== undefined && response.data.closingPettyCash !== null) {
            $('#summary-closing-petty-cash').text(response.data.closingPettyCash);
        } else {
            $('#summary-closing-petty-cash').hide();
            $('.pettyCase-display').hide();
        }
        if (response.data.openingPettyCash) {
            $('#summary-actual-liquor-sale').text(response.data.totalSalesAmount);
        } else {
            $('#summary-actual-liquor-sale').text(response.data.finalCashSettlement);
        }
        if (response.data.diffSettlement !== undefined && response.data.diffSettlement !== null) {
            $('#difference-liquor-sale').text(response.data.diffSettlement);
        } else {
            $('#difference-liquor-sale').hide();
        }
        if (response.data.acCharges !== undefined && response.data.acCharges !== null) {
            $('#summary-AC-Charges').text(response.data.acCharges);
        }
        $("#sales-summary-details").show();
    } else {
        $("#sales-summary-details").hide();
    }

}

async function loadProductTable(editData = null) {
    var data = [];
    var afterPurchaseHasSale = true;
    var hasDraft = false;
    var draftProductList = [];
    var draftExpensesList = [];

    if (editData) {
        isEditMode = true;
        currentEditOrderId = editData.id;
        hasDraft = true;
        draftProductList = editData.productList;
        draftExpensesList = editData.expenseList;
        afterPurchaseHasSale = false;

        for (var i = 0; i < editData.productList.length; i++) {
            var item = editData.productList[i];
            var tableRow = [];
            tableRow.push(item.SKU);
            tableRow.push(item.openingStock);
            tableRow.push(item.purchaseStock);
            tableRow.push(item.stock);
            tableRow.push(i);
            tableRow.push(i);
            tableRow.push(item.salePrice);
            data.push(tableRow);
        }

        if (editData.openingPettyCash !== undefined) {
            $("#pettyCase-summary").show();
            $("#opening-petty-cash").html(editData.openingPettyCash);
            isPettyCashEnabled = true;
        } else {
            $("#non-pettyCase-summary").show();
            $("#sales-denomination").hide();
            isPettyCashEnabled = false;
        }

        // Wait for modal to be ready or just populate
        setTimeout(() => populateEditSalesFields(editData), 100);

    } else {
        isEditMode = false;
        currentEditOrderId = null;

        const responseWrapper = await getRequest(productMasterurl);
        // const response = JSON.parse(``);
        const response = responseWrapper.data;
        data = await constructList(response);

        if (response.afterPurchaseHasSale != null) {
            afterPurchaseHasSale = response.afterPurchaseHasSale;
        }
        if (isStoreRoomAvailable) {
            afterPurchaseHasSale = false;
        }

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

        if ("hasDraftSale" in response && response.hasDraftSale) {
            const draftResponse = await getRequest(salesurl + "/draft");
            //const draftResponse = JSON.parse(``);
            if (draftResponse && draftResponse.data && draftResponse.data.productList && draftResponse.data.productList.length > 0) {
                hasDraft = true;
                draftProductList = draftResponse.data.productList;
                draftExpensesList = draftResponse.data.expenseList;
            }
        }
    }

    const container = document.getElementById("sales-entry-table");
    container.innerHTML = "";
    if (container)
        new gridjs.Grid({
            sort: true,
            pagination: {
                limit: 30
            },
            fixedHeader: true,
            height: '550px',
            search: true,
            data: data,
            columns: [
                {
                    name: "SKU",
                    formatter: (cell, row) => gridjs.html(`<span id="sellerSKU-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
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
                        return gridjs.html(`<span id="opening-stock-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cellValue}</span>`)
                    }

                },
                {
                    name: "Purchase Stock",
                    formatter: (cell, row) => {
                        const stock = parseFloat(row.cells[3].data);
                        const purchaseStock = parseFloat(cell);
                        const openingStock = parseFloat(row.cells[1].data);
                        let cellValue = 0;
                        if (stock == (openingStock + purchaseStock)) {
                            cellValue = purchaseStock;
                        }
                        if (!afterPurchaseHasSale) {
                            cellValue = purchaseStock;
                        }

                        return gridjs.html(`<span id="purchase-stock-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cellValue}</span>`)
                    }
                },
                {
                    name: "Total Stock",
                    formatter: (cell, row) => gridjs.html(`<span id="stock-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "Closing Stock",
                    formatter: (cell, row) => {
                        if (hasDraft) {
                            var item = getDraftItem(row.cells[0].data, draftProductList);
                            if (item != null) {
                                return gridjs.html(`<div class="col-md-8"><input type="number" id="closing-stock-${row.cells[4].data}" class="form-control ram-sales" placeholder="00" value="${item.closingStock}"></div>`);
                            }
                        }
                        return gridjs.html(`<div class="col-md-8"><input type="number" id="closing-stock-${row.cells[4].data}" class="form-control ram-sales" placeholder="00"></div>`);
                    }
                },
                {
                    name: "Sales",
                    formatter: (cell, row) => {
                        if (hasDraft) {
                            var item = getDraftItem(row.cells[0].data, draftProductList);
                            if (item != null) {
                                return gridjs.html(`<span id="sales-${row.cells[4].data}" class="text-dark fw-medium mt-3">${item.sales}</span>`);
                            }
                        }
                        return gridjs.html(`<span id="sales-${row.cells[4].data}" class="text-dark fw-medium mt-3">0</span>`);
                    }
                },
                {
                    name: "Sale Price",
                    formatter: (cell, row) => gridjs.html(`<span id="sale-price-${row.cells[4].data}" class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "Total Sale Amount",
                    formatter: (cell, row) => {
                        if (hasDraft) {
                            var item = getDraftItem(row.cells[0].data, draftProductList);
                            if (item != null) {
                                return gridjs.html(`<span id="saleAmount-${row.cells[4].data}" class="text-dark fw-medium mt-3 ram-saleAmount">${item.totalSaleAmount}</span>`);
                            }
                        }
                        return gridjs.html(`<span id="saleAmount-${row.cells[4].data}" class="text-dark fw-medium mt-3 ram-saleAmount">0</span>`);
                    }
                }],
        }).render(document.getElementById("sales-entry-table"));
    loadExpenses(draftExpensesList);

    if (hasKeyInLocalStorage('isACEnabled')) {
        $("#sales-ac-charges").show();
        $("#summary-AC-Charges").show();
    } else {
        $("#sales-ac-charges").hide();
        $("#summary-AC-Charges").hide();
    }
    if (hasKeyInLocalStorage('isSalesSettlementEnabled')) {
        $("#finalSettlement-sec").show();
    }
    if (hasKeyInLocalStorage('isBankBlanceEnabled')) {
        $("#sales-money-balances").show();
    }
    if (hasKeyInLocalStorage('isOtherIncomeEnabled')) {
        $("#other_income").show();
        $("#summary-other-income").show();
    }
    if (hasKeyInLocalStorage('salesUnitEnabled')) {
        $("#sales-Unit-btn").show();
    }
    if (isAccountPropertyEnabled('isACTransferEnabled')) {
        $("#ac-sales-breakdown").show();
    } else {
        $("#ac-sales-breakdown").hide();
    }

}

$(document).on('keydown', '.ram-sales', function (e) {
    if (e.key === 'Enter') {
        e.preventDefault(); // stop form submit or default behavior

        const inputs = $('.ram-sales'); // all input fields
        const index = inputs.index(this); // current position
        const nextInput = inputs.eq(index + 1); // next input

        if (nextInput.length) {
            nextInput.focus().select();
        }
    }
});

function hasKeyInLocalStorage(keyToCheck) {
    const storedData = localStorage.getItem('account');
    if (!storedData) return false;

    try {
        const parsedData = JSON.parse(storedData);
        return parsedData.hasOwnProperty(keyToCheck);
    } catch (e) {
        console.error("Error parsing JSON from localStorage:", e);
        return false;
    }
}

function isAccountPropertyEnabled(keyToCheck) {
    const storedData = localStorage.getItem('account');
    if (!storedData) return false;

    try {
        const parsedData = JSON.parse(storedData);
        return parsedData[keyToCheck] === true || parsedData[keyToCheck] === 'true';
    } catch (e) {
        return false;
    }
}


async function constructList(response) {
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
        if (accountInfo && accountInfo.hasOwnProperty('isCustomOrderEnabled')) {
            sortedProducts = sortByCustomOrder(response.productList);
        }
        else if (accountInfo && accountInfo.hasOwnProperty('isRumFirstEnabled')) {
            sortedProducts = sortByBrand(response.productList, true);
        } else {
            sortedProducts = sortByBrand(response.productList, false);
        }
    }
    for (var i in sortedProducts) {
        var item = sortedProducts[i];
        if (item.stock != 0) {
            var tableRow = [];
            tableRow.push(item.SKU);
            tableRow.push(item.openingStock);
            tableRow.push(item.purchaseStock);
            tableRow.push(item.stock);
            tableRow.push(i);
            tableRow.push(i);
            tableRow.push(item.salePrice);
            tableList.push(tableRow);
        }
    }
    return tableList;
}

function getDraftItem(SKU, draftProductList) {
    if (!draftProductList) return null;
    var searchSKU = String(SKU).trim();
    for (var i in draftProductList) {
        var item = draftProductList[i];
        var itemSKU = item.SKU || item.sku;
        if (itemSKU && String(itemSKU).trim() === searchSKU) {
            return item;
        }
    }
    return null;
}

async function loadExpenses(draftExpensesList) {
    const response = await getRequest(expensesurl);
    //  const response = JSON.parse(``);
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

    if (draftExpensesList.length > 0) {
        var freeTextIndex = 8;
        $(".sale-exp-input").each(function (index) {
            if (index < draftExpensesList.length) {
                var expenses = draftExpensesList[index];
                var $dropdown = $(this).find(".ram-exp-name");
                var $amountField = $(this).find(".ram-exp");
                var $freeTextInput = $(this).find(".ram-exp-free-text");

                let optionExists = $dropdown.find(`option`).filter(function () {
                    return $(this).text().trim().toLowerCase() === expenses.details.toLowerCase();
                }).length > 0;

                if (optionExists) {
                    $dropdown.val(expenses.details);
                    $amountField.val(expenses.amount);
                    if (expenses.narration) {
                        $("#exp-naration-" + (index + 1)).val(expenses.narration);
                    }
                } else {
                    $("#" + freeTextIndex).val(expenses.details);
                    $("#exp-amount-" + freeTextIndex).val(expenses.amount);
                    freeTextIndex++;
                }


            }
        });
    }

    //Load Date drop Down
    const $Salesdropdown = $('#salesDate');
    const today = new Date();

    for (let i = 0; i < 30; i++) {
        const date = new Date();
        date.setDate(today.getDate() - i);
        const formattedDate = date.toISOString().split('T')[0]; // Format as YYYY-MM-DD
        $Salesdropdown.append(`<option value="${formattedDate}">${formattedDate}</option>`);
    }

}


$(document).ready(function () {
    // Function to handle blur event on any text box
    function onSalesProcess() {
        const id = $(this).attr('id');
        const number = id.replace(/\D/g, '');
        var salePrice = parseFloat($('#sale-price-' + number).text());
        var sales = updateSales(number);
        if (sales < 0) {
            alert("Warning: Sales cannot be negative!");
        } else {
            $("#saleAmount-" + number).text(sales * salePrice);
            udpateTotalSaleAmount();
        }
    }

    $(document).on('blur', '.ram-sales', onSalesProcess);
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
        let acSalePriceSum = 0;
        let nonAcSalePriceSum = 0;

        $('.ram-saleAmount').each(function () {
            const id = $(this).attr('id');
            const index = id.replace(/\D/g, '');
            const price = parseFloat($(this).text()) || 0;
            const sku = $("#sellerSKU-" + index).text().trim();

            if (sku.startsWith("AC ")) {
                acSalePriceSum += price;
            } else {
                nonAcSalePriceSum += price;
            }

            salePriceSum += price;
        });
        $("#salePriceSum").text(salePriceSum.toFixed(2));
        $("#acSalePriceSum").text(acSalePriceSum.toFixed(2));
        $("#nonAcSalePriceSum").text(nonAcSalePriceSum.toFixed(2));

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
    $(document).on('input', 'input.ram-payment-50', function () {
        onPaymentProcess($(this).val(), 50);
    });
    $(document).on('input', '#payment-kitchenSales', function () {
        $('#kitchen-sales').text($(this).val());
        $('#final-kitchen-sales').text($(this).val());
    });
    $(document).on('input', '#payment-otherIncome', function () {
        $('#final-other-income').text($(this).val());
    });
    $(document).on('input', '#payment-ac-charges', function () {
        $('#final-ac-charges').text($(this).val());
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
        const finalACCharges = parseFloat($('#final-ac-charges').text()) || 0;
        if (finalACCharges !== 0) {
            diffLiquorSale -= finalACCharges;
        }
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

function calculateAllUnits() {

    let totalUnit = 0;

    // find all items that match id="sales-*"
    $("[id^='sales-']").each(function () {

        // extract index → "sales-12" -> "12"
        let id = $(this).attr("id");
        let index = id.split("-")[1];

        // get unit for this row
        let rowUnit = getUnit(index);

        totalUnit += rowUnit;
    });

    alert("Final Total Units = " + totalUnit.toFixed(2));
}

function getUnit(index) {

    // 1. Get SKU
    let SKU = $("#sellerSKU-" + index).text().trim();

    // 2. Extract pack size
    let packPart = SKU.split("-")[1] || "";
    let packSize = parseInt(packPart.replace("ML", "").trim());

    // 3. Get sales quantity from SPAN
    let salesQty = parseFloat($("#sales-" + index).text()) || 0;

    // 4. Compute total ML
    let totalML = packSize * salesQty;

    // 5. Get product info from localStorage
    const storedProducts = localStorage.getItem("products");
    const productInfo = storedProducts ? JSON.parse(storedProducts) : null;

    let brand = "";

    if (productInfo && productInfo.productList) {
        for (const item of productInfo.productList) {
            if (item.SKU === SKU) {
                brand = item.brand.toUpperCase();
                break;
            }
        }
    }

    // 6. Compute unit
    let unit = 0;
    const spiritBrands = ["BRANDY", "RUM", "VODKA", "WHISKY"];

    if (spiritBrands.includes(brand)) {
        unit = totalML / 750;
    } else if (brand === "BEER") {
        unit = totalML / 7800;
    } else if (brand === "WINE") {
        unit = totalML / 2250;
    }

    return unit;
}

function saveSales() {
    $('#saleSaveButton').prop('disabled', true).html(`<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>`);
    var productList = constructProductList();
    var expenseList = constructExpenses();
    var payments = constructPayments();
    var denomination = constructDenomination();
    var sales = {};
    sales.productList = productList;
    sales.expenseList = expenseList;
    sales.payments = payments;
    if (Object.keys(denomination).length > 0) {
        sales.denomination = denomination;
    }
    sales.totalSalesAmount = parseFloat($("#final-sales").text());
    sales.totalExpensesAmount = parseFloat($("#final-expenses").text());
    sales.totalDigitalAmount = parseFloat($("#final-digital-payment").text());
    sales.finalCashSettlement = parseFloat($("#final-settlement").text());
    const selectedDate = $("#salesDate").val();
    const unixTime = Math.floor(new Date(selectedDate).getTime() / 1000);
    sales.saleDate = unixTime;
    if (isPettyCashEnabled) {
        sales.openingPettyCash = parseFloat($("#opening-petty-cash").text());
        sales.totalExpensesAmount = parseFloat($("#final-expenses-new").text());
        sales.totalDigitalAmount = parseFloat($("#digital-payment-new").text());
        sales.kitchenSales = parseFloat($("#kitchen-sales").text());
        sales.cashInHand = parseFloat($("#cash-in-hand").text());
        sales.closingPettyCash = parseFloat($("#closing-petty-cash").val());
        sales.diffSettlement = parseFloat($("#diff-liquor-sale").text());
        const finalACCharges = parseFloat($('#final-ac-charges').text()) || 0;
        if (finalACCharges !== 0) {
            sales.acCharges = finalACCharges;
        }
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
    if (hasKeyInLocalStorage('isBankBlanceEnabled')) {
        sales.bankBalance = parseInt($("#amount-bank-balance").val(), 10);
        sales.cashInHand = parseInt($("#amount-cashinhand").val(), 10);
        sales.tasmacBalance = parseInt($("#amount-tasmac-balance").val(), 10);
    }
    sales.isStoreRoomAvailable = isStoreRoomAvailable;

    if (isEditMode && currentEditOrderId) {
        sales.id = currentEditOrderId;
        putRequestWithCallback(salesurl, sales, saveSalesCallback, null);
    } else {
        postRequestWithCallback(salesurl, sales, saveSalesCallback, null);
    }

}

function saveSalesCallback() {
    $('#saleSaveButton').html(`<span class="badge bg-primary me-1">Updated</span>`);
    $('#saleSaveButton').prop('disabled', true);
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

function saveAsDraft() {
    var productList = constructProductList();
    productList = productList.filter(product => !isNaN(product.closingStock));
    if (productList.length === 0) {
        alert("Nothing to Save in Draft");
        return;
    }
    var expenseList = constructExpenses();
    var sales = {};
    sales.productList = productList;
    sales.expenseList = expenseList;
    postRequestWithCallback(salesurl + "/draft", sales, saveSalesDraftCallback, null);
}

function saveSalesDraftCallback() {
    $('#saleDraftButton').html(`<span class="badge bg-primary me-1">Draft Added</span>`);
    $('#saleDraftButton').prop('disabled', true);
}

function enableButton() {
    const validationResponse = validateItemsClosingStock();
    if (validationResponse !== true && validationResponse !== "true") {
        alert(validationResponse);
        return;
    }

    // ✅ Enable save button by default
    $('#saleSaveButton').prop('disabled', false);

    try {
        // Check cashFlow key
        if (hasKeyInLocalStorage("cashFlow")) {
            const storedUser = localStorage.getItem('user');

            if (storedUser) {
                const user = JSON.parse(storedUser);

                // Disable button if role is SALESMAN
                if (user.role === "SALESMAN") {
                    $('#saleSaveButton').prop('disabled', true);
                }
            }
        }
    } catch (e) {
        console.error("Error parsing JSON from localStorage:", e);
    }

    // Disable iAMGdbtn
    $("#iAMGdbtn")
        .addClass("disabled")
        .css({
            "pointer-events": "none",
            "opacity": "0.6"
        })
        .off("click");
}

function validateItemsClosingStock() {
    var returnValue = "true";
    const selectedDate = $('#salesDate').val();
    if (dates.includes(selectedDate)) {
        returnValue = "The selected sales date already has an entry. Please verify your date.";
        return false;
    }
    $('.ram-saleAmount').each(function () {
        const id = $(this).attr('id');
        const index = id.replace(/\D/g, '');

        var SKU = $("#sellerSKU-" + index).text();
        var closingStockValue = $("#closing-stock-" + index).val().trim(); // Get the value and remove extra spaces

        if (closingStockValue === "") {
            returnValue = "SKU: " + SKU + " has no Closing Stock. Please add it before proceeding.";
            return false;
        }
        // Convert to number
        var closingStock = Number(closingStockValue);

        // Invalid number check
        if (isNaN(closingStock)) {
            returnValue = "SKU: " + SKU + " has an invalid number. Please check.";
            return false;
        }

        // 🔥 Negative value check
        if (closingStock < 0) {
            returnValue = "SKU: " + SKU + " has negative Closing Stock. Negative values are not allowed.";
            return false;
        }
        var sales = Number($("#sales-" + index).text());
        if (sales < 0) {
            returnValue = "SKU: " + SKU + " has negative Sales Stock. Negative values are not allowed.";
            return false;
        }

    });
    return returnValue;
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

function constructDenomination() {
    var denomination = {};

    var d500 = parseInt($('.ram-payment-500').val()) || 0;
    if (d500 > 0) denomination["500"] = d500;

    var d200 = parseInt($('.ram-payment-200').val()) || 0;
    if (d200 > 0) denomination["200"] = d200;

    var d100 = parseInt($('.ram-payment-100').val()) || 0;
    if (d100 > 0) denomination["100"] = d100;

    var d50 = parseInt($('.ram-payment-50').val()) || 0;
    if (d50 > 0) denomination["50"] = d50;

    return denomination;
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

async function loadMembersSales(invoiceNumber) {
    const response = await getRequest(memberssurl + "/sales?invoiceNumber=" + invoiceNumber);
    //  const response = JSON.parse(``);
    memberSales = response;
    console.log("Member Sales:", memberSales);
    let data = response?.data?.[0]?.saleDetails || [];


    // Completely randomize the order
    for (let i = data.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [data[i], data[j]] = [data[j], data[i]];
    }
    if (memberSales?.data?.[0]) {
        memberSales.data[0].saleDetails = data;
    }
    addDummyEntryExitTimes(data);
    if ($.fn.DataTable.isDataTable('#member-sales-details-table')) {
        $('#member-sales-details-table').DataTable().clear().destroy();
        $('#member-sales-details-table').empty();
    }

    var currentDate = new Date(response.data[0].date * 1000);
    currentDate = `${currentDate.getDate().toString().padStart(2, '0')}/${(currentDate.getMonth() + 1).toString().padStart(2, '0')}/${currentDate.getFullYear()}`;

    // Render DataTable
    const table = $('#member-sales-details-table').DataTable({
        data: data,
        autoWidth: false,
        pageLength: 50,
        order: [],
        columns: [
            { title: 'Member ID', data: 'id' },
            { title: 'Name', data: 'name' },
            {
                title: 'Items',
                data: 'purchasedItems',
                render: function (data, type, row) {
                    if (Array.isArray(data) && data?.length > 0) {
                        return data.map(item => `${item?.SKU} : ${item?.qty}`).join('<br>');
                    }
                    return "";
                }
            },
            {
                title: 'ML',
                data: 'purchasedItems',
                render: function (data, type, row) {
                    if (Array.isArray(data) && data.length > 0) {
                        let totalML = data.reduce((sum, item) => {
                            let match = item?.SKU?.match(/(\d+)[ ]?ML/i); // Extract volume from SKU
                            let volume = match ? parseInt(match[1]) : 0; // Get the numeric value
                            let qty = item?.qty || 0;
                            return sum + (volume * qty); // Accumulate total ML
                        }, 0);

                        return `${totalML} ML`;
                    }
                    return "";
                }
            },
            { // 🆕 Entry/Exit Time Column
                title: 'Entry / Exit Time',
                render: function (data, type, row) {
                    return `<b>${row.entryTime}</b> - ${row.exitTime}`;
                }
            }
        ],
        dom: 'Bft<"bottom d-flex justify-content-between"lpi>',
        language: {
            lengthMenu: "Show  &nbsp; _MENU_  &nbsp; entries",
        },
        buttons: [
            { extend: 'excelHtml5', exportOptions: { columns: ':visible' } },
            { extend: 'pdfHtml5', exportOptions: { columns: ':visible' } },
            {
                extend: 'print',
                exportOptions: { columns: ':visible' },
                customize: function (win) {
                    $(win.document.body).prepend(`
                        <div style="position: absolute; top: 10px; right: 10px; font-size: 14px;">
                             <b>${currentDate}</b>
                        </div>
                    `);
                }
            }
        ]
    });

    constructPermitPassData(response);
}

function constructPermitPassData(response) {
    var list = [];
    var date = new Date(response.data[0].date * 1000);
    date = date.toDateString();
    var slNO = 1;
    for (var i in response.data[0].saleDetails) {
        var obj = response.data[0].saleDetails[i];
        var data = {
            No: slNO++,
            Date: date,
            Guest_of: obj.name,
            No_of_Persons: obj.guests
        };
        list.push(data);
    }
    jsonList = list;
}

async function printDailySalesSummary(invoiceID) {
    const response = await getRequest(salesurl + "/id?id=" + invoiceID);
    //   const response = JSON.parse(``);

    var salesDataSummary = response.data.productList.map(item => [
        item.SKU,
        item.openingStock,
        item.purchaseStock,
        item.sales,
        item.closingStock
    ]);

    $("#daily-sales-summary").empty();
    $("#daily-sales-summary").DataTable({
        data: salesDataSummary,
        columns: [
            { title: "Item" },
            { title: "Opening" },
            { title: "Purchase" },
            { title: "Sales" },
            { title: "Closing" }
        ],
        destroy: true,
        pageLength: 100,
        dom: 'Bfrtip',
        buttons: ['print'],
        autoWidth: false
    });
}


async function loadIndividualSales() {
    const response = await getRequest(productMasterurl);
    var products = response?.productList | [];
}

var firstOrderID;
async function loadorderUpdate(orderID) {
    firstOrderID = orderID;
    const $Salesdropdown = $('#updateSalesDate');
    const today = new Date();
    for (let i = 0; i < 10; i++) {
        const date = new Date();
        date.setDate(today.getDate() - i);
        const formattedDate = date.toISOString().split('T')[0]; // Format as YYYY-MM-DD
        $Salesdropdown.append(`<option value="${formattedDate}">${formattedDate}</option>`);
    }
}

async function editOrder(id) {
    const response = await getRequest(salesurl + "/id?id=" + id);
    if (response && response.data) {
        if (!response.data.id) response.data.id = id;
        await loadProductTable(response.data);
    }
}

async function updateSaleDate() {
    console.log(firstOrderID);
    var request = {};
    request.id = firstOrderID;
    const selectedDate = $("#updateSalesDate").val();
    const unixTime = Math.floor(new Date(selectedDate).getTime() / 1000);
    request.date = unixTime;
    console.log(request);
    putRequestWithCallback(salesurl, request, updateDateCallback, null);

}

function populateEditSalesFields(editData) {
    if (editData.payments) {
        if (editData.payments.card) $("#payment-card").val(editData.payments.card).trigger('input');
        if (editData.payments.gpay) $("#payment-gpay").val(editData.payments.gpay).trigger('input');
        if (editData.payments.netbanking) $("#payment-netbanking").val(editData.payments.netbanking).trigger('input');
    }

    if (editData.kitchenSales) {
        $("#payment-kitchenSales").val(editData.kitchenSales).trigger('input');
    }

    if (editData.cashInHand) {
        $("#final-cash-in-hand").val(editData.cashInHand).trigger('input');
        $("#cash-in-hand").text(editData.cashInHand);
    }
    if (editData.bankBalance) {
        $("#amount-bank-balance").val(editData.bankBalance);
    }

    if (editData.closingPettyCash) {
        $("#closing-petty-cash").val(editData.closingPettyCash).trigger('input');
    }

    if (editData.acCharges) {
        $("#payment-ac-charges").val(editData.acCharges).trigger('input');
    }

    if (editData.otherIncome) {
        $("#payment-otherIncome").val(editData.otherIncome).trigger('input');
        if (editData.otherIncomeNaretion) $("#payment-otherIncome-naration").val(editData.otherIncomeNaretion);
    }
    if (editData.tasmacBalance) {
        $("#amount-tasmac-balance").val(editData.tasmacBalance);
    }

    // Recalculate Totals
    if (typeof udpateTotalSaleAmount === 'function') {
        udpateTotalSaleAmount();
    }
}

function updateDateCallback() {
    $('#updateSaleDateButton').html(`<span class="badge bg-primary me-1">Updated</span>`);
    $('#updateSaleDateButton').prop('disabled', true);
}


function addDummyEntryExitTimes(bills) {
    const startHour = 11; // 11:00 AM
    const endHour = 22;   // up to 10:30 PM
    const totalMinutes = (endHour - startHour) * 60 + 30; // 690 minutes
    const gap = Math.floor(totalMinutes / bills.length); // evenly spaced entry times

    const baseDate = new Date();
    baseDate.setSeconds(0);
    baseDate.setMilliseconds(0);

    for (let i = 0; i < bills.length; i++) {
        const bill = bills[i];

        // Calculate entry time incrementally
        const minutesFromStart = gap * i;
        const entryHour = startHour + Math.floor(minutesFromStart / 60);
        const entryMinute = minutesFromStart % 60;

        const entryDate = new Date(baseDate);
        entryDate.setHours(entryHour, entryMinute, 0, 0);

        // Exit 30–120 minutes later
        const exitOffset = Math.floor(Math.random() * 91) + 30;
        const exitDate = new Date(entryDate.getTime() + exitOffset * 60000);

        // Cap exit at 10:30 PM
        if (exitDate.getHours() > 22 || (exitDate.getHours() === 22 && exitDate.getMinutes() > 30)) {
            exitDate.setHours(22, 30, 0, 0);
        }

        bill.entryTime = entryDate.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
        bill.exitTime = exitDate.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
    }
}


