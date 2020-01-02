package com.zozidalom.cdu;

import com.google.gson.JsonParser;
import gyurix.minilib.configfile.ConfigFile;
import gyurix.minilib.utils.SU;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
  private static ScheduledExecutorService executor;
  private static final CloseableHttpClient httpClient = HttpClients.createDefault();
  private static final JsonParser parser = new JsonParser();
  private static ConfigFile cfg;

  public static void main(String[] args) {
    SU.saveFiles("config.yml");
    cfg = new ConfigFile(new File("config.yml"));
    cfg.data.deserialize(Config.class);
    executor = Executors.newScheduledThreadPool(1);
    executor.scheduleAtFixedRate(() -> {
      Config.domainList.forEach(domain -> {
        try {
          HttpGet getRequest = new HttpGet("https://api.cloudflare.com/client/v4/zones/" + Config.zoneId + "/dns_records?name=" + domain);
          getRequest.addHeader("X-Auth-Email", Config.emailAddress);
          getRequest.addHeader("X-Auth-Key", Config.globalApiKey);
          CloseableHttpResponse response = httpClient.execute(getRequest);
          parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject().getAsJsonArray("result").forEach(jsonElement -> {
            if (jsonElement.getAsJsonObject().get("type").getAsString().equals("A")) {
              HttpPut putRequest = new HttpPut("https://api.cloudflare.com/client/v4/zones/" + Config.zoneId + "/dns_records/" + jsonElement.getAsJsonObject().get("id").getAsString());
              putRequest.addHeader("X-Auth-Email", Config.emailAddress);
              putRequest.addHeader("X-Auth-Key", Config.globalApiKey);
              try {
                String update = "{\"type\":\"A\",\"name\":\"" + jsonElement.getAsJsonObject().get("name").getAsString() + "\",\"content\":\"" + new Scanner(new URL("http://ipv4.icanhazip.com").openStream(), "UTF-8").next() + "\",\"ttl\":120,\"proxied\":false}";
                putRequest.setEntity(new ByteArrayEntity(update.getBytes()));
                httpClient.execute(putRequest);
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          });
        } catch (Throwable ex) {
          ex.printStackTrace();
        }
      });
    }, 0, 60, TimeUnit.SECONDS);
  }
}
