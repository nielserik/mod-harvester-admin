/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static org.folio.harvesteradmin.ApiStatics.*;
import static org.folio.okapi.common.HttpResponse.*;

/**
 * Request handlers that proxy Harvester APIs and convert between
 * Harvester XML and FOLIO JSON.
 * @author ne
 */
public class AdminRecordsHandlers {
  private final WebClient harvesterClient;
  private final Logger logger = Logger.getLogger( "harvester-admin" );

  public AdminRecordsHandlers(Vertx vertx)
  {
    harvesterClient = WebClient.create( vertx );
    logger.setLevel( Config.logLevel );
  }

  /**
   * Proxies Harvester's GET /harvester/records/storages
   */
  public void handleGetStorages(RoutingContext routingCtx)
  {
    getRecordsAndRespond( routingCtx, HARVESTER_STORAGES_PATH );
  }

  public void handleGetStorageById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_STORAGES_PATH, STORAGE_ROOT_PROPERTY );
  }

  public void handlePutStorage( RoutingContext routingCtx )
  {
    putRecordAndRespond( routingCtx, HARVESTER_STORAGES_PATH, STORAGE_ROOT_PROPERTY );
  }

  /**
   * Proxies Harvester's GET /harvester/records/harvestables
   */
  public void handleGetHarvestables(RoutingContext routingCtx)
  {
    getRecordsAndRespond( routingCtx, HARVESTER_HARVESTABLES_PATH );
  }

  public void handleGetHarvestableById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_HARVESTABLES_PATH, HARVESTABLE_ROOT_PROPERTY );
  }

  public void handlePutHarvestable( RoutingContext routingCtx )
  {
    putRecordAndRespond( routingCtx, HARVESTER_HARVESTABLES_PATH, HARVESTABLE_ROOT_PROPERTY );
  }


  /**
   * Proxies Harvester's GET /harvester/records/transformations
   */
  public void handleGetTransformations( RoutingContext routingCtx )
  {
    getRecordsAndRespond( routingCtx, HARVESTER_TRANSFORMATIONS_PATH );
  }

  public void handleGetTransformationById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH, TRANSFORMATION_ROOT_PROPERTY );
  }

  public void handlePutTransformation( RoutingContext routingContext )
  {
    putRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH, TRANSFORMATION_ROOT_PROPERTY );
  }

  /**
   * Proxies Harvester's GET /harvester/records/steps
   */
  public void handleGetSteps( RoutingContext routingCtx )
  {
    getRecordsAndRespond( routingCtx, HARVESTER_STEPS_PATH );
  }

  public void handleGetStepById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_STEPS_PATH, STEP_ROOT_PROPERTY );
  }

  public void handlePutStep( RoutingContext routingContext )
  {
    logger.debug( "In handlePutStep" );
    putRecordAndRespond( routingContext, HARVESTER_STEPS_PATH, STEP_ROOT_PROPERTY );
  }

  /**
   * Proxies Harvester's GET /harvester/records/tsas  (transformation - step associations)
   */
  public void handleGetTransformationSteps( RoutingContext routingCtx )
  {
    getRecordsAndRespond( routingCtx, HARVESTER_TRANSFORMATIONS_STEPS_PATH );
  }

  public void handleGetTransformationStepById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH, TRANSFORMATION_STEP_ROOT_PROPERTY );
  }

  public void handlePutTransformationStep( RoutingContext routingContext )
  {
    putRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH, TRANSFORMATION_STEP_ROOT_PROPERTY );
  }

  private void getRecordsAndRespond( RoutingContext routingContext, String apiPath )
  {
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath ).send( ar -> {
        ProcessedHarvesterResponseGet response = new ProcessedHarvesterResponseGet( ar, apiPath, null );
        if ( response.getStatusCode() == 200 )
        {
          responseJson( routingContext, response.getStatusCode() ).end( response.getJsonResponse().encodePrettily() );
        }
        else
        {
          logger.error( "GET " + apiPath + " encountered a server error: " + response.getErrorMessage() );
          responseText( routingContext, response.getStatusCode() ).end( response.getErrorMessage() );
        }
      } );
    }
  }

  private void getRecordByIdAndRespond( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    String id = routingContext.request().getParam( "id" );
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).send( ar -> {
        ProcessedHarvesterResponseGetById response = new ProcessedHarvesterResponseGetById( ar, apiPath, id );
        if ( response.getStatusCode() == 200 )
        {
          responseJson( routingContext, response.getStatusCode() ).end(
                  response.getJsonResponse().getJsonObject( rootProperty ).encodePrettily() );
        }
        else
        {
          if ( response.getStatusCode() == 500 )
          {
            logger.error(
                    " GET by ID (" + id + ") to " + apiPath + " encountered a server error: " + response.getErrorMessage() );
          }
          responseText( routingContext, response.getStatusCode() ).end( response.getErrorMessage() );
        }
      } );
    }
  }

  private void putRecordAndRespond( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    String id = routingContext.request().getParam( "id" );
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      lookUpHarvesterRecordById( apiPath, id ).onComplete( idLookUp -> {
        if ( idLookUp.succeeded() )
        {
          int idLookUpStatus = idLookUp.result().getStatusCode();
          if ( idLookUpStatus == 404 )
          {
            responseText( routingContext, idLookUpStatus ).end( idLookUp.result().getErrorMessage() );
          }
          else if ( idLookUpStatus == 200 )
          {
            try
            {
              String xml = wrapJsonAndConvertToXml( routingContext.getBodyAsJson(), rootProperty );
              harvesterClient.put( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).putHeader(
                      ApiStatics.HEADER_CONTENT_TYPE, "application/xml" ).sendBuffer( Buffer.buffer( xml ), ar -> {
                if ( ar.succeeded() )
                {
                  if ( ar.result().statusCode() == 204 )
                  {
                    responseText( routingContext, ar.result().statusCode() ).end( "" );
                  }
                  else
                  {
                    responseText( routingContext, ar.result().statusCode() ).end(
                            "There was a problem PUTting to " + apiPath + "/" + id + ": " + ar.result().statusMessage() );
                  }
                }
                else
                {
                  responseText( routingContext, 500 ).end(
                          "There was an error PUTting to " + apiPath + "/" + id + ": " + ar.cause().getMessage() );
                }
              } );
            }
            catch ( TransformerException | ParserConfigurationException e )
            {
              logger.error( "Error parsing json " + routingContext.getBodyAsJson() );
              responseText( routingContext, 500 ).end( "Error parsing json " + routingContext.getBodyAsJson() );
            }
          }
          else
          {
            responseText( routingContext, idLookUpStatus ).end(
                    "There was an error (" + idLookUpStatus + ") looking up " + apiPath + "/" + id + " before PUT: " + idLookUp.result().getErrorMessage() );
          }
        }
        else
        {
          responseText( routingContext, 500 ).end(
                  "Could not look up record " + apiPath + "/" + id + " before PUT: " + idLookUp.cause().getMessage() );
        }
      } );
    }
  }

  private String wrapJsonAndConvertToXml( JsonObject json, String rootProperty ) throws ParserConfigurationException, TransformerException
  {
    JsonObject wrappedObject = new JsonObject();
    wrappedObject.put( rootProperty, json );
    Document doc = Json2Xml.recordJson2harvesterXml( wrappedObject.encode() );
    return Json2Xml.writeXmlDocumentToString( doc );
  }

  private Future<ProcessedHarvesterResponseGetById> lookUpHarvesterRecordById( String apiPath, String id )
  {
    Promise<ProcessedHarvesterResponseGetById> promise = Promise.promise();
    harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).send( ar -> {
      ProcessedHarvesterResponseGetById adaptedResponse = new ProcessedHarvesterResponseGetById( ar, apiPath, id );
      promise.complete( adaptedResponse );
    } );
    return promise.future();
  }

  private String acl( RoutingContext ctx )
  {
    return "acl=" + getTenant( ctx );
  }

  private boolean isJsonContentTypeOrNone( RoutingContext ctx )
  {
    String contentType = ctx.request().getHeader( HEADER_CONTENT_TYPE );
    return ( contentType == null || contentType.startsWith( "application/json" ) );
  }

  private String getTenant( RoutingContext ctx )
  {
    return ctx.request().getHeader( "x-okapi-tenant" );
  }
}
