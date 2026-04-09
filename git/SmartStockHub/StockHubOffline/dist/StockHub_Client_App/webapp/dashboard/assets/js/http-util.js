const salesurl = webmanagerurl + "/sales";
const productMasterurl = webmanagerurl + "/productmaster";
const membersurl = webmanagerurl + "/members";

// Mock Session State
let currentUser = null;

function getCookie(name) {
    if (name === 'accessToken' || name === 'refreshToken') return "offline-token";
    return null;
}
function setCookie(name, value) { }
function clearSession() { currentUser = null; }

// Main Mock Request Function
async function httpRequest(url, method = 'GET', body = null, headers = {}, callback = null, isAuthReq = true) {
    console.log(`[Offline] ${method} ${url}`, body);

    try {
        let response = { status: "failed" };
        const shopNumber = "1"; // Default Shop

        // Route Requests
        if (url.includes("/sales") && !url.includes("/members")) {
            if (method === 'GET') {
                if (url.includes("id=")) {
                    const id = url.split("id=")[1];
                    response = await Backend.getSingleSales(id);
                } else if (url.includes("/draft")) {
                    response = await Backend.getDraftSales(shopNumber);
                } else {
                    response = await Backend.getSales(shopNumber);
                }
            } else if (method === 'POST') {
                response = await Backend.createSales(shopNumber, body);
            } else if (method === 'PUT') {
                response = await Backend.updateSaleDate(shopNumber, body);
            }
        }
        else if (url.includes("/productmaster")) {
            if (url.includes("/updateItem") && method === 'PUT') {
                response = await Backend.updateProduct(shopNumber, body);
            } else if (url.includes("/rearrangeItems") && method === 'PUT') {
                response = await Backend.rearrangeProducts(shopNumber, body);
            } else if (method === 'DELETE') {
                response = await Backend.deleteProduct(shopNumber, body);
            } else if (method === 'POST') {
                response = await Backend.addProduct(shopNumber, body);
            } else if (method === 'PUT') {
                // Generic PUT to /productmaster implies Adding Stock/Purchase
                response = await Backend.addPurchase(shopNumber, body);
            } else {
                response = await Backend.getProductMaster(shopNumber);
            }
        }
        else if (url.includes("/purchase")) {
            if (url.includes("/draft")) {
                if (method === 'POST') response = await Backend.saveDraftPurchase(shopNumber, body);
                else response = await Backend.getDraftPurchase(shopNumber);
            } else {
                response = await Backend.getPurchases(shopNumber);
            }
        }
        else if (url.includes("/dashboard")) {
            response = await Backend.getDashboard(shopNumber);
        }
        else if (url.includes("/members")) {
            if (url.includes("/sales")) {
                console.log("ram test");
                const invoiceNumber = url.split("invoiceNumber=")[1];
                response = await Backend.getMemberSales(shopNumber, invoiceNumber);
            } else if (method === 'POST') {
                response = await Backend.addMember(shopNumber, body);
            } else {
                response = await Backend.getMembers(shopNumber);
            }
        }
        else if (url.includes("/account/getAccountInfo")) {
            response = await Backend.getAccountInfo();
        }
        else if (url.includes("/expenses")) {
            response = await Backend.getExpenses(shopNumber);
        }
        else if (url.includes("/waiters")) {
            response = await Backend.getWaiters(shopNumber);
        }
        else if (url.includes("/auth")) {
            const u = body.email || body.username;
            const p = body.password;
            const loginRes = await Backend.login(u, p);
            response = loginRes;
            if (loginRes.status === "success") {
                currentUser = loginRes.user;
                localStorage.setItem("user", JSON.stringify(loginRes.user));
                localStorage.setItem("shopNumber", loginRes.shopNumber);
            }
        }

        // Simulate Network Delay
        // await new Promise(r => setTimeout(r, 100));

        if (response.status === "success" || response.accessToken) {
            if (callback) callback(response, null);
            return response;
        } else {
            if (callback) callback(null, "failed");
            throw "Request Failed";
        }

    } catch (error) {
        console.error("Offline Error", error);
        if (callback) callback(null, error);
        throw error;
    }
}

// Helper Wrappers (Same signatures as original)
function getRequestWithCallback(url, body = null, callback, headers = {}) { return httpRequest(url, 'GET', body, headers, callback); }
function postRequestWithCallback(url, body = null, callback, headers = {}) { return httpRequest(url, 'POST', body, headers, callback); }
function putRequestWithCallback(url, body = null, callback, headers = {}) { return httpRequest(url, 'PUT', body, headers, callback); }
async function getRequest(url, body = null, headers = {}) { return await httpRequest(url, 'GET', body, headers); }
async function postRequest(url, body = null, headers = {}) { return await httpRequest(url, 'POST', body, headers); }
async function putRequest(url, body = null, headers = {}) { return await httpRequest(url, 'PUT', body, headers); }
async function postWithoutAuthRequest(url, body = null, headers = {}) { return await httpRequest(url, 'POST', body, headers, null, false); }
function deleteRequestWithCallback(url, body = null, callback, headers = {}) { return httpRequest(url, 'DELETE', body, headers, callback); }
async function deleteRequest(url, body = null, headers = {}) { return await httpRequest(url, 'DELETE', body, headers); }

// Expose to window to fix 'getRequest is not defined' error
window.httpRequest = httpRequest;
window.getRequestWithCallback = getRequestWithCallback;
window.postRequestWithCallback = postRequestWithCallback;
window.putRequestWithCallback = putRequestWithCallback;
window.getRequest = getRequest;
window.postRequest = postRequest;
window.putRequest = putRequest;
window.deleteRequest = deleteRequest;
window.deleteRequestWithCallback = deleteRequestWithCallback;
window.postWithoutAuthRequest = postWithoutAuthRequest;
window.postRequestWithCallback = postRequestWithCallback;

// Auto-Login Removed for Manual Login Flow
(function () {
    if (window.location.pathname.includes("/dashboard/") && !localStorage.getItem("user")) {
        window.location.href = "../index.html";
    }
})();
