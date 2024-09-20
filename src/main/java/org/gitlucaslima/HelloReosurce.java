package org.gitlucaslima;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("")
public class HelloReosurce {


    @GET
    public String hello() {
        return "Hello from Quarkus REST";
    }


}