/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Request handlers that proxy Harvester APIs and convert between
 * Harvester XML and FOLIO JSON.
 * @author ne
 */
public class AdminRecordsHandlers {
  private final WebClient harvesterClient;
  public static final String HARVESTER_HOST_ENV_VAR = "harvester_host";
  public static final String HARVESTER_PORT_ENV_VAR = "harvester_port";
  private final int harvesterPort;
  private final String harvesterHost;
  private final Logger logger = LoggerFactory.getLogger("harvester-admin");

  public AdminRecordsHandlers(Vertx vertx) {
    harvesterClient = WebClient.create(vertx);
    harvesterHost = System.getenv(HARVESTER_HOST_ENV_VAR);
    harvesterPort = Integer.parseInt(System.getenv(HARVESTER_PORT_ENV_VAR));
  }

  /**
   * Proxies Harvester's GET /harvester/records/storages
   * @param routingCtx
   */
  public void handleGetStorages(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("storages");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/harvestables
   * @param routingCtx
   */
  public void handleGetHarvestables(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx))  {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("harvestables");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/transformations
   * @param routingCtx
   */
  public void handleGetTransformations(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("transformations");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/steps
   * @param routingCtx
   */
  public void handleGetSteps(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("steps");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/tsas  (transformation - step associations)
   * @param routingCtx
   */
  public void handleGetTransformationSteps(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("tsas");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  private boolean isJsonContentTypeOrNone (RoutingContext ctx) {
    String contentType = ctx.request().getHeader("Content-Type");
    return (contentType == null || contentType.startsWith("application/json"));
  }

  /**
   * Handles GET requests to Harvester's records APIs
   * @param apiPath
   * @return
   */
  private Future<JsonObject> getRecords(String apiPath) {
    Promise<JsonObject> promise = Promise.promise();
    harvesterClient.get(harvesterPort, harvesterHost, "/harvester/records/"+apiPath).send(ar -> {
      String resp;
      JsonObject records = new JsonObject();
      if (ar.succeeded()) {
        HttpResponse<Buffer> harvesterResponse = ar.result();
        if (harvesterResponse != null) {
          resp = harvesterResponse.bodyAsString();
          records = Records2JsonArray.xml2json(resp);
        } else {
          resp = "Response was null";
          records.put("error", resp);
        }
        promise.complete(records);
      } else if (ar.failed()) {
        String fail = ar.cause().getMessage();
        records.put("error", "GET " + apiPath + " failed " + " [" + fail + "]");
        promise.complete(records);
      }
    });
    return promise.future();
  }


}