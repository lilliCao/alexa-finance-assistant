package amosalexa;

import amosalexa.services.help.HelpService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Service annotation to provide help.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    HelpService.FunctionGroup functionGroup();
    String functionName();
    String example();
    String description();
}
