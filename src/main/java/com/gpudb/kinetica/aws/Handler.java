package com.gpudb.kinetica.aws;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gpudb.GPUdb;
import com.gpudb.GPUdbBase;
import com.gpudb.GPUdbBase.Options;
import com.gpudb.GPUdbException;
import com.gpudb.protocol.InsertRecordsFromPayloadRequest;
import com.gpudb.protocol.InsertRecordsFromPayloadResponse;

public class Handler implements RequestStreamHandler {
	Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static void main(String[] args) throws Exception {
		Handler me = new Handler();
		String initialString = "[\r\n" + "  {\r\n" + "    \"key1\": \"value1\",\r\n" + "    \"key2\": \"value2\",\r\n"
				+ "    \"key3\": \"value3\"\r\n" + "  },\r\n" + "  {\r\n" + "    \"key1\": \"value1\",\r\n"
				+ "    \"key2\": \"value2\",\r\n" + "    \"key3\": \"va\\\\lue3\"\r\n" + "  },\r\n" + "  {\r\n"
				+ "    \"key1\": \"value1\",\r\n" + "    \"key2\": \"value2\",\r\n" + "    \"key3\": \"value3\"\r\n"
				+ "  }\r\n" + "]";
		InputStream inputStream = new ByteArrayInputStream(initialString.getBytes());
		me.handleRequest(inputStream, null, null);
	}

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		//System.out.println("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
		Map<String, String> env = System.getenv();
		String request_table_name = env.get("request_table_name");
		Map<String, String> request_create_table_options = null;
		Map<String, String> request_options = null;
		if (env.containsKey("request_create_table_options")) {
			request_create_table_options = gson.fromJson(env.get("request_create_table_options"), Map.class);
		}
		if (env.containsKey("request_options")) {
			request_options = gson.fromJson(env.get("request_options"), Map.class);
		}
		String kinetica_url = env.get("kinetica_url");
		String data_text = this.getPayload(inputStream);
		GPUdbBase.Options opts;
		try {
			opts = this.getOpts(env.get("kinetica_options"));
			GPUdb gpudb = new GPUdb(kinetica_url, opts);
			InsertRecordsFromPayloadRequest request = new InsertRecordsFromPayloadRequest(request_table_name, data_text,
					null, null, request_create_table_options, request_options);
			gpudb.insertRecordsFromPayload(request);
		} catch (GPUdbException e) {
			throw new IOException(e);
		}
		
	}



	private Options getOpts(String options) throws GPUdbException {
		GPUdbBase.Options result = new GPUdbBase.Options();
		Map<String, String> kinetica_options = null;
		kinetica_options = gson.fromJson(options, Map.class);
		if ( kinetica_options != null) {
			for (Entry<?, ?> entry : kinetica_options.entrySet()) {
				String key = entry.getKey().toString().toUpperCase();
				String value = entry.getValue().toString();
				switch (key) {
				case "BYPASSSSLCERTCHECK":
					result.setBypassSslCertCheck(Boolean.valueOf(value));
					break;
				case "CLUSTERRECONNECTCOUNT":
					result.setClusterReconnectCount(Integer.valueOf(value));
					break;
				case "CONNECTIONINACTIVITYVALIDATIONTIMEOUT":
					result.setConnectionInactivityValidationTimeout(Integer.valueOf(value));
					break;
				case "DISABLEAUTODISCOVERY":
					result.setDisableAutoDiscovery(Boolean.valueOf(value));
					break;
				case "DISABLEFAILOVER":
					result.setDisableFailover(Boolean.valueOf(value));
					break;
				case "HAFAILOVERORDER":
					switch (value.toUpperCase()) {
					case "RANDOM":
						result.setHAFailoverOrder(GPUdbBase.HAFailoverOrder.RANDOM);
						break;
					case "SEQUENTIAL":
						result.setHAFailoverOrder(GPUdbBase.HAFailoverOrder.SEQUENTIAL);
						break;
					}
				case "HOSTMANAGERPORT":
					result.setHostManagerPort(Integer.valueOf(value));
					break;
				case "HOSTNAMEREGEX":
					result.setHostnameRegex(value);
					break;
				case "HTTPHEADERS":
					result.setHttpHeaders(gson.fromJson(value, Map.class));
					break;
				case "INITIALCONNECTIONATTEMPTTIMEOUT":
					result.setInitialConnectionAttemptTimeout(Long.valueOf(value));
					break;
				case "INTRACLUSTERFAILOVERTIMEOUT":
					result.setIntraClusterFailoverTimeout(Long.valueOf(value));
					break;
				case "LOGGINGLEVEL":
					result.setLoggingLevel(value);
					break;
				case "MAXCONNECTIONSPERHOST":
					result.setMaxConnectionsPerHost(Integer.valueOf(value));
					break;
				case "MAXTOTALCONNECTIONS":
					result.setMaxTotalConnections(Integer.valueOf(value));
					break;
				case "PASSWORD":
					result.setPassword(value);
					break;
				case "PRIMARYURL":
					result.setPrimaryUrl(value);
					break;
				case "SERVERCONNECTIONTIMEOUT":
					result.setServerConnectionTimeout(Integer.valueOf(value));
					break;
				case "THREADCOUNT":
					result.setThreadCount(Integer.valueOf(value));
					break;
				case "TIMEOUT":
					result.setTimeout(Integer.valueOf(value));
					break;
				case "USERNAME":
					result.setUsername(value);
					break;
				case "USESNAPPY":
					result.setUseSnappy(Boolean.valueOf(value));
					break;
				}
			}
		}
		return result;
	}

	private String getPayload(InputStream inputStream) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		StringBuffer sb = new StringBuffer();
		String str;
		while ((str = in.readLine()) != null) {
			sb.append(str);
		}
		String event = sb.toString();
		JsonElement jsonElement = JsonParser.parseString(event);

		String result = "";
		if (jsonElement.isJsonArray()) {
			boolean isFirst = true;
			for (JsonElement pa : (JsonArray) jsonElement) {
				if (isFirst) {
					isFirst = false;
					result = this.flattenJson((JsonObject) pa);
				} else {
					result = result + "\n" + this.flattenJson((JsonObject) pa);
				}

			}
		} else if (jsonElement.isJsonObject()) {
			result = this.flattenJson((JsonObject) jsonElement);
		}
		return result;
	}

	private String flattenJson(JsonObject jsonObject) {
		String result = "";
		boolean isFirst = true;
		TreeMap<?, ?> map = gson.fromJson(jsonObject, TreeMap.class);

		for (Entry<?, ?> entry : map.entrySet()) {
			String value = entry.getValue().toString();
			if (isFirst) {
				isFirst = false;
				result = result + value;
			} else {
				result = result + "," + value;
			}
		}
		return result;
	}
}