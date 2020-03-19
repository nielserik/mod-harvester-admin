/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

/**
 *
 * @author ne
 */
import java.lang.management.ManagementFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class MainVerticle extends AbstractVerticle {

  private final Logger logger = LoggerFactory.getLogger("harvester-admin");
  private final static String API_BASE_PATH = "/harvester-admin";
  private AdminRecordsHandlers adminRecordsHandlers;
  @Override
  public void start(Promise<Void> promise)  {
    final int port = Integer.parseInt(System.getProperty("port", "8080"));
    logger.info("Starting Harvester admin service "
      + ManagementFactory.getRuntimeMXBean().getName()
      + " on port " + port);

    adminRecordsHandlers = new AdminRecordsHandlers(vertx);

    Router router = Router.router(vertx);
    //router.put("/*").handler(BodyHandler.create()); // Tell vertx we want the whole PUT body in the handler
    router.get(API_BASE_PATH+"/harvestables").handler(adminRecordsHandlers::handleGetHarvestables);
    router.get(API_BASE_PATH+"/storages").handler(adminRecordsHandlers::handleGetStorages);
    router.get(API_BASE_PATH+"/transformations").handler(adminRecordsHandlers::handleGetTransformations);
    router.get(API_BASE_PATH+"/steps").handler(adminRecordsHandlers::handleGetSteps);
    router.get(API_BASE_PATH+"/transformation-steps").handler(adminRecordsHandlers::handleGetTransformationSteps);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port, result -> {
        if (result.succeeded()) {
          logger.info("Succeeded in starting the listener for Harvester admin service");
          promise.complete();
        } else {
          logger.error("Harvester admin service failed: " + result.cause().getMessage());
          promise.fail(result.cause());
        }
      });
  }
}

