/**
 * 
 */
package com.sellinall.order.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAOrderStatus;

/**
 * @author vikraman
 * 
 */
public class LoadUserDataByNicknameId implements Processor {
	static Logger log = Logger.getLogger(LoadUserDataByNicknameId.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String siteName = exchange.getProperty("siteName", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String accountingChannel = Config.getConfig().getSIAAccountingChannels();
		String orderStatus = exchange.getProperty("orderStatus", String.class);
		Document queryResult = runQuery(accountNumber, nickNameID, siteName, accountingChannel);
		exchange.setProperty("syncInventory", (Boolean) queryResult.get("syncInventory"));
		List<Document> userSiteSpecificObjectList = (List<Document>) queryResult.get(siteName);
		// always userSiteSpecificObject contains only one siteName(eBay-1 only)
		Document userSiteSpecificObject = userSiteSpecificObjectList.get(0);
		String profileID = "";
		if (userSiteSpecificObject.containsKey("invoiceProfile") && userSiteSpecificObject.get("invoiceProfile") != null
				&& !userSiteSpecificObject.get("invoiceProfile").equals("null")) {
			profileID = userSiteSpecificObject.getString("invoiceProfile");
		} else if (userSiteSpecificObject.containsKey("profile") && userSiteSpecificObject.get("profile") != null
				&& !userSiteSpecificObject.get("profile").equals("null")) {
			profileID = userSiteSpecificObject.getString("profile");
		}
		if (!profileID.isEmpty()) {
			List<Document> userProfileList = (List<Document>) queryResult.get("profile");
			String invoiceNumberPrefix = getinvoiceNumberPrefix(userProfileList, profileID);
			exchange.setProperty("invoiceNumberPrefix", invoiceNumberPrefix);
			exchange.setProperty("profileID", profileID);
		}
		if (userSiteSpecificObject.containsKey("timeLinked") && userSiteSpecificObject.get("timeLinked") != null) {
			exchange.setProperty("timeLinked",
					new BigDecimal(userSiteSpecificObject.get("timeLinked").toString()).longValue());
		}

		exchange.setProperty("isAccountingChannel", false);
		String[] channels = accountingChannel.split("-");
		for (String channel : channels) {
			if (queryResult.containsKey(channel)) {
				exchange.setProperty("isAccountingChannel", true);
			}
		}
		exchange.setProperty("isNinjaVanShippingCarrier", false);
		exchange.setProperty("isJanioShippingCarrier", false);
		exchange.setProperty("isSingPostShippingCarrier", false);
		exchange.setProperty("isJTExpressShippingCarrier", false);
		exchange.setProperty("isAramexShippingCarrier", false);
		exchange.setProperty("isMaatramIntegratedShippingCarrier", false);
		exchange.setProperty("isMaatramBridgeIntegratedShippingCarrier", false);
		if (userSiteSpecificObject.containsKey("shippingCarrier")
				&& userSiteSpecificObject.get("shippingCarrier") != null) {
			List<String> shippingCarrier = (List<String>) userSiteSpecificObject.get("shippingCarrier");
			if (shippingCarrier.contains("ninjaVan")) {
				if (userSiteSpecificObject.containsKey("isAutoAcceptOrder")
						&& !userSiteSpecificObject.getBoolean("isAutoAcceptOrder")
						&& orderStatus.equals(SIAOrderStatus.INITIATED.toString())) {
					// isAutoAcceptOrder = false and orderStatus = INITIATED, No need to publish
					// this for ninjavan
					// That accounts auto accept disabled account
				} else {
					// If that channel account don't have 'isAutoAcceptOrder'
					// flag then we can publish to ninjavan
					exchange.setProperty("isNinjaVanShippingCarrier", true);
				}
			} else if (shippingCarrier.contains("janio")) {
				exchange.setProperty("isJanioShippingCarrier", true);
			} else if (shippingCarrier.size() > 0) {
				String shippingCarrierName = shippingCarrier.get(0);
				if (shippingCarrierName.startsWith("jtExpress")) {
					exchange.setProperty("isJTExpressShippingCarrier", true);
				} else if (shippingCarrierName.startsWith("singPost")) {
					exchange.setProperty("isSingPostShippingCarrier", true);
				} else if (shippingCarrierName.startsWith("aramexShipping")) {
					exchange.setProperty("isAramexShippingCarrier", true);
				}
				List<String> maatramIntegratedShippingCarrierList = Arrays
						.asList(Config.getConfig().getMaatramIntegratedShippingCarrier().split("-"));
				maatramIntegratedShippingCarrierList.stream().forEach(i -> {
					if (shippingCarrierName.startsWith(i)) {
						exchange.setProperty("isMaatramIntegratedShippingCarrier", true);
						if (Arrays.asList(Config.getConfig().getMaatramBridgeIntegratedServers().split("-"))
								.contains(shippingCarrierName.split("-")[0])) {
							exchange.setProperty("isMaatramBridgeIntegratedShippingCarrier", true);
						}
					}
				});
			}
		}
		exchange.setProperty("isInforWMS", false);
		exchange.setProperty("isSatsacoWMS", false);
		exchange.setProperty("isNetSuite", false);
		exchange.setProperty("isOdoo", false);
		exchange.setProperty("isSiAWMS", false);
		exchange.setProperty("isAramexWMS", false);
		exchange.setProperty("isVend", false);

		// Handle warehousebased stock update
		boolean isEligibleToProceed = true;
		boolean enableWarehouseBasedStock = false;
		String[] warehouses = Config.getConfig().getWarehouses().split("-");
		List<String> siaLinkedWarehouseList = new LinkedList<>();
		for (String warehouse : warehouses) {
			if (queryResult.containsKey(warehouse)) {
				siaLinkedWarehouseList.add(warehouse);
			}
		}
		if (queryResult.containsKey("enableWarehouseBasedStock")
				&& queryResult.getBoolean("enableWarehouseBasedStock")) {
			enableWarehouseBasedStock = true;
		}

		exchange.setProperty("isMaatramIntegratedWms", false);
		exchange.setProperty("isMaatramBridgeIntegratedWms", false);
		if (userSiteSpecificObject.containsKey("wms") && userSiteSpecificObject.get("wms") != null) {
			List<String> wmsList = (List<String>) userSiteSpecificObject.get("wms");
			for (int i = 0; i < wmsList.size(); i++) {
				String wms = wmsList.get(i).toString();
				List<String> maatramIntegratedWmsList = Arrays
						.asList(Config.getConfig().getMaatramIntegratedWms().split("-"));
				maatramIntegratedWmsList.stream().forEach(configWmsValue -> {
					if (wms.startsWith(configWmsValue)) {
						exchange.setProperty("isMaatramIntegratedWms", true);
					}
					if (Arrays.asList(Config.getConfig().getMaatramBridgeIntegratedServers().split("-"))
							.contains(wms.split("-")[0])) {
						exchange.setProperty("isMaatramBridgeIntegratedWms", true);
					}
				});
				String warehouseName = wms.split("-")[0];
				if (enableWarehouseBasedStock) {
					if (siaLinkedWarehouseList.contains(warehouseName)) {
						exchange.setProperty("warehouseName", warehouseName);
					} else {
						isEligibleToProceed = false;
					}
				}
				if (wms.startsWith("satsaco")) {
					exchange.setProperty("isSatsacoWMS", true);
					break;
				}
				if (wms.startsWith("infor")) {
					exchange.setProperty("isInforWMS", true);
					break;
				}
				if (wms.startsWith("aramex")) {
					exchange.setProperty("isAramexWMS", true);
					break;
				}
				if (wms.startsWith("SiAWMS")) {
					exchange.setProperty("isSiAWMS", true);
					break;
				}
			}
		} else if (enableWarehouseBasedStock) {
			isEligibleToProceed = false;
		}
		exchange.setProperty("isEligibleToProceed", isEligibleToProceed);
		if (exchange.getProperty("isInforWMS", boolean.class)
				|| exchange.getProperty("isNinjaVanShippingCarrier", boolean.class)
				|| exchange.getProperty("isJanioShippingCarrier", boolean.class)) {
			exchange.setProperty("isPartnerLogistics", true);
		}
		exchange.setProperty("isMaatramIntegratedErp", false);
		exchange.setProperty("isMaatramBridgeIntegratedErp", false);
		if (userSiteSpecificObject.containsKey("erp") && userSiteSpecificObject.get("erp") != null) {
			List<String> erpList = (List<String>) userSiteSpecificObject.get("erp");
			for (int i = 0; i < erpList.size(); i++) {
				String erp = erpList.get(i).toString();
				List<String> maatramIntegratedErpList = Arrays
						.asList(Config.getConfig().getMaatramIntegratedErp().split("-"));
				maatramIntegratedErpList.stream().forEach(configErpValue -> {
					if (erp.startsWith(configErpValue)) {
						exchange.setProperty("isMaatramIntegratedErp", true);
					}
					if (Arrays.asList(Config.getConfig().getMaatramBridgeIntegratedServers().split("-"))
							.contains(erp.split("-")[0])) {
						exchange.setProperty("isMaatramBridgeIntegratedErp", true);
					}
				});
				if (erp.startsWith("netSuite")) {
					exchange.setProperty("isNetSuite", true);
					break;
				} else if (erp.startsWith("odoo")) {
					exchange.setProperty("isOdoo", true);
					break;
				} else if (erp.startsWith("vend")) {
					exchange.setProperty("isVend", true);
					break;
				}
			}
		}

		// OMS

		exchange.setProperty("isMaatramIntegratedOms", false);
		exchange.setProperty("isMaatramBridgeIntegratedOms", false);
		if (userSiteSpecificObject.containsKey("oms") && userSiteSpecificObject.get("oms") != null) {
			List<String> omsList = (List<String>) userSiteSpecificObject.get("oms");
			for (int i = 0; i < omsList.size(); i++) {
				String oms = omsList.get(i).toString();
				List<String> maatramIntegratedOmsList = Arrays
						.asList(Config.getConfig().getMaatramIntegratedOms().split("-"));
				maatramIntegratedOmsList.stream().forEach(configOmsValue -> {
					if (oms.startsWith(configOmsValue)) {
						exchange.setProperty("isMaatramIntegratedOms", true);
					}
					if (Arrays.asList(Config.getConfig().getMaatramBridgeIntegratedServers().split("-"))
							.contains(oms.split("-")[0])) {
						exchange.setProperty("isMaatramBridgeIntegratedOms", true);
					}
				});
				if (oms.startsWith("omsPro")) {
					exchange.setProperty("isOmsPro", true);
					break;
				}
			}
		}

		exchange.setProperty("merchantID", queryResult.get("merchantID"));
		if (userSiteSpecificObject.containsKey("countryCode")) {
			exchange.setProperty("countryCode", userSiteSpecificObject.getString("countryCode"));
		}
		exchange.setProperty("userSiteSpecificObject", userSiteSpecificObject);
		Boolean ignoreSoldEvent = false;
		if (userSiteSpecificObject.containsKey("ignoreSoldEvent")) {
			ignoreSoldEvent = userSiteSpecificObject.getBoolean("ignoreSoldEvent");
		}
		exchange.setProperty("ignoreSoldEvent", ignoreSoldEvent);
		Boolean isManaged = false;
		if (userSiteSpecificObject.containsKey("isManaged")) {
			isManaged = userSiteSpecificObject.getBoolean("isManaged");
		}
		exchange.setProperty("isManaged", isManaged);

		Boolean processRule = false;
		if (userSiteSpecificObject.containsKey("processRule")) {
			processRule = userSiteSpecificObject.getBoolean("processRule");
		}
		exchange.setProperty("processRule", processRule);

		boolean isTransactionFee = false;
		if (userSiteSpecificObject.containsKey("isTransactionFee")) {
			isTransactionFee = userSiteSpecificObject.getBoolean("isTransactionFee");
		}
		exchange.setProperty("isTransactionFee", isTransactionFee);

		boolean syncMultipleUnitSKUs = false;
		if (queryResult.containsKey("syncMultipleUnitSKUs")) {
			syncMultipleUnitSKUs = (Boolean) queryResult.get("syncMultipleUnitSKUs");
		}
		boolean syncDuplicateSKUs = false;
		if (queryResult.containsKey("syncDuplicateSKUs")) {
			syncDuplicateSKUs = (Boolean) queryResult.get("syncDuplicateSKUs");
		} else if ((queryResult.containsKey("individualSKUPerChannel")
				&& queryResult.getBoolean("individualSKUPerChannel") && queryResult.getBoolean("syncInventory"))
				|| syncMultipleUnitSKUs) {
			syncDuplicateSKUs = true;
		}
		exchange.setProperty("syncDuplicateSKUs", syncDuplicateSKUs);
		boolean syncBundleSKUs = false;
		if (queryResult.containsKey("syncBundleSKUs")) {
			syncBundleSKUs = queryResult.getBoolean("syncBundleSKUs");
			// Default delimiter
			String bundleDelimiter = "+";
			if (queryResult.containsKey("bundleDelimiter")) {
				bundleDelimiter = queryResult.getString("bundleDelimiter");
			}
			exchange.setProperty("bundleDelimiter", bundleDelimiter);

		}
		exchange.setProperty("syncBundleSKUs", syncBundleSKUs);
		exchange.setProperty("syncMultipleUnitSKUs", syncMultipleUnitSKUs);
		if (queryResult.containsKey("showOnlyManagedOrders")) {
			exchange.setProperty("showOnlyManagedOrders", queryResult.getBoolean("showOnlyManagedOrders"));
		}
		boolean isNeedtoUpdateProductMaster = false;
		ArrayList<String> wmsList = new ArrayList<String>();
		if (queryResult.containsKey("wmsList")) {
			wmsList = (ArrayList<String>) queryResult.get("wmsList");
			if (wmsList.size() == 1) {
				isNeedtoUpdateProductMaster = true;
				exchange.setProperty("wmsList", wmsList);
			}
		}
		exchange.setProperty("isNeedtoUpdateProductMaster", isNeedtoUpdateProductMaster);
		boolean isProductMasterReady = false;
		if (queryResult.containsKey("isProductMasterReady")) {
			isProductMasterReady = queryResult.getBoolean("isProductMasterReady");
		}
		exchange.setProperty("isProductMasterReady", isProductMasterReady);
		boolean isWmsSelected = false;
		if (queryResult.containsKey("syncInventory") && (Boolean) queryResult.get("syncInventory")) {
			if (userSiteSpecificObject.containsKey("wms")) {
				ArrayList<String> wmsListinChannel = (ArrayList<String>) userSiteSpecificObject.get("wms");
				if (wmsListinChannel.size() == 1) {
					isWmsSelected = true;
					exchange.setProperty("selectedWMS", wmsListinChannel.get(0));
				} else if (userSiteSpecificObject.containsKey("multiWarehouseMapping")) {
					isWmsSelected = true;
					exchange.setProperty("selectedWMSList", wmsListinChannel);
				} else {
					log.error("More than one WMS selected for accountNumber : " + accountNumber + ", nickName: "
							+ nickNameID);
				}
			} else {
				log.error("WMS not found for accountNumber : " + accountNumber + ", nickName: " + nickNameID);
			}
		}
		exchange.setProperty("isWmsSelected", isWmsSelected);
		boolean processOrdersWithSKUOnly = false;
		if (userSiteSpecificObject.containsKey("processOrdersWithSKUOnly")) {
			processOrdersWithSKUOnly = userSiteSpecificObject.getBoolean("processOrdersWithSKUOnly");
		}
		exchange.setProperty("processOrdersWithSKUOnly", processOrdersWithSKUOnly);
	}

	private Document runQuery(String accountNumber, String nickNameID, String siteName, String accountingChannel) {
		Document elemMatch = new Document("nickName.id", nickNameID);
		Document searchQuery = new Document(siteName, new Document("$elemMatch", elemMatch));
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);

		Document projection = new Document(siteName + ".$", 1);
		projection.put("merchantID", 1);
		projection.put("profile", 1);
		projection.put("syncDuplicateSKUs", 1);
		projection.put("individualSKUPerChannel", 1);
		projection.put("syncMultipleUnitSKUs", 1);
		projection.put("syncInventory", 1);
		projection.put("showOnlyManagedOrders", 1);
		projection.put("syncBundleSKUs", 1);
		projection.put("bundleDelimiter", 1);
		projection.put("enableWarehouseBasedStock", 1);
		projection.put("wmsList", 1);
		projection.put("isProductMasterReady", 1);

		String[] channels = accountingChannel.split("-");
		for (String channel : channels) {
			projection.put(channel, 1);
		}
		String[] warehouses = Config.getConfig().getWarehouses().split("-");
		for (String warehouse : warehouses) {
			projection.put(warehouse, 1);
		}
		MongoCollection<Document> table = DbUtilities.getDBCollection("accounts");
		Document accountDocument = table.find(searchQuery).projection(projection).first();
		Document accountDetails = Document.parse(accountDocument.toJson());
		return accountDetails;
	}

	private static String getinvoiceNumberPrefix(List<Document> proflieList, String profileID) {
		for (Document profile : proflieList) {
			Document nickName = (Document) profile.get("nickName");
			if (nickName.getString("id").equals(profileID) && profile.containsKey("invoiceNumberPrefix")) {
				return profile.getString("invoiceNumberPrefix");
			}
		}
		return "";
	}
}