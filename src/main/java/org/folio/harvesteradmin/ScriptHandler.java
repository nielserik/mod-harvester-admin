package org.folio.harvesteradmin;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.dataaccess.HarvesterApiClient;
import org.folio.harvesteradmin.statics.ApiPaths;

import static org.folio.okapi.common.HttpResponse.responseText;

public class ScriptHandler extends HarvesterApiClient
{
    public ScriptHandler( Vertx vertx )
    {
        super( vertx );
    }

    public void handleGetScript( RoutingContext routingContext )
    {
        respondWithScript( routingContext );
    }

    public void respondWithScript( RoutingContext routingContext )
    {
        String id = routingContext.request().getParam( "id" );
        getConfigRecordById( ApiPaths.HARVESTER_STEPS_PATH, id ).onComplete( getStep -> {
            if ( getStep.result().found() )
            {
                String script = getStep.result().jsonObject().getString( "script" );
                script = script.replaceAll( "\\r[\\n]?", System.lineSeparator() );
                responseText( routingContext, 200 ).end( script );
            }
            else
            {
                if ( getStep.result().wasNotFound() )
                {
                    responseText( routingContext, 404 ).end(
                            "Did not find step with ID " + id + " to GET script from" );
                }
                else
                {
                    responseText( routingContext, getStep.result().statusCode() ).end(
                            getStep.result().errorMessage() );
                }

            }
        } );
    }

    public void handlePutScript( RoutingContext routingContext )
    {
        String id = routingContext.request().getParam( "id" );
        String script = routingContext.getBodyAsString();
        getConfigRecordById( ApiPaths.HARVESTER_STEPS_PATH, id ).onComplete( getStep -> {
            if ( getStep.result().found() )
            {
                JsonObject step = getStep.result().jsonObject();
                step.put( "script", script );
                putConfigRecord( routingContext, step, id, ApiPaths.HARVESTER_STEPS_PATH ).onComplete( putStep -> {
                    if ( putStep.succeeded() )
                    {
                        responseText( routingContext, 204 ).end( "Script updated for step " + id );
                    }
                    else
                    {
                        responseText( routingContext, 500 ).end( putStep.cause().getMessage() );
                    }
                } );
            }
            else if ( getStep.result().wasNotFound() )
            {
                responseText( routingContext, 404 ).end( "Did not find step with ID " + id + " to PUT script to" );
            }
            else
            {
                responseText( routingContext, getStep.result().statusCode() ).end( getStep.result().errorMessage() );
            }
        } );
    }


}
