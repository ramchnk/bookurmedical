package com.sellinall.order.util;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ChannelsUtil {

	public static void init() throws Exception {
		String sites = getChannels(false);
		PostingSites.getConfig().setSites(sites);
	}

	private static String getChannels(boolean isListing) throws Exception {
		// isListing will return listingSites & if isListing is false will return sites
		String accountManagementUrl = Config.getConfig().getSIAAccountManagementURL() + "/channels?isListing="
				+ isListing;
		Map<String, String> header = new HashMap<String, String>();
		header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		header.put("Content-Type", "application/json");

		JSONObject responseObject = HttpsURLConnectionUtil.doGet(accountManagementUrl, header);
		JSONObject response = new JSONObject(responseObject.getString("payload"));
		JSONArray responseArray = response.getJSONArray("sites");
		String channels = "";
		for (int i = 0; i < responseArray.length(); i++) {
			channels += !channels.isEmpty() ? "-" + responseArray.getString(i) : responseArray.getString(i);
		}
		return channels;
	}

}