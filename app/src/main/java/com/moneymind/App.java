package com.moneymind;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

@QuarkusMain
public class App {

    private static final Logger LOG = Logger.getLogger(App.class);

    public static void main(String ... args) {
        Quarkus.run(args);
    }
}