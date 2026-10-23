package com.events.infrastructure.config;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
// Debe extender Application para que SmallRye lea @OpenAPIDefinition.
// Requisito global: Swagger adjunta el Bearer en todos los endpoints salvo los que lo anulan con @SecurityRequirements vacio
@OpenAPIDefinition(info=@Info(title="events-api",version="1.0.0"), security=@SecurityRequirement(name="bearerAuth"))
@SecurityScheme(securitySchemeName="bearerAuth",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
public class OpenApiConfig extends Application { }
