package com.accounting.core.tasks;

import io.dropwizard.servlets.tasks.Task;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static com.accounting.commons.tasks.TasksSwitcher.badRequestException;
import static com.accounting.commons.tasks.TasksSwitcher.getSingleMandatoryParameter;

public class RepositoryConfigTask extends Task {

    private final String IMPLEMENTATION = "implementation";

    public RepositoryConfigTask() {
        super("repo-impl");
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        String implementation = getSingleMandatoryParameter(parameters, IMPLEMENTATION);

        if (!"CACHE".equalsIgnoreCase(implementation) && !"DATABASE".equalsIgnoreCase(implementation)) {
            throw badRequestException("Parameter '%s' can only have value 'cache' or 'database'.", IMPLEMENTATION);
        }
        throw badRequestException("Feature not implemented yet.");

//        if ("CACHE".equalsIgnoreCase(implementation)) {
//            registerTypeRepo.setRepo(null);
//        }
//        if ("DATABASE".equalsIgnoreCase(implementation)) {
//            registerTypeRepo.setRepo(null);
//        }
    }
}
