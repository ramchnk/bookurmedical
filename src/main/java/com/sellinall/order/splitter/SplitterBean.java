/**
 * 
 */
package com.sellinall.order.splitter;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Body;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Mallikarjun
 * 
 */
public class SplitterBean {
	static Logger log = Logger.getLogger(SplitterBean.class.getName());

	public List<Message> splitOrders(@Body JSONObject body) throws JSONException {
		JSONArray orderItemList = body.getJSONArray("orderItems");
		List<Message> answer = new ArrayList<Message>();
		for (int index = 0; index < orderItemList.length() ; index++) {
			JSONObject splitBody = new JSONObject();
			splitBody = orderItemList.getJSONObject(index);
			DefaultMessage message = new DefaultMessage();
			message.setBody(splitBody);
			answer.add(message);
		}
		return answer;
	}
}