/* assets/js/db.js */
const db = new Dexie("SmartStockHubOffline");
db.version(1).stores({
    sales: "++id, shopNumber, saleDate, timeCreatedAt",
    productMaster: "shopNumber",
    members: "++id, shopNumber",
    expenses: "++id, shopNumber",
    users: "username"
});

const Backend = {
    async login(username, password) {
        if ((username === "admin" || username === "admin@gmail.com") && password === "admin") {
            return {
                status: "success",
                accessToken: "offline-token",
                refreshToken: "offline-refresh",
                user: { username: "admin", role: "OWNER" },
                shopNumber: "1"
            };
        }
        return { status: "failed", message: "Invalid username or password" };
    },
    async getAccountInfo() {
        return {
            status: "success",
            account: { shopName: "Smart Stock Hub Offline", shopNumber: "1" },
            user: { username: "admin", role: "OWNER" }
        };
    },
    async getWaiters(shopNumber) { return { status: "success", data: [] }; },
    async getSales(shopNumber) {
        let sales = await db.sales.where("shopNumber").equals(shopNumber).reverse().sortBy("timeCreatedAt");
        return { status: "success", data: sales.map(d => ({ ...d, _id: d.id.toString(), id: d.id.toString(), productList: d.productList || [] })) };
    },
    async getSingleSales(id) {
        const sale = await db.sales.get(parseInt(id));
        if (!sale) return { status: "failed" };
        return { status: "success", data: { ...sale, _id: sale.id.toString(), id: sale.id.toString() } };
    },
    async getProductMaster(shopNumber) {
        let pm = await db.productMaster.get(shopNumber);
        if (!pm) pm = await seedData(shopNumber);
        return { status: "success", data: pm };
    },
    async createSales(shopNumber, request) {
        request.shopNumber = shopNumber;
        request.timeCreatedAt = Math.floor(Date.now() / 1000);
        request.invoiceNumber = Date.now().toString();
        request.saleDate = request.timeCreatedAt;


        await db.sales.add(request);
        await updateStock(shopNumber, request.productList);
        return { status: "success" };
    },
    async getMemberSales(shopNumber, invoiceNumber) {
        console.log("getMemberSales called for Invoice:", invoiceNumber);
        // invoiceNumber is not indexed, so scan all
        const allSales = await db.sales.toArray();
        const sale = allSales.find(s => s.invoiceNumber == invoiceNumber);

        if (!sale) {
            console.warn("Sale record not found!");
            return { status: "success", data: [] };
        }

        if (!sale.memberSales || sale.memberSales.length === 0) {
            console.log("Generating Member Sales...");
            let members = await db.members.where("shopNumber").equals(shopNumber).toArray();
            console.log("Members found:", members.length);

            if (members.length === 0) {
                console.log("Seeding Guest Member...");
                await db.members.add({ shopNumber: shopNumber, name: "Guest Member" });
                members = await db.members.where("shopNumber").equals(shopNumber).toArray();
            }

            if (members.length > 0) {
                sale.memberSales = generateMemberSales(members, sale.productList);
                console.log("Generated Distribution:", sale.memberSales);
                // Update DB to persist this random assignment
                await db.sales.put(sale);
            }
        }

        return {
            status: "success",
            data: [{
                saleDetails: sale.memberSales || [],
                date: sale.saleDate
            }]
        };
    },
    async updateSaleDate(shopNumber, request) {
        const id = parseInt(request.id);
        const existing = await db.sales.get(id);
        if (!existing) return { status: "failed" };
        if (request.productList) {
            Object.assign(existing, request);
            delete existing.id;
            await db.sales.put(existing, id);
            await updateStock(shopNumber, request.productList, true);
        } else {
            existing.saleDate = request.date;
            await db.sales.put(existing, id);
        }
        return { status: "success" };
    },
    async getDraftSales(shopNumber) { return { status: "success", data: null }; },
    async getMembers(shopNumber) {
        const members = await db.members.where("shopNumber").equals(shopNumber).toArray();
        return { status: "success", data: members };
    },
    async addMember(shopNumber, request) {
        request.shopNumber = shopNumber;
        await db.members.add(request);
        return { status: "success" };
    },
    async addExpense(shopNumber, request) {
        request.shopNumber = shopNumber;
        await db.expenses.add(request);
        return { status: "success" };
    },
    async getExpenses(shopNumber) {
        const customExpenses = await db.expenses.where("shopNumber").equals(shopNumber).toArray();
        const defaultExpenses = [
            { name: "Genral" }, { name: "Stationary" }, { name: "Food" }, { name: "Miscellaneous" }
        ];
        return {
            status: "success",
            data: [...defaultExpenses, ...customExpenses]
        };
    },
    async getDashboard(shopNumber) {
        let pm = await db.productMaster.get(shopNumber);
        if (!pm) pm = await seedData(shopNumber);

        // Calculate totals
        let totalInvestment = 0;
        let totalUnits = { "WINE": 0, "BEER": 0, "RUM": 0, "BRANDY": 0, "WHISKY": 0, "VODKA": 0, "GIN": 0, "COOLDRINKS": 0, "CIGERATTE": 0 };

        pm.productList.forEach(p => {
            totalInvestment += ((parseFloat(p.purchasePrice) || 0) * (parseFloat(p.stock) || 0));
            // Simplified unit calc (approximate)
            let brand = p.brand ? p.brand.toUpperCase() : "OTHER";
            if (totalUnits[brand] !== undefined) {
                totalUnits[brand] += (parseFloat(p.stock) || 0);
            }
        });

        return {
            status: "success",
            investmentAmount: totalInvestment.toFixed(2),
            totalUnits: totalUnits
        };
    },
    async updateProduct(shopNumber, request) {
        let pm = await db.productMaster.get(shopNumber);
        if (!pm) return { status: "failed" };

        const idx = pm.productList.findIndex(p => p.SKU === request.oldSKU);
        if (idx !== -1) {
            // Update existing
            pm.productList[idx] = { ...pm.productList[idx], ...request };
            // Ensure SKU update if changed
            if (request.SKU !== request.oldSKU) pm.productList[idx].SKU = request.SKU;

            await db.productMaster.put(pm, shopNumber);
            return { status: "success" };
        }
        return { status: "failed", message: "Product not found" };
    },
    async deleteProduct(shopNumber, request) {
        let pm = await db.productMaster.get(shopNumber);
        if (!pm) return { status: "failed" };

        const newParams = pm.productList.filter(p => p.SKU !== request.SKU);
        pm.productList = newParams;
        await db.productMaster.put(pm, shopNumber);
        return { status: "success" };
    },
    async rearrangeProducts(shopNumber, request) {
        let pm = await db.productMaster.get(shopNumber);
        if (!pm) return { status: "failed" };

        // request.productList contains [{SKU: '...', order: 1}, ...]
        // Create a map for quick lookup
        let orderMap = {};
        request.productList.forEach(item => {
            orderMap[item.SKU] = item.order;
        });

        pm.productList.forEach(p => {
            if (orderMap[p.SKU] !== undefined) {
                p.order = orderMap[p.SKU];
            }
        });
        // Sort by order 
        pm.productList.sort((a, b) => (a.order || 0) - (b.order || 0));

        await db.productMaster.put(pm, shopNumber);
        return { status: "success" };
    },
    async addProduct(shopNumber, request) {
        let pm = await db.productMaster.get(shopNumber);
        if (!pm) pm = await seedData(shopNumber);

        let newItems = [];
        let details = request.details || []; // Array of sizes

        details.forEach(det => {
            let sku = request.SKU + "-" + det.size;
            // For Beer/Unique items, logic might differ slightly but based on purchase.js:
            if (request.category === "Beer" || request.category === "COOLDRINKS" || request.category === "CIGERATTE") {
                // For beer, SKU is often clean or has size already?
                // details.push({ size: size ... })
                // purchase.js Line 107: const fullSKU = `${sellerSKU}-${size}`;
                sku = request.SKU + "-" + det.size;
            }

            newItems.push({
                SKU: sku,
                brand: request.brand,
                category: request.category,
                stock: det.stock,
                purchasePrice: det.purchasePrice,
                salePrice: det.salePrice,
                profitAmount: det.profitAmount,
                openingStock: det.stock
            });
        });

        // Check duplicates? Assuming validation done in frontend
        pm.productList.push(...newItems);
        await db.productMaster.put(pm, shopNumber);
        return { status: "success" };
    },
    async addPurchase(shopNumber, request) {
        // request has billNo, purchaseDate, productList (SKU, purchaseStock, purchaseAmount)
        // Save to Purchases collection (if exists, or just return success and update stock)
        // db.js didn't define 'purchases' table in step 676. 
        // I should add 'purchases' table or just store in localStorage mock? 
        // But db.version(1).stores does not have purchases.
        // I'll update db.js stores first? No, difficult mid-flight. 
        // I'll just update STOCK in productMaster. 
        // And maybe mock "getPurchases" by storing in 'sales'? No.
        // Wait, step 676 shows stores: sales, productMaster, members, expenses, users.
        // No purchases table.
        // I can add it to 'expenses' if I squint? No.
        // I can add it to 'sales' with a special flag? No.
        // I will dynamically add table? Dexie requires version bump.
        // Use localStorage for purchase history for now? Or just update stock.
        // I'll update stock and log to console.

        await updateStock(shopNumber, request.productList, false, true); // Add stock

        // Persist purchase record in a simple localStorage array for now to support 'getPurchases'
        let history = JSON.parse(localStorage.getItem("offline_purchases_" + shopNumber) || "[]");
        history.push(request);
        localStorage.setItem("offline_purchases_" + shopNumber, JSON.stringify(history));

        return { status: "success" };
    },
    async getPurchases(shopNumber) {
        let history = JSON.parse(localStorage.getItem("offline_purchases_" + shopNumber) || "[]");
        // Wrap in data
        return {
            status: "success", data: history.map(h => ({
                ...h,
                billNumber: h.billNo,
                billTotalAmount: h.purchaseAmount,
                billTotalUnits: h.totalQuantity,
                purchaseList: h.productList
            }))
        };
    },
    async saveDraftPurchase(shopNumber, request) {
        localStorage.setItem("offline_draft_purchase_" + shopNumber, JSON.stringify(request));
        return { status: "success" };
    },
    async getDraftPurchase(shopNumber) {
        let draft = JSON.parse(localStorage.getItem("offline_draft_purchase_" + shopNumber));
        if (draft) return { status: "success", data: [draft] }; // Array per purchase.js expectation?
        return { status: "success", data: null };
    }
};
async function seedData(shopNumber) {
    const data = {
        shopNumber, productList: [
            { SKU: "KF-LAGER-650", category: "Beer", brand: "Kingfisher", stock: 100, salePrice: 180, purchasePrice: 150, profitAmount: 30, openingStock: 100 },
            { SKU: "RC-WHISKY-180", category: "Liquor", brand: "Royal Challenge", stock: 50, salePrice: 220, purchasePrice: 180, profitAmount: 40, openingStock: 50 }
        ], pettyCash: 0, isPettyCashEnabled: true
    };
    await db.productMaster.put(data, shopNumber);
    return data;
}
async function updateStock(shopNumber, items, setMode = false, addMode = false) {
    let pm = await db.productMaster.get(shopNumber);
    if (pm) {
        items.forEach(i => {
            let p = pm.productList.find(x => x.SKU === i.SKU);
            if (p) {
                if (setMode && i.closingStock !== undefined) p.stock = i.closingStock;
                else if (addMode) {
                    // Add stock (Purchase)
                    let qty = i.purchaseStock || i.quantity || 0;
                    p.stock = (parseFloat(p.stock) || 0) + parseFloat(qty);
                    p.purchaseStock = (parseFloat(p.purchaseStock) || 0) + parseFloat(qty); // Update accumulated purchase stock if needed? 
                    // Or just stock. The UI shows 'Purchase Stock' sometimes as opening + purchase. 
                    // db.js seed has 'purchaseStock' as a field.
                }
                else p.stock -= i.sales;
            }
        });
        await db.productMaster.put(pm, shopNumber);
    }
}

function generateMemberSales(members, productList) {
    // 1. Select 80% of members randomly
    const shuffled = [...members].sort(() => 0.5 - Math.random());
    const count = Math.ceil(members.length * 0.8);
    const activeMembers = shuffled.slice(0, Math.max(1, count));

    // Map memberId -> { id, name, purchasedItems: [] }
    let distribution = {};
    activeMembers.forEach(m => distribution[m.id] = { id: m.id, name: m.name, purchasedItems: [] });

    productList.forEach(p => {
        let qty = parseFloat(p.sales);
        if (qty > 0) {
            // Distribute qty among active members
            for (let i = 0; i < Math.floor(qty); i++) {
                const m = activeMembers[Math.floor(Math.random() * activeMembers.length)];
                // Find if item exists for member
                if (!distribution[m.id]) distribution[m.id] = { id: m.id, name: m.name, purchasedItems: [] };

                let existing = distribution[m.id].purchasedItems.find(x => x.SKU === p.SKU);
                if (existing) existing.qty++;
                else distribution[m.id].purchasedItems.push({ SKU: p.SKU, qty: 1 });
            }
        }
    });

    // Return distributions
    return Object.values(distribution);
}

window.Backend = Backend;
