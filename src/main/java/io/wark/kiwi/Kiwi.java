package io.wark.kiwi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.*;

import java.io.IOException;
import java.util.*;

public class Kiwi {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private OkHttpClient client = new OkHttpClient();

    private String clientID;
    private String clientSecret;
    private String userAgent;

    private long sessionExpire = 0;
    private String accessToken;

    private static final int TIME_LIMIT = 60;
    private static final String BASE_URL = "https://anilist.co/api/";

    /*
    public static void main(String[] args){
        Kiwi fruit = new Kiwi(clientid,clientsecret);
        List<Map<String,Object>> resp = fruit.apiGet("anime/99255");
        System.out.println(resp);

    }*/

    private Map<String,Object> stringToMap(String str) throws Exception{
        Map<String,Object> result;
        JsonNode root = JSON_MAPPER.readTree(str);
        result = JSON_MAPPER.convertValue(root,Map.class);
        return result;
    }

    private List<Map<String,Object>> stringToMapList(String str) throws Exception{
        List<Map<String,Object>> l;
        TypeReference<List<Map<String,Object>>> mapType = new TypeReference<List<Map<String,Object>>>(){};
        l = JSON_MAPPER.readValue(str,mapType);
        return l;
    }

    /**
     * Request a new access token
     * @return
     **/

    private void updateToken(){
        //Headers object
        Map<String,String> content = new HashMap<String, String>();

        //Set the headers
        //headers.put("User-Agent",this.userAgent);
        content.put("grant_type","client_credentials");
        content.put("client_id",this.clientID);
        content.put("client_secret",this.clientSecret);

        //Make the request
        try {
            String response = makePostRequest(BASE_URL+"auth/access_token",content);
            Map<String,Object> mapResponse = stringToMap(response);
            //Parse the response to map and update the access token and the expiration time
            this.accessToken = mapResponse.get("access_token").toString();
            this.sessionExpire = Long.parseLong(mapResponse.get("expires").toString());
        }catch (Exception e){
            System.out.println(e.toString());
        }
    }

    private String makePostRequest(String url,Map<String,String> json) throws IOException{
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String content = JSON_MAPPER.writeValueAsString(json);
        RequestBody body = RequestBody.create(JSON,content);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent",this.userAgent)
                .build();
        Response response = this.client.newCall(request).execute();
        return response.body().string();
    }

    private String makeGetRequest(String url,Headers headers) throws IOException{
        Request request = new Request.Builder()
                .headers(headers)
                .url(url)
                .build();
        Response response = this.client.newCall(request).execute();
        return response.body().string();

    }

    //public Map<String,Object> apiGet(String path){
    public List<Map<String,Object>> apiGet(String path){
        List<Map<String,Object>> response;
        //Set headers
        Map<String,String> headers = new HashMap<String, String>();
        headers.put("User-Agent",this.userAgent);
        long currentTime = new Date().getTime(); //Get current time in seconds
        //If less than 60 seconds left or there's no access token, request a access token
        if(this.accessToken == null || currentTime+TIME_LIMIT > this.sessionExpire){
            updateToken();
        }
        headers.put("Authorization","Bearer "+this.accessToken);
        Headers headerBuild = Headers.of(headers);
        String requestResult;
        try{
            requestResult = makeGetRequest(BASE_URL+path,headerBuild);
            try{
                //The response is an array of JSON
                response = stringToMapList(requestResult);
            }catch (Exception listException){
                //The response is a single JSON
                response = new ArrayList<Map<String, Object>>();
                try{
                    Map<String,Object> element = stringToMap(requestResult);
                    response.add(element);
                }catch (Exception elementException){
                    System.out.println(elementException.toString());
                    Map<String,Object> error = new HashMap<String, Object>();
                    error.put("error",elementException.toString());
                    response.add(error);
                }
            }
        }catch (Exception e){
            response = new ArrayList<Map<String, Object>>();
            System.out.println(e.toString());
            Map<String,Object> error = new HashMap<String, Object>();
            error.put("error",e.toString());
            response.add(error);
        }
        return response;
    }

    /**
     * You should not use this constructor
     */
    public Kiwi(){
        this.clientID = null;
        this.clientSecret = null;
        this.userAgent = null;
    }

    /**
     * Only passing clientID and clientSecret. Repository user-agent will be used
     * @param clientID
     * @param clientSecret
     */
    public Kiwi(String clientID,String clientSecret){
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.userAgent = "Kiwi client";
    }

    /**
     * Pass your own user-agent
     * @param clientID
     * @param clientSecret
     * @param userAgent
     */
    public Kiwi(String clientID,String clientSecret,String userAgent){
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.userAgent = userAgent;
    }

}
