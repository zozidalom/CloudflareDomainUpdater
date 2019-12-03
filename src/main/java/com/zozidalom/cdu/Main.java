package com.zozidalom.cdu;

import eu.roboflax.cloudflare.CloudflareAccess;
import eu.roboflax.cloudflare.CloudflareRequest;
import eu.roboflax.cloudflare.CloudflareResponse;
import eu.roboflax.cloudflare.constants.Category;
import eu.roboflax.cloudflare.objects.dns.DNSRecord;
import gyurix.minilib.utils.SU;
import gyurix.minilib.utils.json.JsonAPI;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
  private static ScheduledExecutorService executor;
  private static CloudflareAccess cloudflareAccess;
  public static void main(String[] args) throws Exception{
      SU.saveFiles("config.json");
      JsonAPI.deserialize(String.join("\n", Files.readAllLines(Paths.get("config.json"))), Config.class);
      cloudflareAccess = new CloudflareAccess(Config.getGlobalApiKey(), Config.getEmail());
      executor = Executors.newScheduledThreadPool(1);
      executor.scheduleAtFixedRate(() -> {
        CloudflareResponse<List<DNSRecord>> response = new CloudflareRequest(Category.LIST_DNS_RECORDS, cloudflareAccess).identifiers(Config.getZoneId()).asObjectList(DNSRecord.class);
        response.getObject().forEach(dnsRecord -> {
          try {
            if (dnsRecord.getName().equals(Config.getDomain()) && dnsRecord.getType().equals("A")) {
              new CloudflareRequest(Category.UPDATE_DNS_RECORD, cloudflareAccess).identifiers(dnsRecord.getId()).identifiers(Config.getZoneId(), dnsRecord.getId()).body("content", new Scanner(new URL(Config.getIpProvider()).openStream(), "UTF-8").next()).send();
            }
          } catch(Throwable ex){
            ex.printStackTrace();
          }
        });
      }, 0, Long.parseLong(Config.getUpdateInterval()), TimeUnit.SECONDS);
    }
  }
