/**
* Theme: Rasket- Responsive Bootstrap 5 Admin Dashboard
* Author: Techzaa
* Module/App: Theme Config Js
*/

var webmanagerurl = 'http://localhost:8080/stockhub';
(function () {

     var savedConfig = sessionStorage.getItem("__LARKON_CONFIG__");

     var html = document.getElementsByTagName("html")[0];

     var defaultConfig = {
          theme: "light",             // ['light', 'dark']

          topbar: {
               color: "light",       // ['light', 'dark']
          },

          menu: {
               size: "sm-hover-active",   // [ 'default', 'sm-hover-active', 'sm-hover-active', 'condensed', 'full']
               color: "dark",            // ['light', 'dark']
          },
     };

     this.html = document.getElementsByTagName('html')[0];

     config = Object.assign(JSON.parse(JSON.stringify(defaultConfig)), {});

     config.theme = html.getAttribute('data-bs-theme') || defaultConfig.theme;
     config.topbar.color = html.getAttribute('data-topbar-color') || defaultConfig.topbar.color;
     config.menu.color = html.getAttribute('data-menu-color') || defaultConfig.menu.color;
     config.menu.size = html.getAttribute('data-menu-size') || defaultConfig.menu.size;

     window.defaultConfig = JSON.parse(JSON.stringify(config));

     if (savedConfig !== null) {
          config = JSON.parse(savedConfig);
     }

     window.config = config;

     if (config) {
          html.setAttribute("data-bs-theme", config.theme);
          html.setAttribute("data-topbar-color", config.topbar.color);
          html.setAttribute("data-menu-color", config.menu.color);

          if (window.innerWidth <= 1140) {
               html.setAttribute("data-menu-size", "hidden");
          } else {
               html.setAttribute("data-menu-size", config.menu.size);
          }
     }
})();

const menuItems = [

     {
          "name": "products",
          "roles": ["OWNER"],
          "page": "product-list.html",
          "label": "Products",
          "icon": "t-shirt-bold-duotone"
     },
     {
          "name": "purchase",
          "roles": ["OWNER", "SALESMAN"],
          "page": "purchase.html",
          "label": "Purchases",
          "icon": "card-send-bold-duotone"
     },
     {
          "name": "indent",
          "roles": ["OWNER", "SALESMAN"],
          "page": "indent.html",
          "label": "Indent",
          "icon": "box-bold-duotone"
     },
     {
          "name": "transfer",
          "roles": ["OWNER", "SALESMAN"],
          "page": "transfer.html",
          "label": "Transfer",
          "icon": "transfer-horizontal-bold-duotone"
     },
     {
          "name": "sales",
          "roles": ["OWNER", "SALESMAN"],
          "page": "orders-list.html",
          "label": "Sales",
          "icon": "bag-smile-bold-duotone"
     },

     {
          "name": "members",
          "roles": ["OWNER"],
          "page": "members.html",
          "label": "Members",
          "icon": "users-group-two-rounded-bold-duotone"
     },
     {
          "name": "accounts",
          "roles": ["OWNER"],
          "page": "accounts.html",
          "label": "Accounts",
          "icon": "users-group-two-rounded-bold-duotone"
     },
     {
          "name": "tableSales",
          "roles": ["OWNER", "SALESMAN"],
          "page": "tableSales.html",
          "label": "Table Sales",
          "icon": "bedside-table-4-line-duotone"
     },
     {
          "name": "POS",
          "roles": ["OWNER", "SALESMAN"],
          "page": "POS.html",
          "label": "POS",
          "icon": "bedside-table-4-line-duotone"
     },
     {
          "name": "Logout",
          "roles": ["OWNER", "SALESMAN"],
          "page": "/",
          "label": "Logout",
          "icon": "logout-bold-duotone"
     }
]

function restrictMenu() {
     try {
          let user = JSON.parse(localStorage.getItem("user"));
          let userRole = user?.role;
          let filteredMenus = menuItems.filter(items => items?.roles?.includes(userRole));
          const navContainer = document.querySelector(".menu-container");
          let url = window.location.pathname;
          let lastSegment = url.split('/').filter(Boolean).pop();
          filteredMenus.forEach(item => {
               let showMenu = false;

               if (item?.name === "tableSales") {
                    let account = JSON.parse(localStorage.getItem("account"));
                    if (account?.isTableReservationEnabled) {
                         showMenu = true
                    }
               } else if (item?.name === "POS") {
                    let account = JSON.parse(localStorage.getItem("account"));
                    if (account?.isPOSEnabled) {
                         showMenu = true
                    }
               } else if (item?.name === "indent") {
                    let account = JSON.parse(localStorage.getItem("account"));
                    if (account?.isStoreRoomAvailable || account?.isTransferEnabled) {
                         showMenu = true
                    }
               } else if (item?.name === "transfer") {
                    let account = JSON.parse(localStorage.getItem("account"));
                    if (account?.isACTransferEnabled && (lastSegment === 'orders-list.html' || lastSegment === 'transfer.html')) {
                         showMenu = true
                    }
               } else {
                    showMenu = true;
               }
               if (item?.roles?.includes(userRole) && showMenu) {
                    let isActiveMenu = lastSegment === item?.page || (lastSegment === 'dashboard' && item?.name === 'dashboard');
                    const listItem = document.createElement("li");
                    listItem.className = `nav-item nav-${item.name}`;
                    if (item.name === 'Logout') {
                         listItem.innerHTML = `
                         <a class="nav-link ${isActiveMenu ? 'active' : ''} " href="#" onclick="clearAllCookies(); return false;">
                              <span class="nav-icon">
                                   <iconify-icon icon="solar:${item.icon}"></iconify-icon>
                              </span>
                              <span class="nav-text">${item.label}</span>
                         </a>`;
                    } else {
                         listItem.innerHTML = `
                         <a class="nav-link ${isActiveMenu ? 'active' : ''} " href="${item.page}">
                              <span class="nav-icon">
                                   <iconify-icon icon="solar:${item.icon}"></iconify-icon>
                              </span>
                              <span class="nav-text">${item.label}</span>
                         </a>`;
                    }

                    navContainer.appendChild(listItem);
               }
          });

     } catch (error) {

     }
}

function clearAllCookies() {
     const cookies = document.cookie.split(';');
     for (let i = 0; i < cookies.length; i++) {
          const cookie = cookies[i];
          const eqPos = cookie.indexOf('=');
          const name = eqPos > -1 ? cookie.substring(0, eqPos) : cookie;

          document.cookie = name.trim() +
               "=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/";
     }
     window.location.href = '/';

}

const sortByValume = (products) => {
     // Define volume and category order
     const volumeOrder = {
          "180ML": 1,
          "375ML": 2,
          "750ML": 3,
          "1000ML": 4,
     };
     const categoryOrder = {
          "Wine": 5,
          "Beer": 6,
          "CIGERATTE": 7,
     };

     // Extract volume from SKU (e.g., "180ML", "375ML", etc.)
     /*   const getVolume = (SKU) => {
             const match = SKU.match(/(\d+ML)$/i);
             return match ? match[0].toUpperCase() : null;
        }; */

     const getVolume = (SKU) => {
          const match = SKU.match(/(\d{3,4}ML)/i);
          return match ? match[0].toUpperCase() : null;
     };

     // Sort function
     const sortProducts = (a, b) => {
          const volumeA = getVolume(a.SKU);
          const volumeB = getVolume(b.SKU);

          const volumeIndexA = volumeOrder[volumeA] || 9999;
          const volumeIndexB = volumeOrder[volumeB] || 9999;

          // First sort by volume
          if (volumeIndexA !== volumeIndexB) {
               return volumeIndexA - volumeIndexB;
          }

          // Next sort by category
          const categoryIndexA = categoryOrder[a.category] || 9999;
          const categoryIndexB = categoryOrder[b.category] || 9999;
          return categoryIndexA - categoryIndexB;
     };

     products.sort(sortProducts);
     return products;
};


const sortByBrand = (products, isRumFirst) => {
     const brandOrder = isRumFirst
          ? ["RUM", "BRANDY", "WHISKY", "SCOTCH", "VODKA", "WINE", "BEER", "COOLDRINKS", "CIGERATTE"]
          : ["BRANDY", "WHISKY", "SCOTCH", "RUM", "VODKA", "WINE", "BEER", "COOLDRINKS", "CIGERATTE"];

     const groupedByBrand = {};

     // Group by brand
     products.forEach((product) => {
          const key = product.brand ? product.brand.toUpperCase() : "OTHER";
          if (!groupedByBrand[key]) {
               groupedByBrand[key] = [];
          }
          groupedByBrand[key].push(product);
     });

     // Define custom volume order for sorting
     const volumeOrder = {
          "1000ML": 1,
          "750ML": 2,
          "375ML": 3,
          "180ML": 4
     };

     const finalSortedList = [];

     function processGroup(list) {
          const groupedBySKU = {};

          // Group products by base SKU
          list.forEach((product) => {
               const baseSKU = product.SKU.split("-")[0];  // Get the base part of the SKU
               if (!groupedBySKU[baseSKU]) {
                    groupedBySKU[baseSKU] = [];
               }
               groupedBySKU[baseSKU].push(product);
          });

          // Now, sort each group by volume
          Object.keys(groupedBySKU).forEach((baseSKU) => {
               groupedBySKU[baseSKU].sort((a, b) => {
                    const getVolumeIndex = (sku) => {
                         const match = sku.match(/(\d+ML)$/); // Match the numeric part before 'ML'
                         return match ? volumeOrder[match[0]] || 999 : 999;
                    };

                    const aIndex = getVolumeIndex(a.SKU);
                    const bIndex = getVolumeIndex(b.SKU);
                    return aIndex - bIndex;
               });
          });

          // Combine the sorted SKU groups into the final list (flattened)
          Object.keys(groupedBySKU).forEach((baseSKU) => {
               finalSortedList.push(...groupedBySKU[baseSKU]);
          });
     }

     brandOrder.forEach((brand) => {
          if (groupedByBrand[brand]) {
               processGroup(groupedByBrand[brand]);
               delete groupedByBrand[brand];
          }
     });

     // Process remaining brands
     Object.keys(groupedByBrand).forEach((brand) => {
          processGroup(groupedByBrand[brand]);
     });

     return finalSortedList;
};

const sortByCustomOrder = (products) => {
     return products.sort((a, b) => {
          const orderA = a.order ?? 9999;
          const orderB = b.order ?? 9999;
          return orderA - orderB;
     });
};

function updateShopName() {
     let account = JSON.parse(localStorage.getItem("account"));
     if (account?.shopName) {
          $("#shopName").text(account.shopName);
     }
     if (account?.isStoreRoomAvailable) {
          webmanagerurl = 'https://tnfl2-cb6ea45c64b3.herokuapp.com/services';
     }
}

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


function getHost() {
     return 'https://tnfl2-cb6ea45c64b3.herokuapp.com/services';
}

// ============================================
// LICENSE VALIDATION 
// ============================================
const APP_LICENSE_KEY = "eyJjbGllbnQiOiJSQU0iLCJleHBpcnkiOjE3Njc0NDMyNjU2OTF9.32210511"; // <--- PASTE YOUR GENERATED LICENSE KEY INSIDE THE QUOTES

(function checkLicense() {
     // Whitelist pages that don't require license
     const path = window.location.pathname;
     // Allow access to expired page and the key generator page
     if (path.includes("expired.html") || path.includes("admin-license.html")) return;

     const SECRET_SALT = "STOCKHUB_OFFLINE_SECRET_KEY_999";

     function simpleHashLicense(str) {
          let hash = 0;
          for (let i = 0; i < str.length; i++) {
               const char = str.charCodeAt(i);
               hash = ((hash << 5) - hash) + char;
               hash = hash & hash;
          }
          return Math.abs(hash).toString(16);
     }

     try {
          if (!APP_LICENSE_KEY) {
               console.warn("No License Key Found - Redirecting to Expired Page.");
               // Redirecting because license is mandatory
               window.location.href = "expired.html";
               return;
          }

          const parts = APP_LICENSE_KEY.split('.');
          if (parts.length !== 2) throw new Error("Invalid Format");
          const b64 = parts[0];
          const sig = parts[1];

          if (simpleHashLicense(b64 + SECRET_SALT) !== sig) {
               throw new Error("Invalid Signature");
          }

          const payload = JSON.parse(atob(b64));
          if (Date.now() > payload.expiry) {
               throw new Error("License Expired");
          }

          console.log("License Valid for:", payload.client);

     } catch (e) {
          console.error("License Check Failed:", e);
          window.location.href = "expired.html";
     }
})();