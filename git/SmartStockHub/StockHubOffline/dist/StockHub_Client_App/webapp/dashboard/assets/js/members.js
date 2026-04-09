// membersurl is declared in http-util.js
let productsURL = `${webmanagerurl}/productmaster`;

$(document).ready(function () {
    console.log("Members Page is fully loaded!");
    init();
});

async function init() {
    updateShopName();
    const response = await getRequest(membersurl);
    //	 const response = JSON.parse(``);
    var data = constructMemebrsList(response);
    const container = document.getElementById("membersTable");
    container.innerHTML = "";
    if (document.getElementById("membersTable"))
        new gridjs.Grid({
            sort: true,
            pagination: true,
            fixedHeader: true,
            height: '550px',
            search: true,
            data: data,
            columns: [
                {
                    name: "Member ID",
                    formatter: (cell, row) => gridjs.html(`<span class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "Name",
                    formatter: (cell, row) => gridjs.html(`<span class="text-dark fw-medium mt-3">${cell}</span>`)
                },
                {
                    name: "view",
                    formatter: (cell, row) => gridjs.html(`<div class="d-flex gap-2"><a href="#!" onclick="assignMemberID('${row.cells[0].data}', '${row.cells[1].data}')" class="btn btn-light btn-sm" data-bs-toggle="modal" data-bs-target="#memberMonthlySales"><iconify-icon icon="solar:eye-broken" class="align-middle fs-18"></iconify-icon></a></div>`)
                }],
        }).render(document.getElementById("membersTable"));
}

function constructMemebrsList(response) {
    var tableList = [];
    for (var i in response.data) {
        var item = response.data[i];
        var tableRow = [];
        tableRow.push(item.id);
        tableRow.push(item.name);
        tableList.push(tableRow);
    }
    return tableList;
}

function saveMember() {
    $('#mem-saveButton').html(`<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>`);
    var member = {};
    member.name = $("#mem-name").val();
    member.id = $("#mem-id").val();
    postRequestWithCallback(membersurl, member, saveMemberCallback, null);
}

function saveMemberCallback() {
    $('#mem-saveButton').html(`<span class="badge bg-primary me-1">Updated</span>`);
    $('#mem-saveButton').prop('disabled', true);
    setTimeout(function () {
        location.reload();
    }, 1000);
}

var memberID;
var memberName;
function assignMemberID(selectedMemberID, selectedMemberName) {
    memberID = selectedMemberID;
    memberName = selectedMemberName;
}

async function loadMemberIndividualReport() {
    var startDate = $("#member-startDate").val();
    if (!startDate) {
        alert("Please Select Start Date");
        return;
    }
    var endDate = $("#member-endDate").val();
    if (!endDate) {
        alert("Please Select End Date");
        return;
    }
    var startDatetimestamp = (new Date(startDate).getTime()) / 1000;
    var endDatetimestamp = (new Date(endDate).getTime()) / 1000;
    const response = await getRequest(membersurl + "/sales?memberID=" + memberID + "&fromTime=" + startDatetimestamp + "&toTime=" + endDatetimestamp);
    //  const response = JSON.parse(``);
    constructMemberReport(response);
}

function constructMemberReport(response, memberId) {
    response.data.sort((a, b) => a.date - b.date);
    const storedAccount = localStorage.getItem('account');
    const accountInfo = storedAccount ? JSON.parse(storedAccount) : null;
    var template = '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>' + memberName + '</title><style>' +
        'body {font-family: Arial, sans-serif;margin: 0;padding: 0;background-color: #fff;}' +
        '@page {size: A4;margin: 20mm;}' +
        '.container {width: 100%;max-width: 100%;margin: 0 auto;padding: 10mm;box-sizing: border-box;page-break-inside: avoid;}' +
        '.title {text-align: center;font-size: 18px;font-weight: bold;margin-bottom: 5px;}' +
        '.subtitle {text-align: center;font-size: 18px;margin-bottom: 10px;}' +
        '.info {font-size: 16px;margin-bottom: 10px;line-height: 1.5;}' +
        '.record-table {width: 100%;border-collapse: collapse;margin-top: 10px;}' +
        '.record-table th, .record-table td {border: 1px solid #000;text-align: center;padding: 5px;font-size: 12px;vertical-align: middle;word-wrap: break-word;}' +
        '.record-table th {background-color: #f0f0f0;font-weight: bold;}' +
        '.record-table td {height: 25px;}' +
        '.record-table thead th:nth-child(1) {width: 20%;}' +
        '.record-table thead th:nth-child(2),' +
        '.record-table thead th:nth-child(3),' +
        '.record-table thead th:nth-child(4),' +
        '.record-table thead th:nth-child(5) {width: 15%;}' +
        '.record-table thead th:nth-child(6) {width: 20%;}' +
        '.row {display: flex;justify-content: center;height: 100px;padding-top: 20px;}' +
        'img {width: 20%;height: auto;}' +
        '@media print {body {    background-color: #fff;    -webkit-print-color-adjust: exact;}.container {page-break-inside: avoid;}.record-table th {background-color: #e0e0e0 !important;}}' +
        '</style></head><body>' +
        '<div class="container">' +
        '    <h3 class="title">' + accountInfo.shopName + '</h3>' +
        '    <p class="subtitle">(See rules 17 and 34)</p>' +
        '    <p class="subtitle">License No. F.L.2.No.' + accountInfo.licenceNO + '</p>' +
        '    <p class="info">' +
        '        Name and Address of the (Indian Citizen) Member: <b>' + memberName + '</b><br>' +
        '        Quantity permitted to be possessed at a time and in a month: <strong>5000 Units</strong>' +
        '    </p>' +
        '    <table class="record-table">' +
        '        <thead>' +
        '            <tr>' +
        '                <th>Month and Date of Issue</th>' +
        '                <th colspan="2">Total Issues</th>' +
        '                <th colspan="2">Quantity of Liquor Issued</th>' +
        '                <th>Remarks</th>' +
        '            </tr>' +
        '            <tr>' +
        '                <th></th>' +
        '                <th>Spirits<br>L. ML</th>' +
        '                <th>Wines<br>L. ML</th>' +
        '                <th>Beer<br>L. ML</th>' +
        '                <th>Sealed Bottles<br>L. ML</th>' +
        '                <th></th>' +
        '            </tr>' +
        '        </thead>' +
        '        <tbody>';
    var totalAmount = 0;
    for (var i in response.data) {
        var data = response.data[i];
        let date = new Date(data.date * 1000);
        var tbody = '<tr>' + '<td>' + date.toISOString().split('T')[0] + '</td>';
        var spirits = 0; var wines = 0; var beer = 0; var totQty = 0;
        var sales = data.saleDetails[0];
        var amount = 0;
        sales.purchasedItems.forEach(item => {
            let { qty, SKU } = item;
            let parts = SKU.split("-");
            let mlPart = parts.find(part => part.endsWith("ML"));
            if (mlPart) {
                let ml = parseInt(mlPart.replace("ML", ""), 10);

                if (SKU.includes("MISTY") || SKU.includes("WINE")) {
                    wines += ml * qty;
                } else {
                    spirits += ml * qty;
                }
            } else {
                beer += qty * 650;
            }
            amount = amount + (getItemPrice(SKU) * qty);
            totQty = totQty + qty;
        });
        tbody += '<td>' + spirits + '</td><td>' + wines + '</td><td>' + beer + '</td><td>' + totQty + '</td><td>₹ ' + amount + '</td></tr>';
        totalAmount += amount;
        template += tbody;
    }
    template += '<tr><td></td><td></td><td></td><td></td><td><b>Total</b></td><td>₹ ' + totalAmount + '</td></tr></tr>';
    template += '</tbody></table></div>';
    template += '<div class="row"><img src="http://tnfl2.com/images/' + accountInfo.shopNumber + '.png"></div></body></html>';


    const newWindow = window.open("", "_blank");
    if (newWindow) {
        newWindow.document.write(template);
        newWindow.document.close();
        newWindow.print();
    }
}

function getItemPrice(SKU) {
    const productMaster = localStorage.getItem('products');
    const products = productMaster ? JSON.parse(productMaster) : null;
    for (var i in products.productList) {
        var item = products.productList[i];
        if (item.SKU == SKU) {
            return item.purchasePrice + 20;
        }
    }
    return 0;
}

document.getElementById('member-startDate').flatpickr({
    inline: true
});
document.getElementById('member-endDate').flatpickr({
    inline: true
});

