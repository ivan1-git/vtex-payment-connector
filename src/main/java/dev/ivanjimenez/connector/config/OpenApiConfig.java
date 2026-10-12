package dev.ivanjimenez.connector.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger con los dos headers de VTEX, para poder probar desde /docs con el botón "Authorize". */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI connectorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VTEX Payment Connector")
                        .version("0.1.0")
                        .description("Conector de ejemplo que implementa el Payment Provider Protocol de VTEX."))
                .components(new Components()
                        .addSecuritySchemes("appKey", header(VtexCredentialsFilter.APP_KEY_HEADER))
                        .addSecuritySchemes("appToken", header(VtexCredentialsFilter.APP_TOKEN_HEADER)))
                .addSecurityItem(new SecurityRequirement().addList("appKey").addList("appToken"));
    }

    private static SecurityScheme header(String name) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(name);
    }
}
